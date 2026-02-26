package com.example.mygaeel.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElWorkRecord {
    private final String id;
    private final String regionId;
    private final String targetId;
    private final long startTime;
    private Long endTime;
    private double maxData;
    private final String dateString;
    private final String year;
    private final String month;
    private final String date;

    public ElWorkRecord(String regionId, String targetId, long startTime, double maxData) {
        this.id = regionId + "-" + targetId + "-" + startTime;
        this.regionId = regionId;
        this.targetId = targetId;
        this.startTime = startTime;
        this.endTime = null;
        this.maxData = maxData;

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
