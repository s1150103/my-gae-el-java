package com.example.mygaeel.service;

import com.example.mygaeel.entity.ElWorkRecordEntity;
import com.example.mygaeel.repository.ElWorkRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ■ 稼働記録管理サービスクラス
 *
 * センサーの「稼働開始〜稼働終了」の1サイクルを ElWorkRecord として管理します。
 * ElStateService から呼ばれ、稼働開始・終了・最大値更新を行います。
 *
 * 【呼び出しパターン】
 *   稼働開始 → save(new ElWorkRecordEntity(...))
 *   稼働終了 → updateEndTime(record, endTime)
 *   最大値更新 → record.setMaxData(newValue) → save(record)
 */
@Service
public class ElWorkRecordService {

    private final ElWorkRecordRepository repository;

    public ElWorkRecordService(ElWorkRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * 稼働記録を DB に保存します（新規作成 or 更新）。
     * 保存後のエンティティを返すことで、呼び出し元が保存結果を確認できます。
     */
    public ElWorkRecordEntity save(ElWorkRecordEntity record) {
        ElWorkRecordEntity saved = repository.save(record);
        System.out.println("ElWorkRecord 保存: " + saved.getId());
        return saved;
    }

    /**
     * 稼働終了時刻をセットして稼働記録を更新します。
     *
     * setEndTime() を呼ぶと uptime（稼働秒数）も自動計算されます（エンティティ内で処理）。
     *
     * @param record  更新対象の稼働記録
     * @param endTime 稼働終了時刻（Unix ミリ秒）
     */
    public void updateEndTime(ElWorkRecordEntity record, long endTime) {
        record.setEndTime(endTime);
        repository.save(record);
        System.out.println("ElWorkRecord 更新: " + record.getId()
                + " (uptime: " + record.getUptime() + "秒)");
    }

    /**
     * 指定リージョンの稼働記録を全件取得します（管理画面での一覧表示用）。
     */
    public List<ElWorkRecordEntity> queryByRegionId(String regionId) {
        return repository.findByRegionId(regionId);
    }

    /**
     * 年・月・リージョンを指定して稼働記録を取得します（月次レポート用）。
     * 開始時刻昇順で返されるため、時系列に並んだデータが得られます。
     */
    public List<ElWorkRecordEntity> queryByYearMonthRegion(String year, String month, String regionId) {
        return repository.findByYearAndMonthAndRegionIdOrderByStartTime(year, month, regionId);
    }
}
