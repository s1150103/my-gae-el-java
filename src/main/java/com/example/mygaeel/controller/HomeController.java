package com.example.mygaeel.controller;

import com.example.mygaeel.model.ElTarget;
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

    /**
     * POST /target - ElTarget登録（JSON）
     */
    @PostMapping("/target")
    @ResponseBody
    public ResponseEntity<Map<String, String>> registerTarget(@RequestBody Map<String, String> data) {
        try {
            System.out.println("受信データ: " + data);

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

            ElTarget target = new ElTarget(regionId, targetId, targetName);
            elTargetService.save(target);

            return ResponseEntity.ok(Map.of("message", "Target registered successfully"));
        } catch (Exception e) {
            System.out.println("エラー発生: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * POST /elsettingtargets - ElTarget登録（フォーム）
     */
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

        ElTarget target = new ElTarget(rid, tid, tname);
        elTargetService.save(target);
        return ResponseEntity.ok(Map.of("message", "Target registered successfully"));
    }

    @GetMapping("/KFormPaper3.html")
    public String serveKFormPaper3() {
        return "forward:/KFormPaper3.html";
    }

    @GetMapping("/KunugiPaper.html")
    public String serveKunugiPaper() {
        return "forward:/KunugiPaper.html";
    }

    @GetMapping("/Kunugi_graph.html")
    public String serveKunugiGraph() {
        return "forward:/Kunugi_graph.html";
    }
}
