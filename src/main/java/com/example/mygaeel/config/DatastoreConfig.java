package com.example.mygaeel.config;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Cloud Datastore の接続設定クラス。
 * Datastore は GCP が提供する NoSQL データベースサービス。
 */
@Configuration
public class DatastoreConfig {

    /**
     * Datastore クライアントを Spring の Bean として登録する。
     * getDefaultInstance() を使うと、環境変数 GOOGLE_CLOUD_PROJECT や
     * GCPのサービスアカウント認証情報を自動的に読み取って接続する。
     *
     * @return Datastore クライアントのインスタンス
     */
    @Bean
    public Datastore datastore() {
        return DatastoreOptions.getDefaultInstance().getService();
    }
}
