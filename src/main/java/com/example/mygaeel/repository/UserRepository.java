package com.example.mygaeel.repository;

import com.example.mygaeel.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ■ ユーザーテーブルの DB 操作インターフェース（Repository）
 *
 * 【Repository パターンとは？】
 * DB へのアクセス処理をまとめた「窓口」です。
 * Service クラスはこの Repository を通じて DB を操作します。
 * これにより、Service クラスが SQL の書き方を知らなくても
 * メソッドを呼ぶだけで DB 操作ができます。
 *
 * 【JpaRepository とは？】
 * Spring Data JPA が提供する便利なベースインターフェースです。
 * extends するだけで以下のメソッドが自動で使えるようになります：
 *   - findById(id)   : IDで1件検索
 *   - findAll()      : 全件取得
 *   - save(entity)   : 保存（新規作成 or 更新）
 *   - deleteById(id) : IDで削除
 *   - existsById(id) : IDが存在するか確認
 *   - count()        : 件数を取得
 *   など
 *
 * JpaRepository<UserEntity, String> の意味：
 *   - UserEntity : 操作対象のエンティティクラス
 *   - String     : 主キー（email）の型
 *
 * このシステムでは標準のメソッドだけで十分なため、追加のメソッドは定義していません。
 */
public interface UserRepository extends JpaRepository<UserEntity, String> {
    // findById(email) で取得可能。追加のカスタムクエリは不要。
}
