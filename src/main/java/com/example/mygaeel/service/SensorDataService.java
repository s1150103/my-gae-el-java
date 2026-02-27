package com.example.mygaeel.service;

import com.example.mygaeel.entity.SensorDataEntity;
import com.example.mygaeel.repository.SensorDataRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ■ センサーデータ管理サービスクラス
 *
 * IoT デバイスから送られてきたセンサーデータの保存・取得・解析を担当します。
 * センサーが送ってくるデータ形式：
 *   "ch1 2.5, ch2 0.0, ch3 1.8, ch4 NA"
 * このような文字列を解析して、チャンネルごとの数値マップに変換する処理も含まれます。
 */
@Service
public class SensorDataService {

    private final SensorDataRepository repository;

    public SensorDataService(SensorDataRepository repository) {
        this.repository = repository;
    }

    /**
     * センサーデータを DB に保存します。
     *
     * @param entity 保存する SensorDataEntity
     * @return 保存後のエンティティ（ID などが確定した状態）
     */
    public SensorDataEntity save(SensorDataEntity entity) {
        return repository.save(entity);
    }

    /**
     * 指定の sysId と日付に一致するセンサーデータを時刻順で取得します。
     * グラフ表示やデータ確認に使用します。
     */
    public List<SensorDataEntity> queryBySysIdAndDate(String sysId, String date) {
        return repository.findBySysIdAndDateOrderByTime(sysId, date);
    }

    /**
     * センサーの data1 文字列を解析し、チャンネルごとの数値マップに変換します。
     *
     * 【入力形式の例】
     *   "ch1 2.5, ch2 0.0, ch3 1.8, ch4 NA"
     *
     * 【出力形式の例】
     *   { "ch1": 2.5, "ch2": 0.0, "ch3": 1.8, "ch4": null }
     *
     * 処理の流れ：
     *   1. カンマ（,）で分割 → ["ch1 2.5", " ch2 0.0", " ch3 1.8", " ch4 NA"]
     *   2. 各要素を空白で分割 → ["ch1", "2.5"] など
     *   3. キーが "ch" で始まる場合、数値変換してマップに追加
     *   4. "NA" や数値でない文字列は null としてマップに追加
     *
     * @param data1 センサーから受信した生のデータ文字列
     * @return チャンネル名 → 数値のマップ。有効なデータがなければ null を返す
     */
    public Map<String, Double> parseAllChannels(String data1) {
        Map<String, Double> channelData = new HashMap<>();
        boolean validDataFound = false;  // 有効なデータが1つでもあったかのフラグ

        // null または空文字の場合は即座に null を返す
        if (data1 == null || data1.isBlank()) return null;

        try {
            // カンマで分割して各チャンネルのデータを処理
            for (String item : data1.split(",")) {
                // 前後の空白を除去してから空白で分割
                // 例："ch1 2.5" → ["ch1", "2.5"]
                String[] parts = item.strip().split(" ");

                if (parts.length == 2 && parts[0].startsWith("ch")) {
                    try {
                        // 数値に変換して格納
                        channelData.put(parts[0], Double.parseDouble(parts[1]));
                        validDataFound = true;
                    } catch (NumberFormatException e) {
                        // "NA" など数値でない場合は null を格納
                        channelData.put(parts[0], null);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("データ解析エラー: " + e.getMessage());
            return null;
        }

        // 有効なデータが1件以上あればマップを返す、なければ null
        return validDataFound ? channelData : null;
    }
}
