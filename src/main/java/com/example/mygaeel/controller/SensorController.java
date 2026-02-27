package com.example.mygaeel.controller;

import com.example.mygaeel.entity.SensorDataEntity;
import com.example.mygaeel.model.ElState;
import com.example.mygaeel.model.SensorData;
import com.example.mygaeel.service.ElStateService;
import com.example.mygaeel.service.ElTargetService;
import com.example.mygaeel.service.RegionAccessService;
import com.example.mygaeel.service.SensorDataService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ■ センサーデータ受信 API コントローラー
 *
 * IoT デバイスからのデータ受信と、センサーデータの問い合わせを処理します。
 * SecurityConfig で /ellighttracker2 は認証不要に設定されています（IoTデバイスはログインできないため）。
 *
 * @RestController : @Controller + @ResponseBody の組み合わせ。
 *                  メソッドの戻り値が自動的に JSON（またはXML）に変換されてレスポンスになります。
 *
 * 【3種類のモード（mode パラメータで切り替え）】
 *   mode=d (Data)  : IoTデバイスがセンサーデータを送信する（書き込み）
 *   mode=s (Search): XML形式でセンサーデータを取得する（読み込み）
 *   mode=j (JSON)  : JSON形式でセンサーデータを取得する（読み込み）
 */
@RestController
public class SensorController {

    private final SensorDataService sensorDataService;
    private final ElTargetService elTargetService;
    private final ElStateService elStateService;
    private final RegionAccessService regionAccessService;

    /** コンストラクタインジェクション：4つのサービスを Spring が自動で注入します。 */
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
     * センサーデータの受信・問い合わせを一括処理するエンドポイント。
     *
     * @RequestMapping に POST と GET の両方を指定しています。
     * IoTデバイスは POST、フロントエンドの画面は GET でアクセスすることを想定しています。
     *
     * @param mode    処理モード（"d", "s", "j"）
     * @param sysIdParam "sysId" パラメータ名でのセンサーID（旧形式）
     * @param sidParam   "sid" パラメータ名でのセンサーID（新形式）。どちらかが使われる
     * @param date    日付
     * @param time    時刻
     * @param data1   センサーチャンネルデータ（例："ch1 2.5, ch2 0.0"）
     * @param data2   追加データ2
     * @param data3   追加データ3
     */
    @RequestMapping(value = "/ellighttracker2", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<?> ellighttracker2(
            @RequestParam(required = false) String mode,
            @RequestParam(name = "sysId", required = false) String sysIdParam,
            @RequestParam(name = "sid",   required = false) String sidParam,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String time,
            @RequestParam(defaultValue = "") String data1,
            @RequestParam(defaultValue = "") String data2,
            @RequestParam(defaultValue = "") String data3) {

        // sysId, sid のどちらのパラメータ名でも受け取れるようにする（後方互換）
        String sysId = sysIdParam != null ? sysIdParam : sidParam;

        // URL エンコードで "+" がスペースに変換されるのを修正
        // IoTデバイスが "ch1+2.5" として送ってきた場合に "ch1 2.5" に戻す
        data1 = data1.replace("+", " ");
        data2 = data2.replace("+", " ");
        data3 = data3.replace("+", " ");

        // mode パラメータによって処理を振り分け
        if ("d".equals(mode)) return handleModeD(sysId, date, time, data1, data2, data3);
        if ("s".equals(mode)) return handleModeS(sysId, date);
        if ("j".equals(mode)) return handleModeJ(sysId, date);

        // 未知の mode は 400 Bad Request を返す
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode"));
    }

    /**
     * mode=d の処理：IoTデバイスからのデータ受信（書き込み）。
     *
     * 処理の流れ：
     *   1. センサーデータを DB に保存
     *   2. データを解析してチャンネル値のマップを取得
     *   3. sysId からリージョンIDを特定
     *   4. ElStateService にデータを渡して稼働状態を更新（稼働開始/終了の判定）
     *   5. 成功レスポンスを返す
     *
     * ResponseEntity<?> : HTTP レスポンスのステータスコード・ヘッダー・ボディをまとめて返す型
     *   ResponseEntity.ok(body)           → HTTP 200 OK
     *   ResponseEntity.badRequest().body(body) → HTTP 400 Bad Request
     */
    private ResponseEntity<?> handleModeD(String sysId, String date, String time,
                                           String data1, String data2, String data3) {
        // センサーデータを DB に保存
        SensorDataEntity entity = new SensorDataEntity(sysId, date, time, data1, data2, data3);
        sensorDataService.save(entity);

        // data1 を解析してチャンネル値のマップを取得
        Map<String, Double> channelValues = sensorDataService.parseAllChannels(data1);
        if (channelValues == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "無効なデータ: " + data1));
        }
        if (sysId == null || sysId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sys_id が指定されていません"));
        }

        // sysId → regionId を特定（DBに登録されているかチェック）
        String actualRegionId = elTargetService.getRegionIdByTarget(sysId);
        if (actualRegionId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "sysId " + sysId + " に対応する regionId が見つかりません"));
        }

        // センサーデータを ElStateService に渡して稼働状態を更新
        SensorData sensorData = new SensorData(sysId, date, time, data1, data2, data3);
        elStateService.updateState(sensorData);

        return ResponseEntity.ok(Map.of(
                "message", "Data stored successfully",
                "id",  entity.getId(),
                "udt", entity.getUdt()
        ));
    }

    /**
     * mode=s の処理：XML 形式でセンサーデータを取得（読み込み）。
     *
     * 旧来の XML 形式に対応するため、JSON ではなく XML 文字列を返します。
     * MediaType.APPLICATION_XML でコンテンツタイプを明示します。
     *
     * アクセス権限チェック：ログインしているユーザーがこのリージョンにアクセス可能か確認します。
     */
    private ResponseEntity<?> handleModeS(String sysId, String date) {
        // リージョンへのアクセス権限チェック
        String regionId = elTargetService.getRegionIdByTarget(sysId);
        if (regionId != null && !regionAccessService.canAccess(regionId)) {
            return ResponseEntity.status(403).body(Map.of("error", "このリージョンへのアクセス権限がありません"));
        }

        List<SensorDataEntity> results = sensorDataService.queryBySysIdAndDate(sysId, date);

        // XML を文字列として手動生成
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        xml.append("<result date='").append(date).append("' sysId='").append(sysId).append("'>");
        for (SensorDataEntity e : results) {
            xml.append("\n    <data")
               .append(" ID='").append(e.getId()).append("'")
               .append(" Data1='").append(e.getData1()).append("'")
               .append(" Data2='").append(e.getData2()).append("'")
               .append(" Data3='").append(e.getData3()).append("'")
               .append(" Time='").append(e.getTime()).append("'")
               .append(" UDT='").append(e.getUdt()).append("'")
               .append("/>");
        }
        xml.append("\n</result>");

        // APPLICATION_XML でコンテンツタイプを指定してレスポンスを返す
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml.toString());
    }

    /**
     * mode=j の処理：JSON 形式でセンサーデータを取得（読み込み）。
     *
     * フロントエンド（Vue.js）でグラフ描画するために使います。
     * チャンネルごとのデータリストを最大31個の配列に格納して返します。
     * （1日のデータを時系列で返すため、ch1〜ch31 に対応する配列を用意）
     */
    private ResponseEntity<?> handleModeJ(String sysId, String date) {
        if (sysId == null || date == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "sysId または date が指定されていません"));
        }

        // リージョンへのアクセス権限チェック
        String regionId = elTargetService.getRegionIdByTarget(sysId);
        if (regionId != null && !regionAccessService.canAccess(regionId)) {
            return ResponseEntity.status(403).body(Map.of("error", "このリージョンへのアクセス権限がありません"));
        }

        List<SensorDataEntity> results = sensorDataService.queryBySysIdAndDate(sysId, date);

        // チャンネルごとのデータリスト（最大31チャンネル分）
        List<List<Map<String, Object>>> dataLists = new ArrayList<>();
        for (int i = 0; i < 31; i++) dataLists.add(new ArrayList<>());

        for (SensorDataEntity entity : results) {
            String rawData = entity.getData1();  // 例："ch1 2.5, ch2 0.0"
            String timeVal = entity.getTime();
            try {
                if (rawData != null && rawData.startsWith("ch")) {
                    String[] items = rawData.split(",");  // チャンネルごとに分割
                    for (int i = 0; i < items.length; i++) {
                        String[] parts = items[i].strip().split(" ");  // "ch1 2.5" → ["ch1", "2.5"]
                        Map<String, Object> point = new LinkedHashMap<>();
                        point.put("Time", timeVal);
                        if (parts.length == 2 && !"NA".equals(parts[1])) {
                            try {
                                // 数値に変換できれば格納
                                point.put("data", Double.parseDouble(parts[1]));
                            } catch (NumberFormatException e) {
                                point.put("data", null);  // 変換失敗は null
                            }
                        } else {
                            point.put("data", null);  // "NA" も null で格納
                        }
                        // i=0 は "ch1" なので dataLists[0] に格納
                        if (i - 1 >= 0 && i - 1 < dataLists.size()) {
                            dataLists.get(i - 1).add(point);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Data parse failed: " + e.getMessage());
            }
        }

        // JSON レスポンス：{ "Date": "2025-07-01", "data_lists": [[{...}, ...], [...], ...] }
        return ResponseEntity.ok(Map.of("Date", date, "data_lists", dataLists));
    }
}
