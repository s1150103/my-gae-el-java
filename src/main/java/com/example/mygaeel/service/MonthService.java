package com.example.mygaeel.service;

import com.example.mygaeel.model.ElCounter;
import com.google.cloud.datastore.Entity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;

@Service
public class MonthService {

    private final ElWorkRecordService elWorkRecordService;
    private final ElTargetService elTargetService;

    public MonthService(ElWorkRecordService elWorkRecordService, ElTargetService elTargetService) {
        this.elWorkRecordService = elWorkRecordService;
        this.elTargetService = elTargetService;
    }

    /**
     * mode=t: 各日付ごとの設備の合計データ（回数・稼働時間）
     */
    public Map<String, Object> processTotalMode(String year, String month, String regionId) {
        String paddedMonth = String.format("%02d", Integer.parseInt(month));

        List<Entity> records = elWorkRecordService.queryByYearMonthRegion(year, paddedMonth, regionId);

        Map<String, String> eqMap = new LinkedHashMap<>();       // targetId → targetName
        Map<String, long[]> counterMap = new HashMap<>();         // "targetId@day" → [count, seconds]

        for (Entity rec : records) {
            Long startTime = rec.contains("startTime") ? rec.getLong("startTime") : null;
            Long endTime = rec.contains("endTime") && !rec.isNull("endTime") ? rec.getLong("endTime") : null;

            if (startTime == null || endTime == null) continue;

            long uptimeSeconds = Math.max(0, (long) Math.ceil((endTime - startTime) / 1000.0));
            if (uptimeSeconds == 0) continue;

            String targetId = rec.getString("targetId");
            int day = Integer.parseInt(rec.getString("date"));
            String key = targetId + "@" + day;

            if (!eqMap.containsKey(targetId)) {
                Entity targetEntity = elTargetService.getTargetByKey(regionId, targetId);
                eqMap.put(targetId, targetEntity != null ? targetEntity.getString("targetName") : "UNKNOWN");
            }

            counterMap.computeIfAbsent(key, k -> new long[]{0, 0});
            counterMap.get(key)[0] += 1;
            counterMap.get(key)[1] += uptimeSeconds;
        }

        int daysInMonth = YearMonth.of(Integer.parseInt(year), Integer.parseInt(paddedMonth)).lengthOfMonth();
        List<Map<String, Object>> dataObjectList = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", day);
            for (String targetId : eqMap.keySet()) {
                String key = targetId + "@" + day;
                long[] counter = counterMap.getOrDefault(key, new long[]{0, 0});
                long count = counter[0];
                long seconds = counter[1];
                entry.put("cycle" + targetId, count);
                entry.put("time" + targetId, formatSeconds(seconds));
            }
            dataObjectList.add(entry);
        }

        List<Map<String, String>> eqList = new ArrayList<>();
        for (Map.Entry<String, String> e : eqMap.entrySet()) {
            Map<String, String> eq = new LinkedHashMap<>();
            eq.put("id", e.getKey());
            eq.put("name", e.getValue());
            eqList.add(eq);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eqList", eqList);
        result.put("dataObjectList", dataObjectList);
        return result;
    }

    /**
     * mode=e: 各レコードの詳細稼働データ出力
     */
    public Map<String, Object> processEachMode(String year, String month, String regionId) {
        String paddedMonth = String.format("%02d", Integer.parseInt(month));

        List<Entity> records = elWorkRecordService.queryByYearMonthRegion(year, paddedMonth, regionId);

        Map<String, String> targetMap = new LinkedHashMap<>();
        List<Map<String, Object>> dataList = new ArrayList<>();

        for (Entity rec : records) {
            String targetId = rec.getString("targetId");

            if (!targetMap.containsKey(targetId)) {
                Entity targetEntity = elTargetService.getTargetByKey(regionId, targetId);
                if (targetEntity == null) {
                    System.out.println("target_entity が存在しません: targetId=" + targetId);
                    targetMap.put(targetId, "不明なターゲット");
                } else {
                    targetMap.put(targetId, targetEntity.getString("targetName"));
                }
            }

            double rawMaxData = rec.contains("maxData") ? rec.getDouble("maxData") : 0.0;
            BigDecimal maxData = BigDecimal.valueOf(rawMaxData).setScale(1, RoundingMode.HALF_UP);

            Long startTime = rec.contains("startTime") ? rec.getLong("startTime") : null;
            Long endTime = rec.contains("endTime") && !rec.isNull("endTime") ? rec.getLong("endTime") : null;

            long uptime = 0;
            if (startTime != null && endTime != null) {
                uptime = (endTime - startTime) / 1000;
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("targetName", targetMap.get(targetId));
            entry.put("targetId", targetId);
            entry.put("MaxData", maxData.doubleValue());
            entry.put("startTime", startTime != null ? startTime : "");
            entry.put("endTime", endTime != null ? endTime : "");
            entry.put("upTime", uptime);
            entry.put("date", rec.contains("date") ? rec.getString("date") : "");
            dataList.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datalist", dataList);
        return result;
    }

    /**
     * 秒を "HH:MM:SS" 形式に変換
     */
    public static String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long sec = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, sec);
    }
}
