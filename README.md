# my-gae-el-java

Python (Flask) で実装された EL 監視システムを **Java 21 / Spring Boot 3.x** に移植したプロジェクトです。
Google App Engine (Standard) へのデプロイを想定しています。

---

## 概要

揚水ポンプ等の設備に設置したラズベリーパイからセンサーデータを受信し、
Google Cloud Datastore に蓄積・管理するバックエンドサーバーです。

主な機能：
- センサーデータの受信・保存（`/ellighttracker2`）
- 稼働レコードの自動作成・更新（`ElWorkRecord`）
- 月次稼働データの集計・出力（`/month`）
- 対象機器の登録管理（`/target`, `/elsettingtargets`）

---

## 技術スタック

| 項目 | 内容 |
|---|---|
| 言語 | Java 17（本番は Java 21 推奨） |
| フレームワーク | Spring Boot 3.2 |
| ビルドツール | Maven |
| データベース | Google Cloud Datastore |
| デプロイ先 | Google App Engine Standard |

---

## プロジェクト構成

```
src/main/
├── java/com/example/mygaeel/
│   ├── MyGaeElApplication.java      # エントリポイント
│   ├── config/
│   │   ├── DatastoreConfig.java     # Datastoreクライアント Bean
│   │   └── CorsConfig.java          # CORS設定
│   ├── controller/
│   │   ├── HomeController.java      # /, /target, /elsettingtargets
│   │   ├── SensorController.java    # /ellighttracker2 (mode=d/s/j)
│   │   ├── WorkRecordController.java# /elworkrecord
│   │   └── MonthController.java     # /month (mode=t/e)
│   ├── model/
│   │   ├── ElTarget.java            # 対象機器
│   │   ├── SensorData.java          # センサーデータ
│   │   ├── ElWorkRecord.java        # 稼働レコード
│   │   ├── ElState.java             # センサー状態管理
│   │   └── ElCounter.java           # 稼働集計ヘルパー
│   └── service/
│       ├── ElTargetService.java     # ElTarget CRUD
│       ├── SensorDataService.java   # SensorData CRUD + チャンネル解析
│       ├── ElWorkRecordService.java  # ElWorkRecord CRUD
│       ├── ElStateService.java      # 状態管理・ElWorkRecord自動生成
│       └── MonthService.java        # 月次集計ロジック
└── resources/
    ├── application.properties
    ├── EoE_Eden_Number.xml          # sysId/regionId マッピング定義
    └── static/                      # HTML/CSS/JS 静的ファイル
```

---

## APIエンドポイント

| メソッド | パス | 説明 |
|---|---|---|
| GET | `/` | ターゲット登録画面 |
| GET | `/download` | ダウンロード画面 |
| POST | `/target` | ElTarget 登録（JSON） |
| POST | `/elsettingtargets` | ElTarget 登録（フォーム） |
| POST/GET | `/ellighttracker2?mode=d` | センサーデータ保存 |
| POST/GET | `/ellighttracker2?mode=s` | センサーデータ取得（XML） |
| POST/GET | `/ellighttracker2?mode=j` | センサーデータ取得（JSON） |
| GET | `/elworkrecord?regionId=` | 稼働レコード一覧 |
| GET/POST | `/month?mode=t` | 月次合計データ |
| GET/POST | `/month?mode=e` | 月次詳細データ |

---

## ローカル起動

### 前提条件

- Java 17 以上
- Maven 3.6 以上
- Google Cloud SDK（認証用）

### 手順

```bash
# GCP認証（ローカル実行時）
gcloud auth application-default login

# ビルド＆起動
cd my-gae-el-java
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn spring-boot:run
```

起動後、`http://localhost:8080/` でアクセスできます。

### 動作確認

```bash
# トップページ
curl http://localhost:8080/

# センサーデータ送信
curl -X POST "http://localhost:8080/ellighttracker2?mode=d&sysId=DAQA001&date=2026-02-27&time=12:00:00&data1=ch1+0.0,ch2+5.0"

# 稼働レコード取得
curl "http://localhost:8080/elworkrecord?regionId=2"

# 月次データ取得
curl "http://localhost:8080/month?mode=t&rid=2&year=2026&month=02"
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
