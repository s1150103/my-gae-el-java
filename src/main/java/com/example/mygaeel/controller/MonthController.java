package com.example.mygaeel.controller;

import com.example.mygaeel.service.MonthService;
import com.example.mygaeel.service.RegionAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 月次データの取得を担当するコントローラー。
 * @RestController はすべてのメソッドの戻り値を自動的にJSONに変換する。
 *
 * mode パラメータによって返すデータの形式が変わる:
 * - mode=t : 日ごとの合計集計（グラフ表示用）
 * - mode=e : 個々の稼働レコード詳細（一覧表示用）
 */
@RestController
public class MonthController {

    private final MonthService monthService;
    private final RegionAccessService regionAccessService;

    public MonthController(MonthService monthService, RegionAccessService regionAccessService) {
        this.monthService = monthService;
        this.regionAccessService = regionAccessService;
    }

    /**
     * GET or POST /month - 指定された年月・リージョンの稼働データを返す。
     *
     * @param mode     "t"（合計モード）or "e"（個別モード）
     * @param regionId リージョンID（パラメータ名は "rid"）
     * @param year     年（例: "2024"）
     * @param month    月（例: "3"）
     * @return JSON形式の稼働データ
     */
    @RequestMapping(value = "/month", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> elMonth(
            @RequestParam(required = false) String mode,
            @RequestParam(name = "rid", required = false) String regionId, // URLパラメータ名は "rid"
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String month) {

        // 必須パラメータが不足している場合は 400 Bad Request
        if (mode == null || regionId == null || year == null || month == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
        }

        // ログイン中のユーザーがこのリージョンにアクセスできるか確認
        if (!regionAccessService.canAccess(regionId)) {
            return ResponseEntity.status(403).body(Map.of("error", "このリージョンへのアクセス権限がありません"));
        }

        // mode に応じてサービスメソッドを呼び分ける
        if ("t".equals(mode)) {
            // mode=t: 日ごとの合計稼働回数・稼働時間を返す
            return ResponseEntity.ok(monthService.processTotalMode(year, month, regionId));
        } else if ("e".equals(mode)) {
            // mode=e: 個々の稼働レコード詳細を返す
            return ResponseEntity.ok(monthService.processEachMode(year, month, regionId));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode parameter"));
    }
}
