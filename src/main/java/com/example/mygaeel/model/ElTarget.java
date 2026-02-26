package com.example.mygaeel.model;

public class ElTarget {
    private String id;
    private String regionId;
    private String targetId;
    private String targetName;

    public ElTarget(String regionId, String targetId, String targetName) {
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
