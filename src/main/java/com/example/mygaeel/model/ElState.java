package com.example.mygaeel.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1台の設備（sysId）のリアルタイム状態を保持するクラス。
 * センサーデータを受信するたびに状態が更新される。
 *
 * lastData    : 前回受信したチャンネルごとの値（例: {"ch1": 0.0, "ch2": 5.3}）
 * activeRecords: 現在稼働中の ElWorkRecord（ON 状態のチャンネルごとに1件）
 */
public class ElState {

    private final String sysId;                              // センサーのシステムID
    private final String regionId;                           // 所属するリージョンID
    private Map<String, Double> lastData;                    // 前回のチャンネル値（初回はnull）
    private final Map<String, ElWorkRecord> activeRecords;   // チャンネルキー → 稼働中のレコード

    /**
     * @param sysId    センサーID
     * @param regionId リージョンID
     */
    public ElState(String sysId, String regionId) {
        this.sysId = sysId;
        this.regionId = regionId;
        this.lastData = null; // 最初のデータ受信時に初期化される
        this.activeRecords = new ConcurrentHashMap<>(); // 複数スレッドから安全にアクセスできるMap
    }

    /**
     * センサーから受け取った data1 文字列を解析して、チャンネルごとの数値Mapに変換する。
     * 例: "ch1 0.0,ch2 2.97,ch3 NA" → {"ch1": 0.0, "ch2": 2.97, "ch3": null}
     *
     * @param dataString センサーデータ文字列
     * @return チャンネル名 → 数値のMap（"NA"はnull）
     */
    public Map<String, Double> parseSensorData(String dataString) {
        Map<String, Double> parsed = new HashMap<>();
        if (dataString == null || dataString.isBlank()) return parsed;

        for (String entry : dataString.split(",")) {
            String[] parts = entry.strip().split(" ");
            if (parts.length == 2) {
                String chKey = parts[0]; // チャンネル名（例: "ch1"）
                String val = parts[1];   // 値（例: "2.97" or "NA"）
                try {
                    // "NA" は null として扱い、それ以外は数値に変換
                    parsed.put(chKey, "NA".equalsIgnoreCase(val) ? null : Double.parseDouble(val));
                } catch (NumberFormatException e) {
                    parsed.put(chKey, null); // 数値に変換できない場合も null
                }
            }
        }
        return parsed;
    }

    public String getSysId() { return sysId; }
    public String getRegionId() { return regionId; }
    public Map<String, Double> getLastData() { return lastData; }
    public void setLastData(Map<String, Double> lastData) { this.lastData = lastData; }
    public Map<String, ElWorkRecord> getActiveRecords() { return activeRecords; }
}
