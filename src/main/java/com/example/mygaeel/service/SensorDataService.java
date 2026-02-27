package com.example.mygaeel.service;

import com.example.mygaeel.entity.SensorDataEntity;
import com.example.mygaeel.repository.SensorDataRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SensorDataService {

    private final SensorDataRepository repository;

    public SensorDataService(SensorDataRepository repository) {
        this.repository = repository;
    }

    public SensorDataEntity save(SensorDataEntity entity) {
        return repository.save(entity);
    }

    public List<SensorDataEntity> queryBySysIdAndDate(String sysId, String date) {
        return repository.findBySysIdAndDateOrderByTime(sysId, date);
    }

    /**
     * data1文字列を解析して {ch1: 0.0, ch2: 2.97, ...} の辞書形式に変換
     */
    public Map<String, Double> parseAllChannels(String data1) {
        Map<String, Double> channelData = new HashMap<>();
        boolean validDataFound = false;

        if (data1 == null || data1.isBlank()) return null;

        try {
            for (String item : data1.split(",")) {
                String[] parts = item.strip().split(" ");
                if (parts.length == 2 && parts[0].startsWith("ch")) {
                    try {
                        channelData.put(parts[0], Double.parseDouble(parts[1]));
                        validDataFound = true;
                    } catch (NumberFormatException e) {
                        channelData.put(parts[0], null);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("データ解析エラー: " + e.getMessage());
            return null;
        }

        return validDataFound ? channelData : null;
    }
}
