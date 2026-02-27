package com.example.mygaeel.repository;

import com.example.mygaeel.entity.ElTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * ■ 監視対象テーブルの DB 操作インターフェース
 *
 * JpaRepository を継承することで基本的な CRUD 操作は自動で使えます。
 * ここでは1つカスタムメソッドを追加しています。
 *
 * JpaRepository<ElTargetEntity, String> の意味：
 *   - ElTargetEntity : 操作対象のエンティティ
 *   - String         : 主キー（"regionId#targetId" 形式）の型
 */
public interface ElTargetRepository extends JpaRepository<ElTargetEntity, String> {

    /**
     * targetId に一致するレコードを1件検索します。
     *
     * 【Spring Data JPA のメソッド名自動解析】
     * "findBy" + フィールド名 と書くだけで、Spring が自動的に SQL を生成します。
     *
     * findByTargetId(targetId) →
     *   SELECT * FROM el_targets WHERE target_id = ? LIMIT 1 に相当するSQL
     *
     * Optional<ElTargetEntity> とは：
     *   結果が「あるかもしれないし、ないかもしれない」場合に使う型です。
     *   - 見つかった場合 → Optional.of(entity) : 値が入っている状態
     *   - 見つからない場合 → Optional.empty()  : 空の状態
     *   NullPointerException（NPE）を防ぐための Java の便利な仕組みです。
     */
    Optional<ElTargetEntity> findByTargetId(String targetId);
}
