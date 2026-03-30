package com.example.mygaeel.model;

/**
 * 監視対象設備（ELターゲット）を表すモデルクラス。
 * Datastore の "ElTarget" エンティティに対応する。
 *
 * 設備はリージョン（地域）に紐づいており、
 * ID は "regionId#targetId" の形式で一意性を保つ。
 */
public class ElTarget {

    private String id;           // Datastore のキー。"regionId#targetId" の形式
    private String regionId;     // 設備が属するリージョンID
    private String targetId;     // 設備のID（センサーのsysIdと対応）
    private String targetName;   // 設備の表示名

    /**
     * @param regionId   リージョンID
     * @param targetId   ターゲット（設備）ID
     * @param targetName 設備名
     */
    public ElTarget(String regionId, String targetId, String targetName) {
        this.id = regionId + "#" + targetId; // "#" で結合してユニークなキーを生成
        this.regionId = regionId;
        this.targetId = targetId;
        this.targetName = targetName;
    }

    public String getId() { return id; }
    public String getRegionId() { return regionId; }
    public String getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
}
