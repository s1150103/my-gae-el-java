package com.example.mygaeel.service;

import com.example.mygaeel.model.ElWorkRecord;
import com.google.cloud.datastore.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ElWorkRecord（設備稼働レコード）の Datastore 操作を担当するサービス。
 *
 * 稼働レコードは「センサー値が 3.0 を超えた瞬間に開始し、
 * 3.0 を下回った瞬間に終了」として記録される。
 */
@Service
public class ElWorkRecordService {

    private final Datastore datastore;

    public ElWorkRecordService(Datastore datastore) {
        this.datastore = datastore;
    }

    /**
     * ElWorkRecord を Datastore に保存する。
     * 稼働開始時（endTime=null）と、maxData 更新時の両方で呼ばれる。
     *
     * @param record 保存する ElWorkRecord オブジェクト
     */
    public void save(ElWorkRecord record) {
        Key key = datastore.newKeyFactory().setKind("ElWorkRecord").newKey(record.getId());
        Entity.Builder builder = Entity.newBuilder(key)
                .set("regionId", record.getRegionId())
                .set("targetId", record.getTargetId())
                .set("startTime", record.getStartTime())
                .set("maxData", record.getMaxData())
                .set("dateString", record.getDateString()) // "yyyy-MM-dd" 形式
                .set("year", record.getYear())
                .set("month", record.getMonth())
                .set("date", record.getDate());

        // endTime がある場合は稼働時間（秒）も計算して保存
        if (record.getEndTime() != null) {
            long uptime = (record.getEndTime() - record.getStartTime()) / 1000; // ミリ秒→秒
            builder.set("endTime", record.getEndTime());
            builder.set("uptime", uptime);
        } else {
            // 稼働中（終了時刻未確定）は null として保存
            builder.setNull("endTime");
            builder.setNull("uptime");
        }

        datastore.put(builder.build());
        System.out.println("ElWorkRecord 保存: " + record.getId());
    }

    /**
     * 既存の ElWorkRecord に終了時刻を更新する（稼働終了時に呼ばれる）。
     * Datastore から最新のエンティティを取得し、endTime と uptime を追記する。
     *
     * @param record  更新対象の ElWorkRecord
     * @param endTime 稼働終了時刻（エポックミリ秒）
     */
    public void updateEndTime(ElWorkRecord record, long endTime) {
        Key key = datastore.newKeyFactory().setKind("ElWorkRecord").newKey(record.getId());
        Entity entity = datastore.get(key);
        if (entity != null) {
            long uptime = (endTime - record.getStartTime()) / 1000; // 稼働時間を秒で計算
            Entity updated = Entity.newBuilder(entity)
                    .set("endTime", endTime)
                    .set("uptime", uptime)
                    .build();
            datastore.put(updated);
            record.setEndTime(endTime); // メモリ上の状態も更新
            System.out.println("ElWorkRecord 更新: " + record.getId() + " (uptime: " + uptime + "秒)");
        }
    }

    /**
     * 指定リージョンの全稼働レコードを取得する。
     *
     * @param regionId リージョンID
     * @return 稼働レコードのリスト
     */
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

    /**
     * 年・月・リージョンIDを指定して稼働レコードを取得する（月次集計用）。
     * 開始時刻の昇順でソートされる。
     *
     * @param year     年（例: "2024"）
     * @param month    月（例: "03"、ゼロ埋め必須）
     * @param regionId リージョンID
     * @return 稼働レコードのリスト（startTime 昇順）
     */
    public List<Entity> queryByYearMonthRegion(String year, String month, String regionId) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("ElWorkRecord")
                .setFilter(StructuredQuery.CompositeFilter.and(
                        StructuredQuery.PropertyFilter.eq("year", year),
                        StructuredQuery.PropertyFilter.eq("month", month),
                        StructuredQuery.PropertyFilter.eq("regionId", regionId)
                ))
                .setOrderBy(StructuredQuery.OrderBy.asc("startTime")) // 開始時刻で昇順ソート
                .build();
        QueryResults<Entity> results = datastore.run(query);
        List<Entity> list = new ArrayList<>();
        results.forEachRemaining(list::add);
        return list;
    }
}
