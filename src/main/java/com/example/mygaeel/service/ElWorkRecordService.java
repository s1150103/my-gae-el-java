package com.example.mygaeel.service;

import com.example.mygaeel.entity.ElWorkRecordEntity;
import com.example.mygaeel.repository.ElWorkRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElWorkRecordService {

    private final ElWorkRecordRepository repository;

    public ElWorkRecordService(ElWorkRecordRepository repository) {
        this.repository = repository;
    }

    public ElWorkRecordEntity save(ElWorkRecordEntity record) {
        ElWorkRecordEntity saved = repository.save(record);
        System.out.println("ElWorkRecord 保存: " + saved.getId());
        return saved;
    }

    public void updateEndTime(ElWorkRecordEntity record, long endTime) {
        record.setEndTime(endTime);
        repository.save(record);
        System.out.println("ElWorkRecord 更新: " + record.getId()
                + " (uptime: " + record.getUptime() + "秒)");
    }

    public List<ElWorkRecordEntity> queryByRegionId(String regionId) {
        return repository.findByRegionId(regionId);
    }

    public List<ElWorkRecordEntity> queryByYearMonthRegion(String year, String month, String regionId) {
        return repository.findByYearAndMonthAndRegionIdOrderByStartTime(year, month, regionId);
    }
}
