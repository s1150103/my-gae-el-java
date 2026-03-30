package com.example.mygaeel.controller;

import com.example.mygaeel.model.ElTarget;
import com.example.mygaeel.service.ElTargetService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * トップページ・静的HTMLのルーティングと、ElTarget（監視対象設備）登録を担当するコントローラー。
 */
@Controller
public class HomeController {

    private final ElTargetService elTargetService;

    public HomeController(ElTargetService elTargetService) {
        this.elTargetService = elTargetService;
    }

    /**
     * GET / - トップページ。el_target_register.html（静的ファイル）にフォワードする。
     * forward: を使うことで、ブラウザのURLを変えずに別のリソースを返せる。
     */
    @GetMapping("/")
    public String home() {
        return "forward:/el_target_register.html";
    }

    /**
     * GET /download - ダウンロードページへフォワード。
     */
    @GetMapping("/download")
    public String downloadPage() {
        return "forward:/download.html";
    }

    /**
     * POST /el_target_register - 設備登録ページへフォワード（GETと同じページを返す）。
     */
    @PostMapping("/el_target_register")
    public String elTargetRegister() {
        return "forward:/el_target_register.html";
    }

    /**
     * POST /target - ElTarget（監視対象設備）をJSON形式で登録する。
     *
     * リクエストボディの例:
     * {"mode": "register", "regionId": "R01", "targetId": "EL001", "targetName": "エレベーター1号機"}
     *
     * @param data リクエストボディを Map として受け取る
     * @return 成功時は {"message": "..."}, 失敗時は {"error": "..."}
     */
    @PostMapping("/target")
    @ResponseBody // 戻り値をJSONとしてレスポンスボディに書き込む
    public ResponseEntity<Map<String, String>> registerTarget(@RequestBody Map<String, String> data) {
        try {
            System.out.println("受信データ: " + data);

            String mode = data.getOrDefault("mode", "register");
            String regionId = data.get("regionId");
            String targetId = data.get("targetId");
            String targetName = data.get("targetName");

            // mode が "register" 以外は不正なリクエスト
            if (!"register".equals(mode)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode"));
            }
            // 必須パラメータのチェック
            if (regionId == null || targetId == null || targetName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
            }

            ElTarget target = new ElTarget(regionId, targetId, targetName);
            elTargetService.save(target); // Datastore に保存

            return ResponseEntity.ok(Map.of("message", "Target registered successfully"));
        } catch (Exception e) {
            System.out.println("エラー発生: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * POST /elsettingtargets - ElTarget をフォームパラメータ形式で登録する。
     * /target と同じ処理だが、受け取り方が JSON でなくフォームデータ（@RequestParam）。
     *
     * @param mode  "register" のみ受け付ける
     * @param tid   ターゲットID
     * @param tname ターゲット名
     * @param rid   リージョンID
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

    /** 各静的HTMLファイルへのルーティング */
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
