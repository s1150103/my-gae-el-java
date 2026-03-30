package com.example.mygaeel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS（Cross-Origin Resource Sharing）の設定クラス。
 * 異なるドメイン（例：フロントエンドとバックエンドが別サーバー）からの
 * HTTPリクエストを許可するための設定。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * CORSのルールを登録する。
     * IoTデバイスや外部クライアントからのアクセスを許可するため、全オリジンを許可している。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")           // すべてのURLパスに適用
                .allowedOrigins("*")          // すべてのオリジン（ドメイン）を許可
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 許可するHTTPメソッド
                .allowedHeaders("*");         // すべてのリクエストヘッダーを許可
    }
}
