package com.example.mygaeel.model;

import java.time.Instant;

public class SensorData {
    private final String sysId;
    private final String date;
    private final String time;
    private final String udt;
    private final String id;
    private final String data1;
    private final String data2;
    private final String data3;

    public SensorData(String sysId, String date, String time, String data1, String data2, String data3) {
        this.sysId = sysId;
        this.date = date;
        this.time = time;
        this.udt = Instant.now().toString();
        this.id = String.valueOf(System.currentTimeMillis());
        this.data1 = data1 != null ? data1 : "";
        this.data2 = data2 != null ? data2 : "";
        this.data3 = data3 != null ? data3 : "";
    }

    public String getSysId() { return sysId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getUdt() { return udt; }
    public String getId() { return id; }
    public String getData1() { return data1; }
    public String getData2() { return data2; }
    public String getData3() { return data3; }
}
