package com.example.mygaeel.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sensor_data", indexes = {
    @Index(name = "idx_sysid_date", columnList = "sys_id, date")
})
public class SensorDataEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;  // System.currentTimeMillis() の文字列

    @Column(name = "sys_id", nullable = false)
    private String sysId;

    @Column(name = "date", nullable = false)
    private String date;

    @Column(name = "time")
    private String time;

    @Column(name = "udt")
    private String udt;

    @Column(name = "data1", columnDefinition = "TEXT")
    private String data1;

    @Column(name = "data2", columnDefinition = "TEXT")
    private String data2;

    @Column(name = "data3", columnDefinition = "TEXT")
    private String data3;

    protected SensorDataEntity() {}

    public SensorDataEntity(String sysId, String date, String time,
                            String data1, String data2, String data3) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.sysId = sysId;
        this.date = date;
        this.time = time != null ? time : "";
        this.udt = Instant.now().toString();
        this.data1 = data1 != null ? data1 : "";
        this.data2 = data2 != null ? data2 : "";
        this.data3 = data3 != null ? data3 : "";
    }

    public String getId() { return id; }
    public String getSysId() { return sysId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getUdt() { return udt; }
    public String getData1() { return data1; }
    public String getData2() { return data2; }
    public String getData3() { return data3; }
}
