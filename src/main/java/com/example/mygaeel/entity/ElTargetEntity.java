package com.example.mygaeel.entity;

import jakarta.persistence.*;

/**
 * ■ 監視対象（ELターゲット）テーブルのエンティティクラス
 *
 * IoTデバイスが設置されている監視対象機器の情報を管理します。
 * PostgreSQL の "el_targets" テーブルに対応しています。
 *
 * 【このシステムの構成イメージ】
 *   リージョン（地域）
 *     └─ ターゲット（監視対象機器）
 *           └─ センサー（IoTデバイス）が定期的にデータを送信
 *
 * 例：regionId="6", targetId="DAQA005", targetName="北塩原村　第３水源"
 */
@Entity
@Table(name = "el_targets")
public class ElTargetEntity {

    /**
     * 主キー。"regionId#targetId" の形式で生成します。
     * 例：regionId="6", targetId="DAQA005" → id="6#DAQA005"
     *
     * この方法で「同じリージョン内に同じtargetIdは登録できない」という一意性を保証します。
     */
    @Id
    @Column(name = "id", nullable = false)
    private String id;  // "regionId#targetId" 形式

    /** リージョン（地域）のID。どの地域に属する機器かを示します。 */
    @Column(name = "region_id", nullable = false)
    private String regionId;

    /** ターゲット（監視対象機器）のID。IoTデバイスから送られてくる sysId と対応します。 */
    @Column(name = "target_id", nullable = false)
    private String targetId;

    /** ターゲットの表示名（画面に表示する名前）。例：「北塩原村　第３水源」 */
    @Column(name = "target_name", nullable = false)
    private String targetName;

    /** JPA 用の引数なしコンストラクタ（外部から直接呼ばないでください）。 */
    protected ElTargetEntity() {}

    /**
     * 監視対象を新規作成するコンストラクタ。
     * id は regionId と targetId を "#" で連結して自動生成します。
     */
    public ElTargetEntity(String regionId, String targetId, String targetName) {
        this.id = regionId + "#" + targetId;  // "6#DAQA005" のような形式
        this.regionId = regionId;
        this.targetId = targetId;
        this.targetName = targetName;
    }

    // ── ゲッター ───────────────────────────────────────────────────────
    public String getId() { return id; }
    public String getRegionId() { return regionId; }
    public String getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
}
