package com.example.mygaeel.service;

import com.example.mygaeel.model.ElState;
import com.example.mygaeel.model.ElWorkRecord;
import com.example.mygaeel.model.SensorData;
import com.google.cloud.datastore.Entity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ElStateService {

    private final Map<String, ElState> stateMap = new ConcurrentHashMap<>();
    private final ElTargetService elTargetService;
    private final ElWorkRecordService elWorkRecordService;

    // センサーデータ受信時に動的に追加するマップ（ElStateManager の el_state_map 相当）
    private final Map<String, ElState> dynamicStateMap = new ConcurrentHashMap<>();

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

    /**
     * センサーデータを受信して状態を更新する（/ellighttracker2 mode=d から呼び出し）
     * Python の el_state_map と ElState.update_state() をまとめた実装
     */
    public void updateState(SensorData sensorData) {
        String sysId = sensorData.getSysId();

        // ElTarget から regionId を取得
        Entity targetEntity = elTargetService.getTargetBySysId(sysId);
        if (targetEntity == null) {
            System.out.println("sysId " + sysId + " に対応する targetId が見つかりません");
            return;
        }

        String targetId = targetEntity.getString("targetId");
        String regionId = targetEntity.getString("regionId");

        // dynamicStateMap に ElState がなければ作成
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
            Double newValueRaw = parsedData.get(chKey);
            double newValue = newValueRaw != null ? newValueRaw : 0.0;

            int chNum;
            try {
                chNum = Integer.parseInt(chKey.replace("ch", ""));
            } catch (NumberFormatException e) {
                continue;
            }

            Map<String, ElWorkRecord> activeRecords = state.getActiveRecords();

            if (prevValue <= 3.0 && newValue > 3.0) {
                long startTime = System.currentTimeMillis();
                System.out.println("ElWorkRecord 作成: " + regionId + " - " + chNum + " (開始: " + startTime + ")");
                ElWorkRecord record = new ElWorkRecord(regionId, targetId, startTime, newValue);
                elWorkRecordService.save(record);
                activeRecords.put(chKey, record);

            } else if (prevValue >= 3.0 && newValue < 3.0 && activeRecords.containsKey(chKey)) {
                long endTime = System.currentTimeMillis();
                elWorkRecordService.updateEndTime(activeRecords.get(chKey), endTime);
                activeRecords.remove(chKey);
                System.out.println("ElWorkRecord 更新: " + regionId + " - " + chNum + " (終了: " + endTime + ")");

            } else if (newValue > 3.0 && activeRecords.containsKey(chKey)) {
                ElWorkRecord record = activeRecords.get(chKey);
                if (record.getMaxData() < newValue) {
                    record.setMaxData(newValue);
                    elWorkRecordService.save(record);
                    System.out.println("ElWorkRecord 更新: maxData=" + newValue);
                }
            }

            lastData.put(chKey, newValue);
        }
    }

    public Map<String, ElState> getDynamicStateMap() {
        return dynamicStateMap;
    }
}
