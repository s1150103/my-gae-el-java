package com.example.mygaeel.controller;

import com.example.mygaeel.model.ElState;
import com.example.mygaeel.model.SensorData;
import com.example.mygaeel.service.ElStateService;
import com.example.mygaeel.service.ElTargetService;
import com.example.mygaeel.service.RegionAccessService;
import com.example.mygaeel.service.SensorDataService;
import com.google.cloud.datastore.Entity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * IoTセンサーデータの受信・照会を担当するコントローラー。
 *
 * /ellighttracker2 エンドポイントで mode パラメータによって処理を切り替える:
 * - mode=d : センサーデータを受信して Datastore に保存し、稼働状態を更新する
 * - mode=s : 指定センサー・日付のデータをXML形式で返す
 * - mode=j : 指定センサー・日付のデータをJSON形式（チャンネル別リスト）で返す
 */
@RestController
public class SensorController {

    private final SensorDataService sensorDataService;
    private final ElTargetService elTargetService;
    private final ElStateService elStateService;
    private final RegionAccessService regionAccessService;

    public SensorController(SensorDataService sensorDataService,
                            ElTargetService elTargetService,
                            ElStateService elStateService,
                            RegionAccessService regionAccessService) {
        this.sensorDataService = sensorDataService;
        this.elTargetService = elTargetService;
        this.elStateService = elStateService;
        this.regionAccessService = regionAccessService;
    }

    /**
     * POST or GET /ellighttracker2 - センサーデータの受信・照会エンドポイント。
     *
     * @param mode     処理モード（"d", "s", "j"）
     * @param sysIdParam センサーID（パラメータ名 "sysId"）
     * @param sidParam   センサーID（パラメータ名 "sid"、古い互換用）
     * @param date     日付文字列
     * @param time     時刻文字列
     * @param data1    チャンネルデータ文字列（例: "ch1 0.0,ch2 2.97"）
     * @param data2    予備データ2
     * @param data3    予備データ3
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

        // "sysId" と "sid" どちらのパラメータ名でも受け付ける（後方互換）
        String sysId = sysIdParam != null ? sysIdParam : sidParam;

        // フォームエンコードでは '+' が空白の代わりに送られてくるので変換する
        data1 = data1.replace("+", " ");
        data2 = data2.replace("+", " ");
        data3 = data3.replace("+", " ");

        // mode に応じて処理を振り分ける
        if ("d".equals(mode)) {
            return handleModeD(sysId, date, time, data1, data2, data3); // データ保存
        } else if ("s".equals(mode)) {
            return handleModeS(sysId, date); // XML形式で返す
        } else if ("j".equals(mode)) {
            return handleModeJ(sysId, date); // JSON形式で返す
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode"));
    }

    /**
     * mode=d の処理: センサーデータを Datastore に保存し、稼働状態を更新する。
     */
    private ResponseEntity<?> handleModeD(String sysId, String date, String time,
                                           String data1, String data2, String data3) {
        // SensorData オブジェクトを作成して保存
        SensorData sensorData = new SensorData(sysId, date, time, data1, data2, data3);
        sensorDataService.save(sensorData);

        // data1 を解析してチャンネルごとの数値Mapを取得（解析失敗は400）
        Map<String, Double> channelValues = sensorDataService.parseAllChannels(data1);
        if (channelValues == null) {
            System.out.println("無効なデータ: " + data1);
            return ResponseEntity.badRequest().body(Map.of("error", "無効なデータ: " + data1));
        }

        if (sysId == null || sysId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sys_id が指定されていません"));
        }

        // sysId から regionId を取得（ElTarget テーブルに登録済みである必要がある）
        String actualRegionId = elTargetService.getRegionIdByTarget(sysId);
        if (actualRegionId == null) {
            System.out.println("sysId " + sysId + " に対応する regionId が見つかりません");
            return ResponseEntity.badRequest().body(Map.of("error", "sysId " + sysId + " に対応する regionId が見つかりません"));
        }

        // 前回値と比較して稼働開始・終了・最大値更新を判定し ElWorkRecord を保存
        elStateService.updateState(sensorData);

        // デバッグ用: ch2 の最新値をログ出力
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

    /**
     * mode=s の処理: 指定センサー・日付のデータをXML形式で返す。
     * IoTデバイスや外部システムが過去データを取得するために使う。
     */
    private ResponseEntity<?> handleModeS(String sysId, String date) {
        // リージョンのアクセス権チェック
        String regionId = elTargetService.getRegionIdByTarget(sysId);
        if (regionId != null && !regionAccessService.canAccess(regionId)) {
            return ResponseEntity.status(403).body(Map.of("error", "このリージョンへのアクセス権限がありません"));
        }

        List<Entity> results = sensorDataService.queryBySysIdAndDate(sysId, date);

        // XML文字列を手動で組み立てる
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
                .contentType(MediaType.APPLICATION_XML) // Content-Type: application/xml を指定
                .body(xml.toString());
    }

    /**
     * mode=j の処理: 指定センサー・日付のデータをJSON形式（チャンネル別リスト）で返す。
     * フロントエンドのグラフ描画ライブラリ向けのフォーマット。
     *
     * レスポンス構造:
     * {
     *   "Date": "2024-01-15",
     *   "data_lists": [
     *     [{"Time": "10:00", "data": 2.5}, ...],  // ch1 のデータリスト
     *     [{"Time": "10:00", "data": 1.2}, ...],  // ch2 のデータリスト
     *     ...（最大31チャンネル分）
     *   ]
     * }
     */
    private ResponseEntity<?> handleModeJ(String sysId, String date) {
        if (sysId == null || date == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "sysId または date が指定されていません"));
        }

        // リージョンのアクセス権チェック
        String regionId = elTargetService.getRegionIdByTarget(sysId);
        if (regionId != null && !regionAccessService.canAccess(regionId)) {
            return ResponseEntity.status(403).body(Map.of("error", "このリージョンへのアクセス権限がありません"));
        }

        List<Entity> results = sensorDataService.queryBySysIdAndDate(sysId, date);
        System.out.println("results 件数: " + results.size());

        // 31チャンネル分の空リストを準備（インデックスがチャンネル番号-1に対応）
        List<List<Map<String, Object>>> dataLists = new ArrayList<>();
        for (int i = 0; i < 31; i++) dataLists.add(new ArrayList<>());

        // 各センサーデータのレコードを処理
        for (Entity entity : results) {
            String rawData = entity.getString("data1"); // "ch1 0.0,ch2 2.97,..." 形式
            String timeVal = entity.getString("time");
            try {
                if (rawData != null && rawData.startsWith("ch")) {
                    String[] items = rawData.split(",");
                    for (int i = 0; i < items.length; i++) {
                        String[] parts = items[i].strip().split(" ");
                        Map<String, Object> point = new LinkedHashMap<>();
                        point.put("Time", timeVal);

                        // 数値として解析できる場合のみ data に設定（"NA" は null）
                        if (parts.length == 2 && !"NA".equals(parts[1])) {
                            try {
                                point.put("data", Double.parseDouble(parts[1]));
                            } catch (NumberFormatException e) {
                                point.put("data", null);
                            }
                        } else {
                            point.put("data", null);
                        }

                        // i=0 が "ch1" なので、dataLists[i-1] に追加（ch1 → index 0）
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
