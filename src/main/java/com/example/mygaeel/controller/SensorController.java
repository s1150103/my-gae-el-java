package com.example.mygaeel.controller;

import com.example.mygaeel.model.ElState;
import com.example.mygaeel.model.SensorData;
import com.example.mygaeel.service.ElStateService;
import com.example.mygaeel.service.ElTargetService;
import com.example.mygaeel.service.SensorDataService;
import com.google.cloud.datastore.Entity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class SensorController {

    private final SensorDataService sensorDataService;
    private final ElTargetService elTargetService;
    private final ElStateService elStateService;

    public SensorController(SensorDataService sensorDataService,
                            ElTargetService elTargetService,
                            ElStateService elStateService) {
        this.sensorDataService = sensorDataService;
        this.elTargetService = elTargetService;
        this.elStateService = elStateService;
    }

    /**
     * POST,GET /ellighttracker2 - センサーデータ受信（mode=d/s/j）
     */
    @RequestMapping(value = "/ellighttracker2", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<?> ellighttracker2(
            @RequestParam(required = false) String mode,
            @RequestParam(name = "sysId", required = false) String sysIdParam,
            @RequestParam(name = "sid", required = false) String sidParam,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String time,
            @RequestParam(defaultValue = "") String data1,
            @RequestParam(defaultValue = "") String data2,
            @RequestParam(defaultValue = "") String data3) {

        String sysId = sysIdParam != null ? sysIdParam : sidParam;
        // '+' → ' ' 変換（フォームエンコード対応）
        data1 = data1.replace("+", " ");
        data2 = data2.replace("+", " ");
        data3 = data3.replace("+", " ");

        if ("d".equals(mode)) {
            return handleModeD(sysId, date, time, data1, data2, data3);
        } else if ("s".equals(mode)) {
            return handleModeS(sysId, date);
        } else if ("j".equals(mode)) {
            return handleModeJ(sysId, date);
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode"));
    }

    private ResponseEntity<?> handleModeD(String sysId, String date, String time,
                                           String data1, String data2, String data3) {
        SensorData sensorData = new SensorData(sysId, date, time, data1, data2, data3);
        sensorDataService.save(sensorData);

        Map<String, Double> channelValues = sensorDataService.parseAllChannels(data1);
        if (channelValues == null) {
            System.out.println("無効なデータ: " + data1);
            return ResponseEntity.badRequest().body(Map.of("error", "無効なデータ: " + data1));
        }

        if (sysId == null || sysId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sys_id が指定されていません"));
        }

        String actualRegionId = elTargetService.getRegionIdByTarget(sysId);
        if (actualRegionId == null) {
            System.out.println("sysId " + sysId + " に対応する regionId が見つかりません");
            return ResponseEntity.badRequest().body(Map.of("error", "sysId " + sysId + " に対応する regionId が見つかりません"));
        }

        elStateService.updateState(sensorData);

        // ch2 の前後状態ログ
        Map<String, ElState> dynamicMap = elStateService.getDynamicStateMap();
        if (dynamicMap.containsKey(sysId)) {
            Double ch2Val = dynamicMap.get(sysId).getLastData() != null
                    ? dynamicMap.get(sysId).getLastData().get("ch2") : null;
            System.out.println("ch2 最新値: " + ch2Val + " (sysId=" + sysId + ")");
        }

        return ResponseEntity.ok(Map.of(
                "message", "Data stored successfully",
                "id", sensorData.getId(),
                "udt", sensorData.getUdt()
        ));
    }

    private ResponseEntity<?> handleModeS(String sysId, String date) {
        List<Entity> results = sensorDataService.queryBySysIdAndDate(sysId, date);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        xml.append("<result date='").append(date).append("' sysId='").append(sysId).append("'>");
        for (Entity entity : results) {
            xml.append("\n    <data")
               .append(" ID='").append(entity.getKey().getName()).append("'")
               .append(" Data1='").append(entity.getString("data1")).append("'")
               .append(" Data2='").append(entity.getString("data2")).append("'")
               .append(" Data3='").append(entity.getString("data3")).append("'")
               .append(" Time='").append(entity.getString("time")).append("'")
               .append(" UDT='").append(entity.getString("udt")).append("'")
               .append("/>");
        }
        xml.append("\n</result>");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    private ResponseEntity<?> handleModeJ(String sysId, String date) {
        if (sysId == null || date == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "sysId または date が指定されていません"));
        }

        List<Entity> results = sensorDataService.queryBySysIdAndDate(sysId, date);
        System.out.println("results 件数: " + results.size());

        List<List<Map<String, Object>>> dataLists = new ArrayList<>();
        for (int i = 0; i < 31; i++) dataLists.add(new ArrayList<>());

        for (Entity entity : results) {
            String rawData = entity.getString("data1");
            String timeVal = entity.getString("time");
            try {
                if (rawData != null && rawData.startsWith("ch")) {
                    String[] items = rawData.split(",");
                    for (int i = 0; i < items.length; i++) {
                        String[] parts = items[i].strip().split(" ");
                        Map<String, Object> point = new LinkedHashMap<>();
                        point.put("Time", timeVal);
                        if (parts.length == 2 && !"NA".equals(parts[1])) {
                            try {
                                point.put("data", Double.parseDouble(parts[1]));
                            } catch (NumberFormatException e) {
                                point.put("data", null);
                            }
                        } else {
                            point.put("data", null);
                        }
                        if (i - 1 >= 0 && i - 1 < dataLists.size()) {
                            dataLists.get(i - 1).add(point);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Data parse failed: " + e.getMessage());
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("Date", date);
        response.put("data_lists", dataLists);
        return ResponseEntity.ok(response);
    }
}
