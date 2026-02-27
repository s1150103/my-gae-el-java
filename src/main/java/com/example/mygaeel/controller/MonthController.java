package com.example.mygaeel.controller;

import com.example.mygaeel.service.MonthService;
import com.example.mygaeel.service.RegionAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class MonthController {

    private final MonthService monthService;
    private final RegionAccessService regionAccessService;

    public MonthController(MonthService monthService, RegionAccessService regionAccessService) {
        this.monthService = monthService;
        this.regionAccessService = regionAccessService;
    }

    @RequestMapping(value = "/month", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> elMonth(
            @RequestParam(required = false) String mode,
            @RequestParam(name = "rid", required = false) String regionId,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String month) {

        if (mode == null || regionId == null || year == null || month == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
        }

        if (!regionAccessService.canAccess(regionId)) {
            return ResponseEntity.status(403).body(Map.of("error", "このリージョンへのアクセス権限がありません"));
        }

        if ("t".equals(mode)) {
            return ResponseEntity.ok(monthService.processTotalMode(year, month, regionId));
        } else if ("e".equals(mode)) {
            return ResponseEntity.ok(monthService.processEachMode(year, month, regionId));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode parameter"));
    }
}
