# my-gae-el-java

Python (Flask) で実装された EL 監視システムを **Java 17 / Spring Boot 3.x** に移植したプロジェクトです。
Google App Engine (Standard) へのデプロイを想定しています。

---

## 概要

揚水ポンプ等の設備に設置したラズベリーパイからセンサーデータを受信し、
Google Cloud Datastore に蓄積・管理する IoT プラットフォームです。

主な機能：
- センサーデータの受信・保存（`/ellighttracker2`）
- 稼働レコードの自動作成・更新（`ElWorkRecord`）
- 月次稼働データの集計・出力（`/month`）
- 対象機器の登録管理（`/target`, `/elsettingtargets`）
- メール/パスワード認証（Spring Security）

---

## アーキテクチャ

### システム全体図

```
【現場】                          【クラウド (GCP)】

  センサー                        ┌──────────────────────────────────┐
  (電流計等)                       │   Google App Engine Standard     │
     │                           │          (Java 17)               │
     │                           │                                   │
  ┌──┴─────────────┐   HTTP POST  │  ┌────────────────────────────┐  │
  │ Raspberry Pi   │─────────────▶│  │   Spring Boot アプリ        │  │
  │ (sysId: DAQA001│  /ellighttracker2│  │   (:8080)                  │  │
  └────────────────┘   ?mode=d   │  └──────────┬─────────────────┘  │
                                  │             │                     │
                                  │             ▼                     │
  ブラウザ                         │  ┌────────────────────────────┐  │
  (管理者)                         │  │  Google Cloud Datastore    │  │
     │          HTTPS             │  │                            │  │
     └───────────────────────────▶│  │  User / ElTarget /         │  │
       ログイン後に閲覧可            │  │  SensorData / ElWorkRecord │  │
                                  │  └────────────────────────────┘  │
                                  └──────────────────────────────────┘
```

---

### レイヤー構成

```
┌──────────────────────────────────────────────────────────┐
│  クライアント層                                            │
│  Raspberry Pi (HTTPリクエスト)  /  ブラウザ (Vue.js)      │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│  セキュリティ層 (Spring Security Filter)                   │
│  /ellighttracker2 → 認証スキップ（IoTデバイス用）           │
│  それ以外          → ログイン必須（セッション認証）          │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│  コントローラー層                                          │
│  AuthController   /login  /register                      │
│  HomeController   /  /target  /elsettingtargets          │
│  SensorController /ellighttracker2 (mode=d/s/j)          │
│  WorkRecordController  /elworkrecord                     │
│  MonthController  /month (mode=t/e)                      │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│  サービス層                                               │
│  UserService       ユーザー認証・登録                      │
│  ElTargetService   対象機器のCRUD                         │
│  SensorDataService センサーデータのCRUD・チャンネル解析     │
│  ElStateService    状態管理・ElWorkRecord自動生成          │
│  ElWorkRecordService 稼働レコードのCRUD                   │
│  MonthService      月次集計ロジック                        │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│  データ層 (Google Cloud Datastore)                        │
│  User / ElTarget / SensorData / ElWorkRecord             │
└──────────────────────────────────────────────────────────┘
```

---

### センサーデータ受信フロー（最重要フロー）

Raspberry Pi からデータが届いたときの処理の流れです。

```
Raspberry Pi
  └─ POST /ellighttracker2?mode=d
       └─ SensorController
            │
            ├─① SensorDataService.save()
            │      └─ Datastore[SensorData] に保存
            │
            └─② ElStateService.updateState()
                   │
                   ├─ ElTargetService.getTargetBySysId()
                   │      └─ sysId → targetId / regionId を取得
                   │
                   └─ ch ごとに閾値判定（3.0 A を境界）
                          │
                          ├─ 待機 → 稼働（値が 3.0 超）
                          │    └─ ElWorkRecordService.save()  新規レコード作成
                          │
                          ├─ 稼働 → 待機（値が 3.0 未満）
                          │    └─ ElWorkRecordService.updateEndTime()  終了時刻を記録
                          │
                          └─ 稼働継続（値が 3.0 超のまま）
                               └─ maxData を更新して保存
```

---

### 稼働状態ステートマシン（ElState）

各チャンネル（ch1〜ch32）ごとに独立して状態を管理します。

```
            値 > 3.0
  ┌─────────────────────────────┐
  │                             ▼
待機中 ──── 値が 3.0 超 ──→  稼働中
(last_data ≤ 3.0)           (active_records に登録)
  ▲                             │
  │                             │ 稼働継続中
  └──── 値が 3.0 未満 ──────────┤ → maxData を更新
                                │
                                └─ 値が 3.0 未満 → endTime を記録して待機中へ
```

`ElStateService` は `@PostConstruct` で起動時に `EoE_Eden_Number.xml` を読み込み、
sysId ↔ regionId のマッピングをメモリに保持します。

---

### Datastore エンティティ設計

```
┌────────────────────────────────────────────────────┐
│ ElTarget                                           │
│  Key: "regionId#targetId"  例) "2#DAQA001"         │
│  regionId   : "2"                                  │
│  targetId   : "DAQA001"                            │
│  targetName : "北塩原村 大久保"                      │
└───────────────────────┬────────────────────────────┘
                        │ 1対多
┌───────────────────────▼────────────────────────────┐
│ ElWorkRecord                                       │
│  Key: "regionId-targetId-startTime"                │
│  regionId   : "2"                                  │
│  targetId   : "DAQA001"                            │
│  startTime  : 1709000000000  (Unix ms)             │
│  endTime    : 1709003600000  (Unix ms)             │
│  uptime     : 3600           (秒)                  │
│  maxData    : 5.2            (最大電流値 A)         │
│  year/month/date : "2026"/"02"/"27"                │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│ SensorData                                         │
│  Key: timestamp_ms文字列  例) "1709000000000"       │
│  sysId  : "DAQA001"                               │
│  date   : "2026-02-27"                             │
│  time   : "12:00:00"                               │
│  data1  : "ch1 0.0,ch2 5.2,ch3 NA,..."            │
│           └─ ch1〜ch32 の電流値をカンマ区切り       │
│  udt    : ISO8601 (受信日時)                       │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│ User                                               │
│  Key: email                                        │
│  email        : "admin@example.com"                │
│  passwordHash : BCryptハッシュ                      │
└────────────────────────────────────────────────────┘
```

---

### sysId / regionId マッピング（EoE_Eden_Number.xml）

Raspberry Pi の `sysId` とシステム内部の `regionId` を紐付ける設定ファイルです。
アプリ起動時に `ElStateService` がメモリに読み込みます。

```
EoE_Eden_Number.xml
  └─ region num="2"               ← regionId
       └─ client sysid="DAQA001"  ← Raspberry Pi の識別ID
            └─ data num="1"
                 └─ type: CHANNEL32  ← ch1〜ch32 の電流値データ
```

新しい拠点・機器を追加する場合はこのファイルに `<region>` / `<client>` を追加し、
再デプロイしてください。

---

## 技術スタック

| 項目 | 内容 |
|---|---|
| 言語 | Java 17（本番は Java 21 推奨） |
| フレームワーク | Spring Boot 3.2 |
| 認証 | Spring Security 6 + BCrypt |
| テンプレートエンジン | Thymeleaf（ログイン/登録画面のみ） |
| フロントエンド | Vue.js 2 + axios（静的 HTML） |
| ビルドツール | Maven |
| データベース | Google Cloud Datastore |
| デプロイ先 | Google App Engine Standard |

---

## プロジェクト構成

```
src/main/
├── java/com/example/mygaeel/
│   ├── MyGaeElApplication.java
│   ├── config/
│   │   ├── DatastoreConfig.java     # Datastoreクライアント Bean
│   │   ├── CorsConfig.java          # CORS設定
│   │   └── SecurityConfig.java      # Spring Security 設定
│   ├── controller/
│   │   ├── AuthController.java      # /login /register
│   │   ├── HomeController.java      # / /target /elsettingtargets
│   │   ├── SensorController.java    # /ellighttracker2
│   │   ├── WorkRecordController.java# /elworkrecord
│   │   └── MonthController.java     # /month
│   ├── model/
│   │   ├── User.java
│   │   ├── ElTarget.java
│   │   ├── SensorData.java
│   │   ├── ElWorkRecord.java
│   │   ├── ElState.java
│   │   └── ElCounter.java
│   └── service/
│       ├── UserService.java         # UserDetailsService 実装
│       ├── ElTargetService.java
│       ├── SensorDataService.java
│       ├── ElWorkRecordService.java
│       ├── ElStateService.java      # 状態管理 @PostConstruct
│       └── MonthService.java
└── resources/
    ├── application.properties
    ├── EoE_Eden_Number.xml          # sysId / regionId マッピング
    ├── templates/                   # Thymeleaf テンプレート
    │   ├── login.html
    │   └── register.html
    └── static/                      # Vue.js + HTML ダッシュボード
```

---

## API エンドポイント

| メソッド | パス | 認証 | 説明 |
|---|---|---|---|
| GET | `/login` | 不要 | ログイン画面 |
| POST | `/login` | 不要 | ログイン処理（Spring Security） |
| GET | `/register` | 不要 | 新規登録画面 |
| POST | `/register` | 不要 | ユーザー登録 |
| GET | `/logout` | 必要 | ログアウト |
| GET | `/` | 必要 | ターゲット登録画面 |
| GET | `/download` | 必要 | ダウンロード画面 |
| POST | `/target` | 必要 | ElTarget 登録（JSON） |
| POST | `/elsettingtargets` | 必要 | ElTarget 登録（フォーム） |
| POST/GET | `/ellighttracker2?mode=d` | **不要** | センサーデータ保存（IoTデバイス用） |
| POST/GET | `/ellighttracker2?mode=s` | 必要 | センサーデータ取得（XML） |
| POST/GET | `/ellighttracker2?mode=j` | 必要 | センサーデータ取得（JSON） |
| GET | `/elworkrecord?regionId=` | 必要 | 稼働レコード一覧 |
| GET/POST | `/month?mode=t` | 必要 | 月次合計データ |
| GET/POST | `/month?mode=e` | 必要 | 月次詳細データ |

---

## ローカル起動

### 前提条件

- Java 17 以上
- Maven 3.6 以上
- Google Cloud SDK（Datastore 認証用）

### 手順

```bash
# GCP 認証（ローカル実行時）
gcloud auth application-default login

# ビルド & 起動
cd my-gae-el-java
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn spring-boot:run
```

起動後、`http://localhost:8080/login` にアクセスしてアカウントを作成してください。

### 動作確認

```bash
# センサーデータ送信（認証不要）
curl -X POST "http://localhost:8080/ellighttracker2?mode=d&sysId=DAQA001&date=2026-02-27&time=12:00:00&data1=ch1+0.0,ch2+5.0"

# 稼働レコード取得（要セッション Cookie）
curl -b "SESSION=xxx" "http://localhost:8080/elworkrecord?regionId=2"

# 月次データ取得
curl -b "SESSION=xxx" "http://localhost:8080/month?mode=t&rid=2&year=2026&month=02"
```

---

## GAE デプロイ

```bash
# app.yaml の GOOGLE_CLOUD_PROJECT を設定してからデプロイ
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn package -DskipTests
gcloud app deploy
```

`app.yaml` の `runtime` は環境に合わせて `java17` または `java21` を指定してください。

---

## 設定ファイル

### `app.yaml`

```yaml
runtime: java17

env_variables:
  GOOGLE_CLOUD_PROJECT: your-project-id  # ← 実際のプロジェクトIDに変更
```

### `EoE_Eden_Number.xml`

sysId と regionId のマッピングを定義します。
新しい拠点・機器を追加する場合はこのファイルを編集してください。

---

## 元プロジェクト

Python (Flask) 版は [`my-gae-el`](https://github.com/s1150103/my-gae-el) リポジトリにあります。
