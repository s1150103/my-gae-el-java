package com.example.mygaeel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "el_work_records", indexes = {
    @Index(name = "idx_region_year_month", columnList = "region_id, year, month"),
    @Index(name = "idx_region_id", columnList = "region_id")
})
public class ElWorkRecordEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;  // "regionId-targetId-startTime"

    @Column(name = "region_id", nullable = false)
    private String regionId;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(name = "start_time", nullable = false)
    private Long startTime;

    @Column(name = "end_time")
    private Long endTime;

    @Column(name = "max_data")
    private Double maxData;

    @Column(name = "date_string")
    private String dateString;

    @Column(name = "year")
    private String year;

    @Column(name = "month")
    private String month;

    @Column(name = "date")
    private String date;

    @Column(name = "uptime")
    private Long uptime;

    protected ElWorkRecordEntity() {}

    public ElWorkRecordEntity(String regionId, String targetId, long startTime, double maxData) {
        this.id = regionId + "-" + targetId + "-" + startTime;
        this.regionId = regionId;
        this.targetId = targetId;
        this.startTime = startTime;
        this.maxData = maxData;

        Instant instant = Instant.ofEpochMilli(startTime);
        this.dateString = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(instant);
        this.year  = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneOffset.UTC).format(instant);
        this.month = DateTimeFormatter.ofPattern("MM").withZone(ZoneOffset.UTC).format(instant);
        this.date  = DateTimeFormatter.ofPattern("dd").withZone(ZoneOffset.UTC).format(instant);
    }

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

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
        if (endTime != null && startTime != null) {
            this.uptime = (endTime - startTime) / 1000;
        }
    }
    public void setMaxData(Double maxData) { this.maxData = maxData; }
}
