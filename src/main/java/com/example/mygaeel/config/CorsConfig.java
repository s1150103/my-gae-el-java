package com.example.mygaeel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ■ CORS（クロスオリジン）設定クラス
 *
 * 【CORSとは？】
 * ブラウザのセキュリティ機能で、「異なるドメインからのHTTPリクエストはデフォルトでブロックする」という制限です。
 * 例：フロントエンドが http://localhost:3000 で動いていて、
 *     バックエンドが http://localhost:8080 の場合、ポートが違うので「別ドメイン」扱いになります。
 *
 * このクラスでその制限を緩和し、IoTデバイスやフロントエンドからのリクエストを受け付けられるようにします。
 *
 * @Configuration : このクラスが「設定クラス」であることを Spring に伝えます。
 *                  アプリ起動時に読み込まれます。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * CORS のルールを追加するメソッド。
     * WebMvcConfigurer インターフェースのメソッドを上書き（@Override）しています。
     *
     * 設定内容：
     *   - addMapping("/**")          : すべてのURLパスに対してCORS設定を適用
     *   - allowedOrigins("*")        : どのドメインからのリクエストも許可（本番環境では絞り込みを検討）
     *   - allowedMethods(...)        : GET/POST/PUT/DELETE/OPTIONS の5種類のHTTPメソッドを許可
     *   - allowedHeaders("*")        : すべてのリクエストヘッダーを許可
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
