package com.example.mygaeel.repository;

import com.example.mygaeel.entity.ElWorkRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ElWorkRecordRepository extends JpaRepository<ElWorkRecordEntity, String> {

    List<ElWorkRecordEntity> findByRegionId(String regionId);

    List<ElWorkRecordEntity> findByYearAndMonthAndRegionIdOrderByStartTime(
            String year, String month, String regionId);
}
