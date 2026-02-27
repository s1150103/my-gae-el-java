package com.example.mygaeel.service;

import com.example.mygaeel.entity.ElTargetEntity;
import com.example.mygaeel.entity.ElWorkRecordEntity;
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
        List<ElWorkRecordEntity> records = elWorkRecordService.queryByYearMonthRegion(year, paddedMonth, regionId);

        Map<String, String> eqMap = new LinkedHashMap<>();
        Map<String, long[]> counterMap = new HashMap<>();

        for (ElWorkRecordEntity rec : records) {
            Long startTime = rec.getStartTime();
            Long endTime = rec.getEndTime();
            if (startTime == null || endTime == null) continue;

            long uptimeSeconds = Math.max(0, (long) Math.ceil((endTime - startTime) / 1000.0));
            if (uptimeSeconds == 0) continue;

            String targetId = rec.getTargetId();
            int day = Integer.parseInt(rec.getDate());
            String key = targetId + "@" + day;

            if (!eqMap.containsKey(targetId)) {
                Optional<ElTargetEntity> te = elTargetService.getTargetByKey(regionId, targetId);
                eqMap.put(targetId, te.map(ElTargetEntity::getTargetName).orElse("UNKNOWN"));
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
                long[] counter = counterMap.getOrDefault(targetId + "@" + day, new long[]{0, 0});
                entry.put("cycle" + targetId, counter[0]);
                entry.put("time" + targetId, formatSeconds(counter[1]));
            }
            dataObjectList.add(entry);
        }

        List<Map<String, String>> eqList = new ArrayList<>();
        for (Map.Entry<String, String> e : eqMap.entrySet()) {
            eqList.add(Map.of("id", e.getKey(), "name", e.getValue()));
        }

        return Map.of("eqList", eqList, "dataObjectList", dataObjectList);
    }

    /**
     * mode=e: 各レコードの詳細稼働データ出力
     */
    public Map<String, Object> processEachMode(String year, String month, String regionId) {
        String paddedMonth = String.format("%02d", Integer.parseInt(month));
        List<ElWorkRecordEntity> records = elWorkRecordService.queryByYearMonthRegion(year, paddedMonth, regionId);

        Map<String, String> targetMap = new LinkedHashMap<>();
        List<Map<String, Object>> dataList = new ArrayList<>();

        for (ElWorkRecordEntity rec : records) {
            String targetId = rec.getTargetId();

            if (!targetMap.containsKey(targetId)) {
                Optional<ElTargetEntity> te = elTargetService.getTargetByKey(regionId, targetId);
                targetMap.put(targetId, te.map(ElTargetEntity::getTargetName).orElse("不明なターゲット"));
            }

            BigDecimal maxData = BigDecimal.valueOf(rec.getMaxData() != null ? rec.getMaxData() : 0.0)
                    .setScale(1, RoundingMode.HALF_UP);

            long uptime = 0;
            if (rec.getStartTime() != null && rec.getEndTime() != null) {
                uptime = (rec.getEndTime() - rec.getStartTime()) / 1000;
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("targetName", targetMap.get(targetId));
            entry.put("targetId", targetId);
            entry.put("MaxData", maxData.doubleValue());
            entry.put("startTime", rec.getStartTime() != null ? rec.getStartTime() : "");
            entry.put("endTime", rec.getEndTime() != null ? rec.getEndTime() : "");
            entry.put("upTime", uptime);
            entry.put("date", rec.getDate() != null ? rec.getDate() : "");
            dataList.add(entry);
        }

        return Map.of("datalist", dataList);
    }

    public static String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long sec = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, sec);
    }
}
