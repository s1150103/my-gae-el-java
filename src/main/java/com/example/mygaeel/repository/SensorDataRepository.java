package com.example.mygaeel.repository;

import com.example.mygaeel.entity.SensorDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * ■ センサーデータテーブルの DB 操作インターフェース
 *
 * JpaRepository<SensorDataEntity, String> の意味：
 *   - SensorDataEntity : 操作対象のエンティティ
 *   - String           : 主キー（タイムスタンプ文字列）の型
 */
public interface SensorDataRepository extends JpaRepository<SensorDataEntity, String> {

    /**
     * sysId と date の両方に一致するレコードを、time 順（昇順）で全件取得します。
     *
     * 【メソッド名の読み方】
     * findBy + SysId + And + Date + OrderBy + Time
     *   ↓
     * "sysId が一致して、かつ date が一致するものを time で並べて返す"
     *
     * 自動生成される SQL のイメージ：
     *   SELECT * FROM sensor_data
     *   WHERE sys_id = ? AND date = ?
     *   ORDER BY time ASC
     *
     * 使用場面：
     *   特定のデバイス（sysId）のある日（date）のデータをグラフ表示する際に使います。
     */
    List<SensorDataEntity> findBySysIdAndDateOrderByTime(String sysId, String date);
}
