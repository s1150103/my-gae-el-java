package com.example.mygaeel.controller;

import com.example.mygaeel.service.ElWorkRecordService;
import com.example.mygaeel.service.RegionAccessService;
import com.google.cloud.datastore.Entity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class WorkRecordController {

    private final ElWorkRecordService elWorkRecordService;
    private final RegionAccessService regionAccessService;

    public WorkRecordController(ElWorkRecordService elWorkRecordService,
                                RegionAccessService regionAccessService) {
        this.elWorkRecordService = elWorkRecordService;
        this.regionAccessService = regionAccessService;
    }

    @GetMapping("/elworkrecord")
    public ResponseEntity<Map<String, Object>> getElWorkRecord(@RequestParam String regionId) {
        if (!regionAccessService.canAccess(regionId)) {
            return ResponseEntity.status(403).body(Map.of("error", "このリージョンへのアクセス権限がありません"));
        }

        List<Entity> results = elWorkRecordService.queryByRegionId(regionId);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Entity entity : results) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("ID", entity.getKey().getName());
            rec.put("targetId", entity.getString("targetId"));
            rec.put("startTime", entity.getLong("startTime"));
            rec.put("endTime", entity.contains("endTime") && !entity.isNull("endTime")
                    ? entity.getLong("endTime") : null);
            rec.put("maxData", entity.getDouble("maxData"));
            records.add(rec);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("regionId", regionId);
        response.put("records", records);
        return ResponseEntity.ok(response);
    }
}
