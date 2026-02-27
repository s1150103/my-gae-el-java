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
 * ■ セキュリティ設定クラス
 *
 * 「誰がどのページにアクセスできるか」「ログイン・ログアウトの動作」を定義します。
 * Spring Security というライブラリを使って、認証・認可を管理しています。
 *
 * @Configuration   : Spring の設定クラスとして認識させるアノテーション
 * @EnableWebSecurity: Spring Security を有効化するアノテーション
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * パスワードのハッシュ化ロジックを定義します。
     *
     * 【BCryptとは？】
     * パスワードを「元に戻せない形式」に変換する暗号化アルゴリズムです。
     * DB にはハッシュ化されたパスワードのみ保存され、万が一 DB が漏洩しても
     * 元のパスワードは推測困難です。
     *
     * @Bean : このメソッドの戻り値を Spring が管理するオブジェクト（Bean）として登録します。
     *         他のクラスで「PasswordEncoder を使いたい」と書くと、Spring が自動でここで作った
     *         オブジェクトを渡してくれます（依存性注入）。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 「ユーザー情報をどこから取得してログイン認証するか」を設定します。
     *
     * DaoAuthenticationProvider = DBからユーザー情報を取得して認証する仕組み
     *   - setUserDetailsService : ユーザー情報の取得元（UserService = DBから取得）を指定
     *   - setPasswordEncoder    : パスワードの照合方法（BCrypt）を指定
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * 認証処理を実行するマネージャーを登録します。
     * 主にプログラムから手動でログイン処理を呼び出す際に使います。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * ■ アクセス制御のルールを定義するメソッド（最重要）
     *
     * どのURLに誰がアクセスできるか、ログイン・ログアウトの挙動をまとめて設定します。
     * HttpSecurity は「セキュリティの設定書」のようなものです。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── アクセス権限のルール ──────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // 認証不要（ログインしていなくても見られるページ）
                .requestMatchers("/login", "/register").permitAll()
                // 静的ファイル（CSS、JS、画像）も認証不要
                .requestMatchers("/css/**", "/js/**", "/lib/**", "/*.css", "/*.png").permitAll()
                // IoTデバイスからのデータ送信エンドポイントも認証不要（デバイスはログインできないため）
                .requestMatchers("/ellighttracker2").permitAll()
                // /admin/ 以下は ADMIN ロールを持つユーザーのみアクセス可
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 上記以外のすべてのページはログインが必須
                .anyRequest().authenticated()
            )

            // ── ログイン設定 ─────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")           // ログインページの URL（GET）
                .loginProcessingUrl("/login")  // フォーム送信先の URL（POST）。Spring Security が自動処理
                .usernameParameter("email")    // フォームの「ユーザー名」フィールド名（デフォルトは username）
                .passwordParameter("password") // フォームの「パスワード」フィールド名
                .defaultSuccessUrl("/", true)  // ログイン成功後のリダイレクト先
                .failureUrl("/login?error=true") // ログイン失敗時のリダイレクト先
                .permitAll()
            )

            // ── ログアウト設定 ───────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")                   // ログアウトを実行する URL（POST）
                .logoutSuccessUrl("/login?logout=true") // ログアウト後のリダイレクト先
                .permitAll()
            )

            // CSRF 保護を無効化（IoT デバイスからの Ajax リクエスト対応のため）
            // ※ CSRF とは：悪意あるサイトから勝手にフォームを送信させる攻撃への対策
            //    今回は IoT デバイスが直接 API を叩く構成のため無効化しています
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
