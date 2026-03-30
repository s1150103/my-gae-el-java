package com.example.mygaeel.model;

/**
 * 設備の月次集計データを保持するクラス。
 * 1設備×1日分の稼働回数と稼働時間を管理する。
 */
public class ElCounter {

    private final String targetId;     // 設備ID
    private int count;                 // 稼働回数（ON〜OFF の回数）
    private long uptimeSeconds;        // 累計稼働時間（秒）
    private final int day;             // 対象の日（1〜31）

    /**
     * @param targetId      設備ID
     * @param count         初期稼働回数
     * @param uptimeSeconds 初期稼働時間（秒）
     * @param day           対象日
     */
    public ElCounter(String targetId, int count, long uptimeSeconds, int day) {
        this.targetId = targetId;
        this.count = count;
        this.uptimeSeconds = uptimeSeconds;
        this.day = day;
    }

    /**
     * 稼働データを1件分加算する。
     * @param uptime 今回の稼働時間（秒）
     */
    public void applyData(long uptime) {
        this.count += 1;           // 稼働回数を1増やす
        this.uptimeSeconds += uptime; // 稼働時間を加算
    }

    /**
     * 稼働回数を返す（グラフ表示などに使用）。
     */
    public int getCountCycleData() {
        return count;
    }

    /**
     * 稼働時間を "HH:MM" 形式の文字列で返す。
     * 例: 3661秒 → "01:01"
     */
    public String getColonFormatTime() {
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    public String getTargetId() { return targetId; }
    public int getCount() { return count; }
    public long getUptimeSeconds() { return uptimeSeconds; }
    public int getDay() { return day; }
}
