package com.example.mygaeel.service;

import com.example.mygaeel.model.ElTarget;
import com.google.cloud.datastore.*;
import org.springframework.stereotype.Service;

@Service
public class ElTargetService {

    private final Datastore datastore;

    public ElTargetService(Datastore datastore) {
        this.datastore = datastore;
    }

    public void save(ElTarget target) {
        Key key = datastore.newKeyFactory().setKind("ElTarget").newKey(target.getId());
        Entity entity = Entity.newBuilder(key)
                .set("regionId", target.getRegionId())
                .set("targetId", target.getTargetId())
                .set("targetName", target.getTargetName())
                .build();
        datastore.put(entity);
        System.out.println("ElTarget 登録: regionId=" + target.getRegionId() + ", targetId=" + target.getTargetId());
    }

    public Entity getTargetBySysId(String sysId) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("ElTarget")
                .setFilter(StructuredQuery.PropertyFilter.eq("targetId", sysId))
                .setLimit(1)
                .build();
        QueryResults<Entity> results = datastore.run(query);
        return results.hasNext() ? results.next() : null;
    }

    public Entity getTarget(String targetId) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("ElTarget")
                .setFilter(StructuredQuery.PropertyFilter.eq("targetId", targetId))
                .setLimit(1)
                .build();
        QueryResults<Entity> results = datastore.run(query);
        return results.hasNext() ? results.next() : null;
    }

    public String getRegionIdByTarget(String targetId) {
        Entity entity = getTarget(targetId);
        if (entity != null) {
            return entity.getString("regionId");
        }
        return null;
    }

    public Entity getTargetByKey(String regionId, String targetId) {
        Key key = datastore.newKeyFactory().setKind("ElTarget").newKey(regionId + "#" + targetId);
        return datastore.get(key);
    }
}
