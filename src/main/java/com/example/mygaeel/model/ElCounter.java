package com.example.mygaeel.model;

public class ElCounter {
    private final String targetId;
    private int count;
    private long uptimeSeconds;
    private final int day;

    public ElCounter(String targetId, int count, long uptimeSeconds, int day) {
        this.targetId = targetId;
        this.count = count;
        this.uptimeSeconds = uptimeSeconds;
        this.day = day;
    }

    public void applyData(long uptime) {
        this.count += 1;
        this.uptimeSeconds += uptime;
    }

    public int getCountCycleData() {
        return count;
    }

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
