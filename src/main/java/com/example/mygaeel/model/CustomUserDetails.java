package com.example.mygaeel.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * ■ Spring Security が内部で使うユーザー情報クラス
 *
 * 【なぜこのクラスが必要か？】
 * Spring Security は認証済みユーザーの情報を「UserDetails」という決まった形式で管理します。
 * しかしデフォルトの UserDetails には「アクセス可能なリージョン」などの独自フィールドを
 * 追加できません。そこでこのクラスで UserDetails を実装（implements）し、
 * カスタムフィールドを追加しています。
 *
 * 【implements UserDetails とは？】
 * UserDetails インターフェースを「実装する」= 決められたメソッドを全部書くことを約束する
 * Spring Security は UserDetails を受け取る前提で動くため、このクラスはその約束を守っています。
 */
public class CustomUserDetails implements UserDetails {

    private final String email;
    private final String password;  // BCrypt ハッシュ済みパスワード
    private final String role;      // "ADMIN" または "INSPECTOR"
    private final List<String> allowedRegions;  // アクセス可能なリージョンIDのリスト

    /**
     * コンストラクタ。UserService.loadUserByUsername() から呼ばれます。
     * DB から取得したユーザー情報をもとに生成されます。
     */
    public CustomUserDetails(String email, String password, String role, List<String> allowedRegions) {
        this.email = email;
        this.password = password;
        this.role = role != null ? role : "INSPECTOR";
        this.allowedRegions = allowedRegions != null ? allowedRegions : List.of();
    }

    /**
     * このユーザーが持つ権限（ロール）のリストを返します。
     * Spring Security はこのメソッドで「このユーザーが何のロールを持つか」を判断します。
     *
     * "ROLE_" プレフィックスは Spring Security の慣習です。
     * - role = "ADMIN"     → "ROLE_ADMIN"
     * - role = "INSPECTOR" → "ROLE_INSPECTOR"
     *
     * SecurityConfig の .hasRole("ADMIN") は内部で "ROLE_ADMIN" と照合します。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /** BCrypt ハッシュ済みパスワードを返します。Spring Security が認証時に使います。 */
    @Override
    public String getPassword() { return password; }

    /**
     * ユーザー名（= メールアドレス）を返します。
     * Spring Security はログインフォームの "email" フィールドをここと照合します。
     */
    @Override
    public String getUsername() { return email; }

    // 以下4つは「アカウントの有効期限」や「ロック状態」を管理するメソッドです。
    // このシステムではすべて true（=有効）固定にしています。
    @Override public boolean isAccountNonExpired() { return true; }   // アカウントは期限切れでない
    @Override public boolean isAccountNonLocked() { return true; }    // アカウントはロックされていない
    @Override public boolean isCredentialsNonExpired() { return true; }// パスワードは期限切れでない
    @Override public boolean isEnabled() { return true; }              // アカウントは有効

    // ── 独自追加フィールドのゲッター ──────────────────────────────────

    /** ロール文字列を直接返します。例："ADMIN" */
    public String getRole() { return role; }

    /** アクセス可能なリージョンIDのリストを返します。 */
    public List<String> getAllowedRegions() { return allowedRegions; }

    /**
     * このユーザーが管理者かどうかを判定します。
     * RegionAccessService などで簡潔にチェックするための便利メソッドです。
     */
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
