package com.example.mygaeel.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * ■ センサーデータテーブルのエンティティクラス
 *
 * IoT デバイス（センサー）から送信されてきたデータを格納するテーブルです。
 * PostgreSQL の "sensor_data" テーブルに対応しています。
 *
 * センサーは定期的に「ch1 2.5, ch2 0.0, ch3 1.8」のような形式でデータを送ってきます。
 * このクラスはその1件分のデータを表します。
 *
 * @Index アノテーション：検索を高速化するためのデータベースインデックスを定義します。
 *   idx_sysid_date → sys_id と date の組み合わせで頻繁に検索するため、インデックスを作成
 */
@Entity
@Table(name = "sensor_data", indexes = {
    @Index(name = "idx_sysid_date", columnList = "sys_id, date")
})
public class SensorDataEntity {

    /**
     * 主キー。System.currentTimeMillis()（ミリ秒単位のタイムスタンプ）を文字列化したものを使用。
     * 例："1720000000000"
     * 簡易的な一意ID生成方法です（大量データでは UUID の方が安全）。
     */
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    /**
     * センサー（IoTデバイス）のシステムID。
     * どのデバイスから送られてきたデータかを識別します。
     * 例："DAQA005"
     */
    @Column(name = "sys_id", nullable = false)
    private String sysId;

    /** データが記録された日付。例："2025-07-01" */
    @Column(name = "date", nullable = false)
    private String date;

    /** データが記録された時刻。例："14:30:00" */
    @Column(name = "time")
    private String time;

    /**
     * レコードが作成された UTC タイムスタンプ（ISO 8601形式）。
     * UDT = Updated Date Time の略。
     * 例："2025-07-01T05:30:00Z"
     */
    @Column(name = "udt")
    private String udt;

    /**
     * センサーのチャンネルデータ1。
     * 例："ch1 2.5, ch2 0.0, ch3 1.8" のような形式のテキストデータ。
     * columnDefinition = "TEXT" → 長い文字列を格納するため TEXT 型を明示指定
     */
    @Column(name = "data1", columnDefinition = "TEXT")
    private String data1;

    /** センサーのチャンネルデータ2（追加データ用）。 */
    @Column(name = "data2", columnDefinition = "TEXT")
    private String data2;

    /** センサーのチャンネルデータ3（追加データ用）。 */
    @Column(name = "data3", columnDefinition = "TEXT")
    private String data3;

    /** JPA 用の引数なしコンストラクタ（外部から直接呼ばないでください）。 */
    protected SensorDataEntity() {}

    /**
     * センサーデータを新規作成するコンストラクタ。
     * id と udt は自動生成します。
     *
     * null 安全処理：data1〜3 や time が null の場合は空文字列 "" をセットして
     * DB への NULL 保存を防ぎます。
     */
    public SensorDataEntity(String sysId, String date, String time,
                            String data1, String data2, String data3) {
        // 現在時刻（ミリ秒）を文字列化して ID とする
        this.id = String.valueOf(System.currentTimeMillis());
        this.sysId = sysId;
        this.date = date;
        this.time = time != null ? time : "";
        // Instant.now() = 現在のUTC時刻を ISO 8601 形式で取得
        this.udt = Instant.now().toString();
        this.data1 = data1 != null ? data1 : "";
        this.data2 = data2 != null ? data2 : "";
        this.data3 = data3 != null ? data3 : "";
    }

    // ── ゲッター ───────────────────────────────────────────────────────
    public String getId() { return id; }
    public String getSysId() { return sysId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getUdt() { return udt; }
    public String getData1() { return data1; }
    public String getData2() { return data2; }
    public String getData3() { return data3; }
}
