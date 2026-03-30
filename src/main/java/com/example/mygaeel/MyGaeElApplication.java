package com.example.mygaeel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * アプリケーションのエントリーポイント（起動クラス）。
 * @SpringBootApplication を付けることで、Spring Boot の自動設定が有効になる。
 */
@SpringBootApplication
public class MyGaeElApplication {

    /**
     * アプリケーションを起動するメインメソッド。
     * SpringApplication.run() を呼び出すことで、組み込みTomcatサーバーが起動する。
     */
    public static void main(String[] args) {
        SpringApplication.run(MyGaeElApplication.class, args);
    }
}
