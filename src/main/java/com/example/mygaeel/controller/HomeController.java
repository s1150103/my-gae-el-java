package com.example.mygaeel.controller;

import com.example.mygaeel.entity.ElTargetEntity;
import com.example.mygaeel.service.ElTargetService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class HomeController {

    private final ElTargetService elTargetService;

    public HomeController(ElTargetService elTargetService) {
        this.elTargetService = elTargetService;
    }

    @GetMapping("/")
    public String home() {
        return "forward:/el_target_register.html";
    }

    @GetMapping("/download")
    public String downloadPage() {
        return "forward:/download.html";
    }

    @PostMapping("/el_target_register")
    public String elTargetRegister() {
        return "forward:/el_target_register.html";
    }

    @PostMapping("/target")
    @ResponseBody
    public ResponseEntity<Map<String, String>> registerTarget(@RequestBody Map<String, String> data) {
        try {
            String mode = data.getOrDefault("mode", "register");
            String regionId = data.get("regionId");
            String targetId = data.get("targetId");
            String targetName = data.get("targetName");

            if (!"register".equals(mode)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode"));
            }
            if (regionId == null || targetId == null || targetName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
            }

            elTargetService.save(new ElTargetEntity(regionId, targetId, targetName));
            return ResponseEntity.ok(Map.of("message", "Target registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/elsettingtargets")
    @ResponseBody
    public ResponseEntity<Map<String, String>> elSettingTargets(
            @RequestParam String mode,
            @RequestParam(required = false) String tid,
            @RequestParam(required = false) String tname,
            @RequestParam(required = false) String rid) {

        if (!"register".equals(mode)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode"));
        }
        if (tid == null || tname == null || rid == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
        }

        elTargetService.save(new ElTargetEntity(rid, tid, tname));
        return ResponseEntity.ok(Map.of("message", "Target registered successfully"));
    }

    @GetMapping("/KFormPaper3.html")
    public String serveKFormPaper3() { return "forward:/KFormPaper3.html"; }

    @GetMapping("/KunugiPaper.html")
    public String serveKunugiPaper() { return "forward:/KunugiPaper.html"; }

    @GetMapping("/Kunugi_graph.html")
    public String serveKunugiGraph() { return "forward:/Kunugi_graph.html"; }
}
