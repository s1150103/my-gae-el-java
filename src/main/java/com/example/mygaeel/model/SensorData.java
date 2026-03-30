package com.example.mygaeel.model;

import java.time.Instant;

/**
 * センサーから受信した生データを表すモデルクラス。
 * Datastore の "SensorData" エンティティに対応する。
 *
 * /ellighttracker2 エンドポイントで受信したデータをそのまま保存する。
 */
public class SensorData {

    private final String sysId;  // センサーのシステムID（どの設備のデータか）
    private final String date;   // 計測日（センサーが送信してきた日付文字列）
    private final String time;   // 計測時刻（センサーが送信してきた時刻文字列）
    private final String udt;    // サーバー受信時刻（ISO-8601形式のタイムスタンプ）
    private final String id;     // Datastore のキー（受信時のミリ秒タイムスタンプ）
    private final String data1;  // チャンネルデータ（例: "ch1 0.0,ch2 2.97,ch3 NA"）
    private final String data2;  // 予備データ2
    private final String data3;  // 予備データ3

    /**
     * @param sysId  センサーID
     * @param date   計測日
     * @param time   計測時刻
     * @param data1  チャンネルデータ文字列
     * @param data2  予備データ2
     * @param data3  予備データ3
     */
    public SensorData(String sysId, String date, String time, String data1, String data2, String data3) {
        this.sysId = sysId;
        this.date = date;
        this.time = time;
        this.udt = Instant.now().toString();           // サーバー受信時刻を自動設定
        this.id = String.valueOf(System.currentTimeMillis()); // 一意なIDとして現在時刻を使用
        this.data1 = data1 != null ? data1 : "";       // null の場合は空文字
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
