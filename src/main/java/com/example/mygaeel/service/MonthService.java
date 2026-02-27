package com.example.mygaeel.service;

import com.example.mygaeel.entity.ElTargetEntity;
import com.example.mygaeel.entity.ElWorkRecordEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;

/**
 * ■ 月次稼働レポート生成サービスクラス
 *
 * 月ごとの稼働データを集計・整形して、フロントエンドに返す形式を作ります。
 * 2種類のレポートモードに対応しています。
 *
 * 【mode=t（Total：合計モード）】
 *   各日付ごとに、ターゲット（機器）別の「稼働回数」と「合計稼働時間」を集計します。
 *   月次サマリー表示に使います。
 *
 * 【mode=e（Each：個別モード）】
 *   各稼働レコードの詳細（開始・終了時刻、最大値、稼働秒数など）をそのまま返します。
 *   詳細ログ表示に使います。
 */
@Service
public class MonthService {

    private final ElWorkRecordService elWorkRecordService;
    private final ElTargetService elTargetService;

    public MonthService(ElWorkRecordService elWorkRecordService, ElTargetService elTargetService) {
        this.elWorkRecordService = elWorkRecordService;
        this.elTargetService = elTargetService;
    }

    /**
     * mode=t の集計処理。
     *
     * 【処理の流れ】
     *   1. DB から指定年月・リージョンの稼働記録を全件取得
     *   2. ターゲット×日付ごとに「稼働回数（cycle）」「合計稼働秒数（time）」を集計
     *   3. その月の日数分（1日〜月末日）のデータ行を生成
     *   4. 各ターゲットの表示名（eqList）とデータ行（dataObjectList）を返す
     *
     * @param year     年（例："2025"）
     * @param month    月（例："7"）
     * @param regionId リージョンID
     * @return { "eqList": [...], "dataObjectList": [...] } 形式のマップ
     */
    public Map<String, Object> processTotalMode(String year, String month, String regionId) {
        // 月を2桁にゼロ埋め（例："7" → "07"）
        String paddedMonth = String.format("%02d", Integer.parseInt(month));
        List<ElWorkRecordEntity> records = elWorkRecordService.queryByYearMonthRegion(year, paddedMonth, regionId);

        // ターゲットID → 表示名のマップ（重複なし、挿入順を維持するため LinkedHashMap）
        Map<String, String> eqMap = new LinkedHashMap<>();
        // "targetId@day"（例："DAQA005@15"）→ [稼働回数, 合計稼働秒数] の配列
        Map<String, long[]> counterMap = new HashMap<>();

        for (ElWorkRecordEntity rec : records) {
            Long startTime = rec.getStartTime();
            Long endTime = rec.getEndTime();
            // 終了時刻が記録されていないもの（稼働中）はスキップ
            if (startTime == null || endTime == null) continue;

            // 稼働秒数を計算（ミリ秒差 → 秒、切り上げ）
            long uptimeSeconds = Math.max(0, (long) Math.ceil((endTime - startTime) / 1000.0));
            if (uptimeSeconds == 0) continue;

            String targetId = rec.getTargetId();
            int day = Integer.parseInt(rec.getDate());
            String key = targetId + "@" + day;  // 例："DAQA005@15"

            // このターゲットの表示名をまだ取得していなければ DB から取得して登録
            if (!eqMap.containsKey(targetId)) {
                Optional<ElTargetEntity> te = elTargetService.getTargetByKey(regionId, targetId);
                eqMap.put(targetId, te.map(ElTargetEntity::getTargetName).orElse("UNKNOWN"));
            }

            // 稼働回数と合計稼働秒数を加算
            // computeIfAbsent: キーが存在しなければ new long[]{0, 0} で初期化
            counterMap.computeIfAbsent(key, k -> new long[]{0, 0});
            counterMap.get(key)[0] += 1;              // 稼働回数 +1
            counterMap.get(key)[1] += uptimeSeconds;  // 合計稼働秒数を加算
        }

        // その月の日数を取得（例：7月 → 31日）
        int daysInMonth = YearMonth.of(Integer.parseInt(year), Integer.parseInt(paddedMonth)).lengthOfMonth();
        List<Map<String, Object>> dataObjectList = new ArrayList<>();

        // 1日から月末まで1行ずつデータ行を生成
        for (int day = 1; day <= daysInMonth; day++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", day);  // 日付
            for (String targetId : eqMap.keySet()) {
                long[] counter = counterMap.getOrDefault(targetId + "@" + day, new long[]{0, 0});
                entry.put("cycle" + targetId, counter[0]);              // 稼働回数
                entry.put("time" + targetId, formatSeconds(counter[1])); // 稼働時間（HH:mm:ss形式）
            }
            dataObjectList.add(entry);
        }

        // ターゲット一覧（フロントエンドで列ヘッダーを生成するために使う）
        List<Map<String, String>> eqList = new ArrayList<>();
        for (Map.Entry<String, String> e : eqMap.entrySet()) {
            eqList.add(Map.of("id", e.getKey(), "name", e.getValue()));
        }

        return Map.of("eqList", eqList, "dataObjectList", dataObjectList);
    }

    /**
     * mode=e の詳細データ出力処理。
     *
     * 各稼働レコード1件ずつの詳細情報を返します。
     * 最大値（maxData）は小数点第1位で四捨五入して返します。
     *
     * @param year     年
     * @param month    月
     * @param regionId リージョンID
     * @return { "datalist": [...] } 形式のマップ
     */
    public Map<String, Object> processEachMode(String year, String month, String regionId) {
        String paddedMonth = String.format("%02d", Integer.parseInt(month));
        List<ElWorkRecordEntity> records = elWorkRecordService.queryByYearMonthRegion(year, paddedMonth, regionId);

        Map<String, String> targetMap = new LinkedHashMap<>();  // ターゲットID → 表示名
        List<Map<String, Object>> dataList = new ArrayList<>();

        for (ElWorkRecordEntity rec : records) {
            String targetId = rec.getTargetId();

            // ターゲット表示名を取得（同じtargetIdは一度だけDBアクセス）
            if (!targetMap.containsKey(targetId)) {
                Optional<ElTargetEntity> te = elTargetService.getTargetByKey(regionId, targetId);
                targetMap.put(targetId, te.map(ElTargetEntity::getTargetName).orElse("不明なターゲット"));
            }

            // maxData を小数点第1位で四捨五入（例：2.456 → 2.5）
            // BigDecimal を使うことで浮動小数点の誤差を避けます
            BigDecimal maxData = BigDecimal.valueOf(rec.getMaxData() != null ? rec.getMaxData() : 0.0)
                    .setScale(1, RoundingMode.HALF_UP);

            // 稼働秒数を計算（endTime が null の場合は 0）
            long uptime = 0;
            if (rec.getStartTime() != null && rec.getEndTime() != null) {
                uptime = (rec.getEndTime() - rec.getStartTime()) / 1000;
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("targetName", targetMap.get(targetId));
            entry.put("targetId", targetId);
            entry.put("MaxData", maxData.doubleValue());
            entry.put("startTime", rec.getStartTime() != null ? rec.getStartTime() : "");
            entry.put("endTime",   rec.getEndTime()   != null ? rec.getEndTime()   : "");
            entry.put("upTime", uptime);
            entry.put("date", rec.getDate() != null ? rec.getDate() : "");
            dataList.add(entry);
        }

        return Map.of("datalist", dataList);
    }

    /**
     * 秒数を "HH:mm:ss" 形式の文字列に変換します。
     *
     * 例：3661秒 → "01:01:01"
     *   3661 / 3600 = 1 時間
     *   3661 % 3600 = 61 → 61 / 60 = 1 分
     *   3661 % 60   = 1  → 1 秒
     *
     * String.format("%02d", ...) : 2桁になるようゼロ埋めしてフォーマット
     */
    public static String formatSeconds(long totalSeconds) {
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long sec     = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, sec);
    }
}
