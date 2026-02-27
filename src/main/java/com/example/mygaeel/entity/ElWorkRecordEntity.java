package com.example.mygaeel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * ■ 稼働記録テーブルのエンティティクラス
 *
 * センサーのチャンネル値が「稼働状態（3.0以上）」になった期間を1レコードとして記録します。
 * PostgreSQL の "el_work_records" テーブルに対応しています。
 *
 * 【稼働記録の仕組み】
 *   センサー値が 3.0 未満 → 3.0 以上 に変化  ：稼働開始 → このレコードを作成（startTime を記録）
 *   センサー値が 3.0 以上 → 3.0 未満 に変化  ：稼働終了 → endTime と uptime（稼働秒数）を更新
 *
 * @Index アノテーション：よく使う検索条件に対してインデックスを定義し、検索を高速化します。
 *   idx_region_year_month → 月次レポート生成時の検索用（リージョン・年・月での絞り込み）
 *   idx_region_id         → リージョンでの一覧取得用
 */
@Entity
@Table(name = "el_work_records", indexes = {
    @Index(name = "idx_region_year_month", columnList = "region_id, year, month"),
    @Index(name = "idx_region_id", columnList = "region_id")
})
public class ElWorkRecordEntity {

    /**
     * 主キー。"regionId-targetId-startTime" の形式で生成します。
     * 例："6-DAQA005-1720000000000"
     * 同一ターゲットの同じ時刻に複数レコードが作られることはないため、これで一意性を保証します。
     */
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    /** リージョン（地域）のID。 */
    @Column(name = "region_id", nullable = false)
    private String regionId;

    /** ターゲット（監視対象機器）のID。 */
    @Column(name = "target_id", nullable = false)
    private String targetId;

    /**
     * 稼働開始時刻（Unix ミリ秒）。
     * Unix ミリ秒とは、1970年1月1日 0時0分0秒からの経過ミリ秒数です。
     * 例：1720000000000 → 2024年7月3日頃
     */
    @Column(name = "start_time", nullable = false)
    private Long startTime;

    /**
     * 稼働終了時刻（Unix ミリ秒）。
     * 稼働中は null で、終了時に値がセットされます。
     */
    @Column(name = "end_time")
    private Long endTime;

    /**
     * この稼働期間中のセンサー最大値。
     * 稼働中にセンサー値が更新されるたびに最大値を上書きします。
     */
    @Column(name = "max_data")
    private Double maxData;

    /**
     * 稼働開始日付の文字列表現。月次集計での表示に使います。
     * 例："2025-07-01"
     */
    @Column(name = "date_string")
    private String dateString;

    /** 稼働開始年。例："2025" （月次検索に使用） */
    @Column(name = "year")
    private String year;

    /** 稼働開始月（ゼロ埋め2桁）。例："07" （月次検索に使用） */
    @Column(name = "month")
    private String month;

    /** 稼働開始日（ゼロ埋め2桁）。例："01" （日別集計に使用） */
    @Column(name = "date")
    private String date;

    /**
     * 稼働時間（秒）。
     * endTime がセットされたタイミングで (endTime - startTime) / 1000 で計算します。
     */
    @Column(name = "uptime")
    private Long uptime;

    /** JPA 用の引数なしコンストラクタ（外部から直接呼ばないでください）。 */
    protected ElWorkRecordEntity() {}

    /**
     * 稼働記録を新規作成するコンストラクタ（稼働開始時に呼ぶ）。
     *
     * startTime（Unix ミリ秒）から year/month/date/dateString を自動計算します。
     * DateTimeFormatter を使って Unix ミリ秒 → 日付文字列に変換しています。
     *
     * @param regionId  リージョンID
     * @param targetId  ターゲットID
     * @param startTime 稼働開始時刻（Unix ミリ秒）
     * @param maxData   初期センサー値
     */
    public ElWorkRecordEntity(String regionId, String targetId, long startTime, double maxData) {
        this.id = regionId + "-" + targetId + "-" + startTime;
        this.regionId = regionId;
        this.targetId = targetId;
        this.startTime = startTime;
        this.maxData = maxData;

        // Unix ミリ秒 → UTC の日付情報に変換
        Instant instant = Instant.ofEpochMilli(startTime);
        this.dateString = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(instant);
        this.year  = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneOffset.UTC).format(instant);
        this.month = DateTimeFormatter.ofPattern("MM").withZone(ZoneOffset.UTC).format(instant);
        this.date  = DateTimeFormatter.ofPattern("dd").withZone(ZoneOffset.UTC).format(instant);
    }

    // ── ゲッター ───────────────────────────────────────────────────────
    public String getId() { return id; }
    public String getRegionId() { return regionId; }
    public String getTargetId() { return targetId; }
    public Long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public Double getMaxData() { return maxData; }
    public String getDateString() { return dateString; }
    public String getYear() { return year; }
    public String getMonth() { return month; }
    public String getDate() { return date; }
    public Long getUptime() { return uptime; }

    /**
     * 稼働終了時刻をセットし、稼働時間（uptime）を計算します。
     * 稼働終了イベントが発生したときに ElStateService から呼ばれます。
     *
     * uptime = (終了時刻ミリ秒 - 開始時刻ミリ秒) ÷ 1000 = 秒数
     */
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
        if (endTime != null && startTime != null) {
            this.uptime = (endTime - startTime) / 1000;
        }
    }

    /** センサー最大値を更新します。稼働中に新しい最大値が出たときに呼ばれます。 */
    public void setMaxData(Double maxData) { this.maxData = maxData; }
}
