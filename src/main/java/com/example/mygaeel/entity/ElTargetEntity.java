package com.example.mygaeel.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "el_targets")
public class ElTargetEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;  // "regionId#targetId"

    @Column(name = "region_id", nullable = false)
    private String regionId;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(name = "target_name", nullable = false)
    private String targetName;

    protected ElTargetEntity() {}

    public ElTargetEntity(String regionId, String targetId, String targetName) {
        this.id = regionId + "#" + targetId;
        this.regionId = regionId;
        this.targetId = targetId;
        this.targetName = targetName;
    }

    public String getId() { return id; }
    public String getRegionId() { return regionId; }
    public String getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
}
