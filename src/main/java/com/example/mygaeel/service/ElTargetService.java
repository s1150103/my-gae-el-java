package com.example.mygaeel.service;

import com.example.mygaeel.model.ElTarget;
import com.google.cloud.datastore.*;
import org.springframework.stereotype.Service;

/**
 * ElTarget（監視対象設備）の Datastore 操作を担当するサービス。
 *
 * ElTarget は "regionId#targetId" をキーとして Datastore の "ElTarget" エンティティに保存される。
 */
@Service
public class ElTargetService {

    private final Datastore datastore; // Cloud Datastore クライアント

    public ElTargetService(Datastore datastore) {
        this.datastore = datastore;
    }

    /**
     * ElTarget を Datastore に保存（新規作成 or 上書き）する。
     *
     * @param target 保存する ElTarget オブジェクト
     */
    public void save(ElTarget target) {
        // "regionId#targetId" 形式のキーで Datastore エンティティを作成
        Key key = datastore.newKeyFactory().setKind("ElTarget").newKey(target.getId());
        Entity entity = Entity.newBuilder(key)
                .set("regionId", target.getRegionId())
                .set("targetId", target.getTargetId())
                .set("targetName", target.getTargetName())
                .build();
        datastore.put(entity); // 保存（存在すれば上書き）
        System.out.println("ElTarget 登録: regionId=" + target.getRegionId() + ", targetId=" + target.getTargetId());
    }

    /**
     * sysId（センサーID）から ElTarget エンティティを検索する。
     * ElTarget の "targetId" フィールドが sysId と一致するレコードを返す。
     *
     * @param sysId センサーのシステムID
     * @return 一致する Entity（見つからない場合は null）
     */
    public Entity getTargetBySysId(String sysId) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("ElTarget")
                .setFilter(StructuredQuery.PropertyFilter.eq("targetId", sysId))
                .setLimit(1) // 最初の1件のみ取得
                .build();
        QueryResults<Entity> results = datastore.run(query);
        return results.hasNext() ? results.next() : null;
    }

    /**
     * targetId から ElTarget エンティティを検索する。
     * getTargetBySysId と同じ処理（命名の違いに注意）。
     *
     * @param targetId 設備ID
     * @return 一致する Entity（見つからない場合は null）
     */
    public Entity getTarget(String targetId) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("ElTarget")
                .setFilter(StructuredQuery.PropertyFilter.eq("targetId", targetId))
                .setLimit(1)
                .build();
        QueryResults<Entity> results = datastore.run(query);
        return results.hasNext() ? results.next() : null;
    }

    /**
     * targetId から対応する regionId を取得する。
     *
     * @param targetId 設備ID
     * @return regionId（見つからない場合は null）
     */
    public String getRegionIdByTarget(String targetId) {
        Entity entity = getTarget(targetId);
        if (entity != null) {
            return entity.getString("regionId");
        }
        return null;
    }

    /**
     * regionId と targetId の複合キーで ElTarget エンティティを直接取得する。
     * キーが "regionId#targetId" の形式なので、キー検索が最も高速。
     *
     * @param regionId リージョンID
     * @param targetId 設備ID
     * @return 一致する Entity（見つからない場合は null）
     */
    public Entity getTargetByKey(String regionId, String targetId) {
        Key key = datastore.newKeyFactory().setKind("ElTarget").newKey(regionId + "#" + targetId);
        return datastore.get(key);
    }
}
