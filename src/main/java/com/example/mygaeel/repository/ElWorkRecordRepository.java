package com.example.mygaeel.repository;

import com.example.mygaeel.entity.ElWorkRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * ■ 稼働記録テーブルの DB 操作インターフェース
 *
 * JpaRepository<ElWorkRecordEntity, String> の意味：
 *   - ElWorkRecordEntity : 操作対象のエンティティ
 *   - String             : 主キー（"regionId-targetId-startTime" 形式）の型
 */
public interface ElWorkRecordRepository extends JpaRepository<ElWorkRecordEntity, String> {

    /**
     * 指定したリージョンの稼働記録を全件取得します。
     *
     * 自動生成される SQL のイメージ：
     *   SELECT * FROM el_work_records WHERE region_id = ?
     *
     * 使用場面：特定リージョンの全稼働履歴を表示する際に使います。
     */
    List<ElWorkRecordEntity> findByRegionId(String regionId);

    /**
     * 年・月・リージョンを指定して稼働記録を取得し、開始時刻順（昇順）で返します。
     *
     * 【メソッド名の読み方】
     * findBy + Year + And + Month + And + RegionId + OrderBy + StartTime
     *   ↓
     * "year が一致して、month が一致して、regionId が一致するものを startTime で並べて返す"
     *
     * 自動生成される SQL のイメージ：
     *   SELECT * FROM el_work_records
     *   WHERE year = ? AND month = ? AND region_id = ?
     *   ORDER BY start_time ASC
     *
     * 使用場面：月次稼働レポート（MonthService）を生成する際に使います。
     *   例：2025年7月のリージョン6の全稼働記録を開始時刻順で取得
     */
    List<ElWorkRecordEntity> findByYearAndMonthAndRegionIdOrderByStartTime(
            String year, String month, String regionId);
}
