package com.example.mygaeel.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ■ ユーザーテーブルのエンティティクラス
 *
 * 【エンティティとは？】
 * データベースのテーブル1行分を Java のオブジェクトとして表したものです。
 * JPA（Java Persistence API）という仕組みを使うと、SQL を書かなくても
 * Java オブジェクトの操作だけで DB の読み書きができます。
 *
 * このクラスは PostgreSQL の "users" テーブルに対応しています。
 *
 * @Entity : このクラスが DB テーブルと対応していることを JPA に伝えます
 * @Table  : 対応するテーブル名を指定します（省略すると クラス名が使われる）
 */
@Entity
@Table(name = "users")
public class UserEntity {

    /**
     * 主キー（Primary Key）= テーブルの各行を一意に識別する列。
     * このシステムでは email アドレスをIDとして使用しています。
     *
     * @Id     : この フィールドが主キーであることを示します
     * @Column : DB の列名と Java フィールドを対応付けます
     *           nullable = false → NULL 値を許可しない（必須項目）
     */
    @Id
    @Column(name = "email", nullable = false)
    private String email;

    /**
     * パスワードのハッシュ値（BCrypt で暗号化済みの文字列）。
     * 生のパスワードはDBに保存せず、ハッシュ値のみ保存します。
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * ユーザーの権限ロール。
     * "ADMIN"（管理者）または "INSPECTOR"（点検者）のいずれかが入ります。
     */
    @Column(name = "role", nullable = false)
    private String role;

    /**
     * ユーザーがアクセスを許可されているリージョン（地域）のIDリスト。
     *
     * @ElementCollection : このフィールドが別テーブルに保存されるコレクションであることを示します
     * @CollectionTable   : コレクションを格納する別テーブルの情報を指定
     *                      name="user_allowed_regions" → テーブル名
     *                      joinColumns → このテーブルと users テーブルの結合キー
     * FetchType.EAGER   : ユーザー情報を取得するとき、アクセス可能リージョンも同時に取得する設定
     *
     * 例：ユーザー "tanaka@example.com" が regionId "1" と "3" にアクセス可能な場合、
     *     allowedRegions = ["1", "3"] となります
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_allowed_regions",
                     joinColumns = @JoinColumn(name = "email"))
    @Column(name = "region_id")
    private List<String> allowedRegions = new ArrayList<>();

    /**
     * JPA が内部で使う引数なしコンストラクタ（直接呼ばないでください）。
     * protected にすることで外部からの呼び出しを防ぎつつ JPA の要件を満たします。
     */
    protected UserEntity() {}

    /**
     * ユーザーを新規作成するコンストラクタ。
     *
     * @param email        メールアドレス（ログインID）
     * @param passwordHash BCrypt でハッシュ化済みのパスワード
     * @param role         権限ロール（null の場合は "INSPECTOR" をデフォルト値として使用）
     */
    public UserEntity(String email, String passwordHash, String role) {
        this.email = email;
        this.passwordHash = passwordHash;
        // role が null の場合は安全のため "INSPECTOR" をデフォルト値にする
        this.role = role != null ? role : "INSPECTOR";
    }

    // ── ゲッター（フィールドの値を取得するメソッド） ──────────────────
    // private フィールドは外部から直接アクセスできないため、ゲッターを通じて値を取得します
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public List<String> getAllowedRegions() { return allowedRegions; }

    // ── セッター（フィールドの値を変更するメソッド） ──────────────────
    // 変更が必要なフィールドにのみセッターを定義します（email は変更不可のためなし）
    public void setRole(String role) { this.role = role; }

    public void setAllowedRegions(List<String> allowedRegions) {
        // null が渡された場合は空リストをセットして NullPointerException を防ぐ
        this.allowedRegions = allowedRegions != null ? allowedRegions : new ArrayList<>();
    }
}
