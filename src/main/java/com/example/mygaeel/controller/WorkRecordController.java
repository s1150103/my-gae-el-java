package com.example.mygaeel.controller;

import com.example.mygaeel.service.ElWorkRecordService;
import com.example.mygaeel.service.RegionAccessService;
import com.google.cloud.datastore.Entity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 設備の稼働レコード取得を担当するコントローラー。
 * リージョンIDを指定して、そのリージョンに属する全稼働レコードをJSON形式で返す。
 */
@RestController
public class WorkRecordController {

    private final ElWorkRecordService elWorkRecordService;
    private final RegionAccessService regionAccessService;

    public WorkRecordController(ElWorkRecordService elWorkRecordService,
                                RegionAccessService regionAccessService) {
        this.elWorkRecordService = elWorkRecordService;
        this.regionAccessService = regionAccessService;
    }

    /**
     * GET /elworkrecord?regionId={regionId} - 指定リージョンの稼働レコード一覧を返す。
     *
     * @param regionId 取得対象のリージョンID
     * @return JSON形式の稼働レコード一覧
     *
     * レスポンス例:
     * {
     *   "regionId": "R01",
     *   "records": [
     *     {"ID": "...", "targetId": "EL001", "startTime": 1234567890, "endTime": 1234568000, "maxData": 5.2},
     *     ...
     *   ]
     * }
     */
    @GetMapping("/elworkrecord")
    public ResponseEntity<Map<String, Object>> getElWorkRecord(@RequestParam String regionId) {

        // ログイン中のユーザーがこのリージョンにアクセスできるか確認
        if (!regionAccessService.canAccess(regionId)) {
            return ResponseEntity.status(403).body(Map.of("error", "このリージョンへのアクセス権限がありません"));
        }

        // Datastore から指定リージョンの稼働レコードを取得
        List<Entity> results = elWorkRecordService.queryByRegionId(regionId);

        // Datastore の Entity を Map 形式に変換してリスト化
        List<Map<String, Object>> records = new ArrayList<>();
        for (Entity entity : results) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("ID", entity.getKey().getName());            // Datastore のキー名
            rec.put("targetId", entity.getString("targetId"));   // 設備ID
            rec.put("startTime", entity.getLong("startTime"));   // 稼働開始時刻（エポックミリ秒）

            // endTime は稼働中（null）の場合があるため、存在確認してから取得
            rec.put("endTime", entity.contains("endTime") && !entity.isNull("endTime")
                    ? entity.getLong("endTime") : null);

            rec.put("maxData", entity.getDouble("maxData"));     // 稼働中の最大センサー値
            records.add(rec);
        }

        // レスポンスデータを組み立てて返す
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("regionId", regionId);
        response.put("records", records);
        return ResponseEntity.ok(response);
    }
}
