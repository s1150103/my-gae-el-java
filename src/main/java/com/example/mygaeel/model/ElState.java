package com.example.mygaeel.model;

import com.example.mygaeel.entity.ElWorkRecordEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ElState {
    private final String sysId;
    private final String regionId;
    private Map<String, Double> lastData;
    private final Map<String, ElWorkRecordEntity> activeRecords;

    public ElState(String sysId, String regionId) {
        this.sysId = sysId;
        this.regionId = regionId;
        this.lastData = null;
        this.activeRecords = new ConcurrentHashMap<>();
    }

    public Map<String, Double> parseSensorData(String dataString) {
        Map<String, Double> parsed = new HashMap<>();
        if (dataString == null || dataString.isBlank()) return parsed;
        for (String entry : dataString.split(",")) {
            String[] parts = entry.strip().split(" ");
            if (parts.length == 2) {
                String chKey = parts[0];
                String val = parts[1];
                try {
                    parsed.put(chKey, "NA".equalsIgnoreCase(val) ? null : Double.parseDouble(val));
                } catch (NumberFormatException e) {
                    parsed.put(chKey, null);
                }
            }
        }
        return parsed;
    }

    public String getSysId() { return sysId; }
    public String getRegionId() { return regionId; }
    public Map<String, Double> getLastData() { return lastData; }
    public void setLastData(Map<String, Double> lastData) { this.lastData = lastData; }
    public Map<String, ElWorkRecordEntity> getActiveRecords() { return activeRecords; }
}
