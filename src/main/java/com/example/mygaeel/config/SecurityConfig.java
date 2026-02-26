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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 認証不要（IoTデバイス・静的ファイル・認証ページ）
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/css/**", "/js/**", "/lib/**", "/*.css", "/*.png").permitAll()
                .requestMatchers("/ellighttracker2").permitAll()  // IoTデバイスからのデータ受信
                // 上記以外はすべてログイン必須
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")           // カスタムログインページ
                .loginProcessingUrl("/login")  // POSTを受け取るURL
                .usernameParameter("email")    // フォームのフィールド名
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)  // ログイン成功後
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            // GAEのヘルスチェック・AJAX対応のためCSRFを無効化
            // ※本番環境でCSRFを有効にする場合はフロントエンド側の対応が必要
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
