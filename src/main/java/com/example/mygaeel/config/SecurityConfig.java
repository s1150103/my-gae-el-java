package com.example.mygaeel.config;

import com.example.mygaeel.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security の設定クラス。
 * 認証（ログイン）・認可（アクセス制御）・パスワード暗号化などを設定する。
 */
@Configuration
@EnableWebSecurity // Spring Security を有効化するアノテーション
public class SecurityConfig {

    /**
     * パスワードのハッシュ化に使う BCrypt エンコーダーを Bean 登録する。
     * BCrypt は強力なハッシュアルゴリズムで、パスワードの安全な保存に使われる。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ユーザー認証プロバイダーを設定する。
     * UserService からユーザー情報を取得し、BCrypt でパスワードを照合する。
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);  // ユーザー情報の取得元
        provider.setPasswordEncoder(passwordEncoder()); // パスワードの照合方法
        return provider;
    }

    /**
     * 認証マネージャーを Bean 登録する。
     * ログイン処理の中核となるオブジェクト。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * URLごとのアクセス制御・ログイン・ログアウトの設定を行う。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 認証不要（IoTデバイス・静的ファイル・認証ページ）
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/css/**", "/js/**", "/lib/**", "/*.css", "/*.png").permitAll()
                .requestMatchers("/ellighttracker2").permitAll()  // IoTデバイスからのデータ受信
                // 管理者専用（ADMIN ロールのみアクセス可）
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 上記以外はすべてログイン必須
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")           // カスタムログインページのURL
                .loginProcessingUrl("/login")  // ログインフォームのPOST送信先
                .usernameParameter("email")    // フォームのユーザー名フィールド名
                .passwordParameter("password") // フォームのパスワードフィールド名
                .defaultSuccessUrl("/", true)  // ログイン成功後のリダイレクト先
                .failureUrl("/login?error=true") // ログイン失敗後のリダイレクト先
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")                      // ログアウトのURL
                .logoutSuccessUrl("/login?logout=true")    // ログアウト後のリダイレクト先
                .permitAll()
            )
            // GAEのヘルスチェック・AJAX対応のためCSRFを無効化
            // ※本番環境でCSRFを有効にする場合はフロントエンド側の対応が必要
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
