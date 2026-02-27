package com.example.mygaeel.service;

import com.example.mygaeel.entity.ElTargetEntity;
import com.example.mygaeel.entity.ElWorkRecordEntity;
import com.example.mygaeel.model.ElState;
import com.example.mygaeel.model.SensorData;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ElStateService {

    private final Map<String, ElState> stateMap = new ConcurrentHashMap<>();
    private final Map<String, ElState> dynamicStateMap = new ConcurrentHashMap<>();

    private final ElTargetService elTargetService;
    private final ElWorkRecordService elWorkRecordService;

    public ElStateService(ElTargetService elTargetService, ElWorkRecordService elWorkRecordService) {
        this.elTargetService = elTargetService;
        this.elWorkRecordService = elWorkRecordService;
    }

    @PostConstruct
    public void initialize() {
        try {
            InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("EoE_Eden_Number.xml");
            if (xmlStream == null) {
                System.out.println("EoE_Eden_Number.xml が見つかりません");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlStream);
            doc.getDocumentElement().normalize();

            NodeList regions = doc.getElementsByTagName("region");
            for (int i = 0; i < regions.getLength(); i++) {
                org.w3c.dom.Element region = (org.w3c.dom.Element) regions.item(i);
                String regionId = region.getAttribute("num");
                NodeList clients = region.getElementsByTagName("client");
                for (int j = 0; j < clients.getLength(); j++) {
                    org.w3c.dom.Element client = (org.w3c.dom.Element) clients.item(j);
                    String sysId = client.getAttribute("sysid");
                    System.out.println("ElState 登録: sysId=" + sysId + ", regionId=" + regionId);
                    stateMap.put(sysId, new ElState(sysId, regionId));
                }
            }
            System.out.println("ElStateService 初期化完了 (" + stateMap.size() + "件)");
        } catch (Exception e) {
            System.out.println("ElStateService 初期化エラー: " + e.getMessage());
        }
    }

    public void updateState(SensorData sensorData) {
        String sysId = sensorData.getSysId();

        Optional<ElTargetEntity> targetOpt = elTargetService.getTargetBySysId(sysId);
        if (targetOpt.isEmpty()) {
            System.out.println("sysId " + sysId + " に対応する targetId が見つかりません");
            return;
        }

        ElTargetEntity target = targetOpt.get();
        String targetId = target.getTargetId();
        String regionId = target.getRegionId();

        dynamicStateMap.computeIfAbsent(sysId, id -> {
            System.out.println("ElState 作成: sysId=" + sysId + ", regionId=" + regionId);
            return new ElState(sysId, regionId);
        });

        ElState state = dynamicStateMap.get(sysId);
        Map<String, Double> parsedData = state.parseSensorData(sensorData.getData1());

        if (state.getLastData() == null) {
            Map<String, Double> init = new HashMap<>();
            for (String ch : parsedData.keySet()) init.put(ch, 0.0);
            state.setLastData(init);
        }

        Map<String, Double> lastData = state.getLastData();

        for (Map.Entry<String, Double> entry : lastData.entrySet()) {
            String chKey = entry.getKey();
            double prevValue = entry.getValue() != null ? entry.getValue() : 0.0;
            double newValue = parsedData.getOrDefault(chKey, 0.0) != null
                    ? parsedData.getOrDefault(chKey, 0.0) : 0.0;

            int chNum;
            try {
                chNum = Integer.parseInt(chKey.replace("ch", ""));
            } catch (NumberFormatException e) {
                continue;
            }

            Map<String, ElWorkRecordEntity> activeRecords = state.getActiveRecords();

            if (prevValue <= 3.0 && newValue > 3.0) {
                long startTime = System.currentTimeMillis();
                System.out.println("ElWorkRecord 作成: " + regionId + " - " + chNum);
                ElWorkRecordEntity record = new ElWorkRecordEntity(regionId, targetId, startTime, newValue);
                elWorkRecordService.save(record);
                activeRecords.put(chKey, record);

            } else if (prevValue >= 3.0 && newValue < 3.0 && activeRecords.containsKey(chKey)) {
                long endTime = System.currentTimeMillis();
                elWorkRecordService.updateEndTime(activeRecords.get(chKey), endTime);
                activeRecords.remove(chKey);
                System.out.println("ElWorkRecord 更新: " + regionId + " - " + chNum + " (終了)");

            } else if (newValue > 3.0 && activeRecords.containsKey(chKey)) {
                ElWorkRecordEntity record = activeRecords.get(chKey);
                if (record.getMaxData() < newValue) {
                    record.setMaxData(newValue);
                    elWorkRecordService.save(record);
                }
            }

            lastData.put(chKey, newValue);
        }
    }

    public Map<String, ElState> getDynamicStateMap() {
        return dynamicStateMap;
    }

    public List<String> getKnownRegionIds() {
        return stateMap.values().stream()
                .map(ElState::getRegionId)
                .distinct()
                .sorted()
                .toList();
    }
}
