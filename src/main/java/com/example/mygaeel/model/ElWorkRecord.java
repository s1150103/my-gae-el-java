package com.example.mygaeel.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 設備の稼働レコード（1回の ON〜OFF の記録）を表すモデルクラス。
 * Datastore の "ElWorkRecord" エンティティに対応する。
 *
 * センサー値が 3.0 を超えたときに「稼働開始」、
 * 3.0 を下回ったときに「稼働終了」として記録される。
 */
public class ElWorkRecord {

    private final String id;          // Datastore のキー。"regionId-targetId-startTime" の形式
    private final String regionId;    // 所属リージョンID
    private final String targetId;    // 設備ID
    private final long startTime;     // 稼働開始時刻（エポックミリ秒）
    private Long endTime;             // 稼働終了時刻（稼働中は null）
    private double maxData;           // 稼働中の最大センサー値
    private final String dateString;  // 開始日（"yyyy-MM-dd" 形式）
    private final String year;        // 開始年（"yyyy"）
    private final String month;       // 開始月（"MM"）
    private final String date;        // 開始日（"dd"）

    /**
     * @param regionId  リージョンID
     * @param targetId  設備ID
     * @param startTime 稼働開始時刻（エポックミリ秒）
     * @param maxData   初期の最大センサー値
     */
    public ElWorkRecord(String regionId, String targetId, long startTime, double maxData) {
        this.id = regionId + "-" + targetId + "-" + startTime; // ユニークなキーを生成
        this.regionId = regionId;
        this.targetId = targetId;
        this.startTime = startTime;
        this.endTime = null; // 稼働終了までは null
        this.maxData = maxData;

        // startTime から日付情報を抽出（UTC基準）
        Instant instant = Instant.ofEpochMilli(startTime);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
        this.dateString = dtf.format(instant);
        this.year = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneOffset.UTC).format(instant);
        this.month = DateTimeFormatter.ofPattern("MM").withZone(ZoneOffset.UTC).format(instant);
        this.date = DateTimeFormatter.ofPattern("dd").withZone(ZoneOffset.UTC).format(instant);
    }

    public String getId() { return id; }
    public String getRegionId() { return regionId; }
    public String getTargetId() { return targetId; }
    public long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public double getMaxData() { return maxData; }
    public void setMaxData(double maxData) { this.maxData = maxData; }
    public String getDateString() { return dateString; }
    public String getYear() { return year; }
    public String getMonth() { return month; }
    public String getDate() { return date; }
}
