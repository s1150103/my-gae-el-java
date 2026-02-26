package com.example.mygaeel.service;

import com.example.mygaeel.model.ElWorkRecord;
import com.google.cloud.datastore.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElWorkRecordService {

    private final Datastore datastore;

    public ElWorkRecordService(Datastore datastore) {
        this.datastore = datastore;
    }

    public void save(ElWorkRecord record) {
        Key key = datastore.newKeyFactory().setKind("ElWorkRecord").newKey(record.getId());
        Entity.Builder builder = Entity.newBuilder(key)
                .set("regionId", record.getRegionId())
                .set("targetId", record.getTargetId())
                .set("startTime", record.getStartTime())
                .set("maxData", record.getMaxData())
                .set("dateString", record.getDateString())
                .set("year", record.getYear())
                .set("month", record.getMonth())
                .set("date", record.getDate());

        if (record.getEndTime() != null) {
            long uptime = (record.getEndTime() - record.getStartTime()) / 1000;
            builder.set("endTime", record.getEndTime());
            builder.set("uptime", uptime);
        } else {
            builder.setNull("endTime");
            builder.setNull("uptime");
        }

        datastore.put(builder.build());
        System.out.println("ElWorkRecord 保存: " + record.getId());
    }

    public void updateEndTime(ElWorkRecord record, long endTime) {
        Key key = datastore.newKeyFactory().setKind("ElWorkRecord").newKey(record.getId());
        Entity entity = datastore.get(key);
        if (entity != null) {
            long uptime = (endTime - record.getStartTime()) / 1000;
            Entity updated = Entity.newBuilder(entity)
                    .set("endTime", endTime)
                    .set("uptime", uptime)
                    .build();
            datastore.put(updated);
            record.setEndTime(endTime);
            System.out.println("ElWorkRecord 更新: " + record.getId() + " (uptime: " + uptime + "秒)");
        }
    }

    public List<Entity> queryByRegionId(String regionId) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("ElWorkRecord")
                .setFilter(StructuredQuery.PropertyFilter.eq("regionId", regionId))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        List<Entity> list = new ArrayList<>();
        results.forEachRemaining(list::add);
        return list;
    }

    public List<Entity> queryByYearMonthRegion(String year, String month, String regionId) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("ElWorkRecord")
                .setFilter(StructuredQuery.CompositeFilter.and(
                        StructuredQuery.PropertyFilter.eq("year", year),
                        StructuredQuery.PropertyFilter.eq("month", month),
                        StructuredQuery.PropertyFilter.eq("regionId", regionId)
                ))
                .setOrderBy(StructuredQuery.OrderBy.asc("startTime"))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        List<Entity> list = new ArrayList<>();
        results.forEachRemaining(list::add);
        return list;
    }
}
