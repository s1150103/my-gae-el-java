package com.example.mygaeel.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security が認証に使うユーザー情報クラス。
 * UserDetails インターフェースを実装することで、Spring Security と連携できる。
 *
 * ログイン中のユーザー情報（ロール・アクセス可能リージョンなど）を保持する。
 */
public class CustomUserDetails implements UserDetails {

    private final String email;                  // ログインID（メールアドレス）
    private final String password;               // ハッシュ化済みパスワード
    private final String role;                   // "ADMIN" or "INSPECTOR"
    private final List<String> allowedRegions;   // アクセス許可されたリージョンIDのリスト

    public CustomUserDetails(String email, String password, String role, List<String> allowedRegions) {
        this.email = email;
        this.password = password;
        this.role = role != null ? role : "INSPECTOR";
        this.allowedRegions = allowedRegions != null ? allowedRegions : List.of();
    }

    /**
     * このユーザーが持つ権限（ロール）のリストを返す。
     * Spring Security は "ROLE_" プレフィックスを付けたロール名を使う。
     * 例: "ADMIN" → "ROLE_ADMIN"
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }          // Spring Security はメールをユーザー名として使う

    // アカウントの有効状態（すべて true = 有効）
    @Override public boolean isAccountNonExpired() { return true; }    // アカウント期限切れでない
    @Override public boolean isAccountNonLocked() { return true; }     // アカウントロックされていない
    @Override public boolean isCredentialsNonExpired() { return true; } // パスワードの期限切れでない
    @Override public boolean isEnabled() { return true; }              // アカウントが有効

    public String getRole() { return role; }
    public List<String> getAllowedRegions() { return allowedRegions; }

    /** このユーザーが管理者かどうかを返す */
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
