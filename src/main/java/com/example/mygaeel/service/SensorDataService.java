package com.example.mygaeel.service;

import com.example.mygaeel.model.SensorData;
import com.google.cloud.datastore.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SensorDataService {

    private final Datastore datastore;

    public SensorDataService(Datastore datastore) {
        this.datastore = datastore;
    }

    public void save(SensorData data) {
        Key key = datastore.newKeyFactory().setKind("SensorData").newKey(data.getId());
        Entity entity = Entity.newBuilder(key)
                .set("sysId", data.getSysId())
                .set("date", data.getDate())
                .set("time", data.getTime() != null ? data.getTime() : "")
                .set("udt", data.getUdt())
                .set("data1", data.getData1())
                .set("data2", data.getData2())
                .set("data3", data.getData3())
                .build();
        datastore.put(entity);
    }

    public List<Entity> queryBySysIdAndDate(String sysId, String date) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("SensorData")
                .setFilter(StructuredQuery.CompositeFilter.and(
                        StructuredQuery.PropertyFilter.eq("sysId", sysId),
                        StructuredQuery.PropertyFilter.eq("date", date)
                ))
                .setOrderBy(StructuredQuery.OrderBy.asc("time"))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        List<Entity> list = new ArrayList<>();
        results.forEachRemaining(list::add);
        return list;
    }

    /**
     * data1文字列を解析して {ch1: 0.0, ch2: 2.97, ...} の辞書形式に変換
     * "ch1 0.0,ch2 5.0,ch3 NA" → {"ch1": 0.0, "ch2": 5.0, "ch3": null}
     */
    public Map<String, Double> parseAllChannels(String data1) {
        Map<String, Double> channelData = new HashMap<>();
        boolean validDataFound = false;

        if (data1 == null || data1.isBlank()) return null;

        try {
            for (String item : data1.split(",")) {
                String[] parts = item.strip().split(" ");
                if (parts.length == 2 && parts[0].startsWith("ch")) {
                    String chName = parts[0];
                    try {
                        channelData.put(chName, Double.parseDouble(parts[1]));
                        validDataFound = true;
                    } catch (NumberFormatException e) {
                        channelData.put(chName, null);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("データ解析エラー: " + e.getMessage());
            return null;
        }

        if (!validDataFound) {
            System.out.println("すべてのデータが無効: " + data1);
            return null;
        }

        return channelData;
    }
}
