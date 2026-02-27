# my-gae-el-java

マンホールポンプの稼働状況をリアルタイムで監視する **IoT プラットフォーム**です。
役場が発注し、下水道点検業者が日常点検・月次報告に利用します。

Python (Flask) 版を **Java 17 / Spring Boot 3.x** に移植したプロジェクトです。

---

## システムの目的

```
役場（お客様）
  └─ マンホールポンプ場に Raspberry Pi を設置
  └─ 点検業者に閲覧アカウントを発行

点検業者（エンドユーザー）
  └─ 担当ポンプ場の稼働状況をブラウザで確認
  └─ 月次の稼働回数・稼働時間をもとに点検報告書を作成
```

---

## アーキテクチャ

### システム全体図

```
【現場】                              【クラウド (GCP)】

  電流センサー                         ┌──────────────────────────────────────┐
  (ポンプ電流を計測)                    │   Google App Engine Standard         │
       │                             │          (Java 17)                   │
       │                             │                                       │
  ┌────┴───────────────┐  HTTP POST  │  ┌─────────────────────────────────┐  │
  │  Raspberry Pi      │────────────▶│  │   Spring Boot アプリ (:8080)     │  │
  │  (各ポンプ場に1台)  │  /ellighttracker2  │  │                                 │  │
  └────────────────────┘  ?mode=d   │  └──────────────┬──────────────────┘  │
                                     │                 │                     │
                                     │                 ▼                     │
  ブラウザ                            │  ┌─────────────────────────────────┐  │
  (点検業者・役場)                     │  │   Google Cloud Datastore        │  │
       │         HTTPS               │  │                                 │  │
       └────────────────────────────▶│  │  User / ElTarget /              │  │
         ログイン後に閲覧可             │  │  SensorData / ElWorkRecord      │  │
                                     │  └─────────────────────────────────┘  │
                                     └──────────────────────────────────────┘
```

---

### ユーザーロール

| ロール | 利用者 | できること |
|---|---|---|
| **管理者** | 役場担当者 | ユーザー登録・機器登録・全リージョン閲覧 |
| **点検業者** | 下水道点検業者 | 担当ポンプ場の稼働データ閲覧・報告書出力 |

> ⚠️ **現在未実装**：ロール管理・リージョンごとのアクセス制限は未実装です。
> 現状は全ユーザーが全データを閲覧できます。

---

### レイヤー構成

```
┌──────────────────────────────────────────────────────────┐
│  クライアント層                                            │
│  Raspberry Pi (HTTP送信)  /  ブラウザ (点検業者・役場)     │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│  セキュリティ層 (Spring Security)                          │
│  /ellighttracker2?mode=d → 認証スキップ（IoTデバイス用）   │
│  それ以外               → ログイン必須（セッション認証）    │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│  コントローラー層                                          │
│  AuthController        /login  /register  /logout        │
│  HomeController        /  /target  /elsettingtargets     │
│  SensorController      /ellighttracker2 (mode=d/s/j)     │
│  WorkRecordController  /elworkrecord                     │
│  MonthController       /month (mode=t/e)                 │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│  サービス層                                               │
│  UserService         ユーザー認証・登録                    │
│  ElTargetService     対象機器のCRUD                       │
│  SensorDataService   センサーデータのCRUD・チャンネル解析   │
│  ElStateService      状態管理・ElWorkRecord自動生成        │
│  ElWorkRecordService 稼働レコードのCRUD                   │
│  MonthService        月次集計ロジック                      │
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
Raspberry Pi（ポンプ場）
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
                   └─ チャンネルごとに閾値判定（3.0 A を境界）
                          │
                          ├─ 待機 → 稼働（値が 3.0 超）
                          │    └─ ElWorkRecord を新規作成（startTime を記録）
                          │
                          ├─ 稼働 → 待機（値が 3.0 未満）
                          │    └─ ElWorkRecord に endTime・uptime を記録
                          │
                          └─ 稼働継続（値が 3.0 超のまま）
                               └─ maxData（最大電流値）を更新
```

---

### 稼働状態ステートマシン（ElState）

チャンネル（ch1〜ch32）ごとに独立して管理します。
1チャンネル = ポンプ1台に対応します。

```
            値 > 3.0
  ┌─────────────────────────────┐
  │                             ▼
待機中 ──── 値が 3.0 超 ──→  稼働中
(ポンプ停止)                 (ElWorkRecord を作成)
  ▲                             │
  │                             │ 稼働継続中 → maxData を更新
  └──── 値が 3.0 未満 ──────────┘
         (endTime・uptime を記録)
```

`ElStateService` は起動時に `EoE_Eden_Number.xml` を読み込み、
sysId ↔ regionId のマッピングをメモリに保持します。

---

### Datastore エンティティ設計

```
┌────────────────────────────────────────────────────┐
│ ElTarget  （監視対象ポンプ場）                       │
│  Key: "regionId#targetId"  例) "2#DAQA001"         │
│  regionId   : "2"          ← 地域ID（役場単位）     │
│  targetId   : "DAQA001"    ← Raspberry Pi の識別ID  │
│  targetName : "北塩原村 大久保"                      │
└───────────────────────┬────────────────────────────┘
                        │ 1対多
┌───────────────────────▼────────────────────────────┐
│ ElWorkRecord  （稼働1回分のレコード）                │
│  Key: "regionId-targetId-startTime"                │
│  startTime  : 1709000000000  (Unix ms)             │
│  endTime    : 1709003600000  (Unix ms)             │
│  uptime     : 3600           (秒)                  │
│  maxData    : 5.2            (最大電流値 A)         │
│  year/month/date : "2026"/"02"/"27"                │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│ SensorData  （生センサーデータ）                     │
│  Key: timestamp_ms文字列  例) "1709000000000"       │
│  sysId  : "DAQA001"                               │
│  date   : "2026-02-27"                             │
│  time   : "12:00:00"                               │
│  data1  : "ch1 0.0,ch2 5.2,ch3 NA,..."            │
│           └─ ch1〜ch32 の電流値（A）をカンマ区切り  │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│ User  （ログインユーザー）                           │
│  Key: email                                        │
│  email        : "inspector@example.com"            │
│  passwordHash : BCryptハッシュ                      │
└────────────────────────────────────────────────────┘
```

---

### sysId / regionId マッピング（EoE_Eden_Number.xml）

Raspberry Pi の `sysId` とシステム内部の `regionId` を紐付ける設定ファイルです。

```
EoE_Eden_Number.xml
  └─ region num="2"               ← regionId（役場・地域の単位）
       └─ client sysid="DAQA001"  ← Raspberry Pi の識別ID
            └─ data num="1"
                 └─ type: CHANNEL32  ← ch1〜ch32（ポンプ最大32台）
```

新しいポンプ場を追加する場合はこのファイルに `<region>` / `<client>` を追加し、
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
│   │   ├── AuthController.java      # /login /register /logout
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
    └── static/                      # Vue.js ダッシュボード
```

---

## API エンドポイント

| メソッド | パス | 認証 | 説明 |
|---|---|---|---|
| GET | `/login` | 不要 | ログイン画面 |
| POST | `/login` | 不要 | ログイン処理 |
| GET | `/register` | 不要 | 新規登録画面 |
| POST | `/register` | 不要 | ユーザー登録 |
| GET | `/logout` | 必要 | ログアウト |
| GET | `/` | 必要 | ポンプ場登録画面 |
| GET | `/download` | 必要 | データダウンロード画面 |
| POST | `/target` | 必要 | 監視対象ポンプ場の登録（JSON） |
| POST | `/elsettingtargets` | 必要 | 監視対象ポンプ場の登録（フォーム） |
| POST/GET | `/ellighttracker2?mode=d` | **不要** | センサーデータ保存（Raspberry Pi 用） |
| POST/GET | `/ellighttracker2?mode=s` | 必要 | センサーデータ取得（XML） |
| POST/GET | `/ellighttracker2?mode=j` | 必要 | センサーデータ取得（JSON グラフ用） |
| GET | `/elworkrecord?regionId=` | 必要 | 稼働レコード一覧 |
| GET/POST | `/month?mode=t` | 必要 | 月次合計（回数・稼働時間） |
| GET/POST | `/month?mode=e` | 必要 | 月次詳細（レコード一覧） |

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

起動後、`http://localhost:8080/login` でアカウントを作成してログインしてください。

### 動作確認

```bash
# センサーデータ送信（Raspberry Pi からの送信を模擬）
curl -X POST "http://localhost:8080/ellighttracker2?mode=d&sysId=DAQA001&date=2026-02-27&time=12:00:00&data1=ch1+0.0,ch2+5.0"

# 稼働レコード取得（要ログイン）
curl -b "SESSION=xxx" "http://localhost:8080/elworkrecord?regionId=2"

# 月次データ取得（要ログイン）
curl -b "SESSION=xxx" "http://localhost:8080/month?mode=t&rid=2&year=2026&month=02"
```

---

## GAE デプロイ

```bash
# app.yaml の GOOGLE_CLOUD_PROJECT を設定してからデプロイ
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn package -DskipTests
gcloud app deploy
```

---

## 今後の課題

| 優先度 | 機能 | 内容 |
|---|---|---|
| 高 | ロール管理 | 管理者（役場）と点検業者でできることを分ける |
| 高 | リージョン単位のアクセス制限 | 点検業者が担当外のポンプ場を見られないようにする |
| 中 | データエクスポート（CSV） | 点検報告書作成用のデータダウンロード |
| 中 | 現在の稼働状況画面 | 今この瞬間どのポンプが動いているか一覧表示 |
| 低 | デバイス死活監視 | 一定時間データが来なければ異常として記録 |

---

## 元プロジェクト

Python (Flask) 版は [`my-gae-el`](https://github.com/s1150103/my-gae-el) リポジトリにあります。
