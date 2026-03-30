package com.example.mygaeel.model;

import java.util.List;

/**
 * ユーザー情報を表すモデルクラス。
 * Datastore の "User" エンティティに対応する。
 *
 * ロールは "ADMIN"（管理者）と "INSPECTOR"（点検者）の2種類がある。
 */
public class User {

    private final String email;           // メールアドレス（ユーザーID兼ログインID）
    private final String passwordHash;    // BCryptでハッシュ化されたパスワード
    private final String role;            // "ADMIN" or "INSPECTOR"
    private final List<String> allowedRegions; // 閲覧可能なリージョンID一覧

    /**
     * @param email          メールアドレス
     * @param passwordHash   ハッシュ化済みパスワード
     * @param role           ロール（null の場合は "INSPECTOR" をデフォルト値として使用）
     * @param allowedRegions アクセス許可リージョンのリスト（null の場合は空リスト）
     */
    public User(String email, String passwordHash, String role, List<String> allowedRegions) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : "INSPECTOR"; // null なら INSPECTOR をデフォルトに
        this.allowedRegions = allowedRegions != null ? allowedRegions : List.of();
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public List<String> getAllowedRegions() { return allowedRegions; }
}
