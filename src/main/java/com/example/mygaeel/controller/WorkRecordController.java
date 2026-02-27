package com.example.mygaeel.controller;

import com.example.mygaeel.entity.ElWorkRecordEntity;
import com.example.mygaeel.service.ElWorkRecordService;
import com.example.mygaeel.service.RegionAccessService;
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

        List<ElWorkRecordEntity> results = elWorkRecordService.queryByRegionId(regionId);
        List<Map<String, Object>> records = new ArrayList<>();
        for (ElWorkRecordEntity e : results) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("ID", e.getId());
            rec.put("targetId", e.getTargetId());
            rec.put("startTime", e.getStartTime());
            rec.put("endTime", e.getEndTime());
            rec.put("maxData", e.getMaxData());
            records.add(rec);
        }

        return ResponseEntity.ok(Map.of("regionId", regionId, "records", records));
    }
}
