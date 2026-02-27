package com.example.mygaeel.repository;

import com.example.mygaeel.entity.SensorDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorDataRepository extends JpaRepository<SensorDataEntity, String> {

    List<SensorDataEntity> findBySysIdAndDateOrderByTime(String sysId, String date);
}
