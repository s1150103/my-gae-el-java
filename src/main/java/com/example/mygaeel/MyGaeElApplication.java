package com.example.mygaeel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ■ アプリケーションの起動クラス
 *
 * Spring Boot アプリの「玄関口」です。
 * このクラスの main メソッドを実行すると、サーバーが起動します。
 *
 * @SpringBootApplication を付けることで、以下の3つが自動的に有効になります：
 *   - @Configuration  : このクラス自体を設定クラスとして扱う
 *   - @EnableAutoConfiguration : Spring Boot の自動設定を有効化（DB接続なども自動でセットアップ）
 *   - @ComponentScan  : 同じパッケージ以下にある @Service, @Controller などを自動検出
 */
@SpringBootApplication
public class MyGaeElApplication {

    /**
     * Java アプリの起動メソッド。
     * SpringApplication.run() を呼ぶと、Tomcat（内蔵Webサーバー）が起動し
     * HTTP リクエストを受け付けられる状態になります。
     */
    public static void main(String[] args) {
        SpringApplication.run(MyGaeElApplication.class, args);
    }
}
