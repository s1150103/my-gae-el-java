package com.example.mygaeel.service;

import com.example.mygaeel.model.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * ■ リージョンアクセス権限チェックサービスクラス
 *
 * 「現在ログインしているユーザーが、指定のリージョンにアクセスできるか？」を判定します。
 *
 * 【権限ルール】
 *   - ADMIN    : すべてのリージョンにアクセス可能
 *   - INSPECTOR: allowedRegions に含まれるリージョンのみアクセス可能
 *
 * 【SecurityContextHolder とは？】
 * Spring Security が管理する「現在のリクエストのログインユーザー情報の保管庫」です。
 * HTTP リクエストごとにスレッドローカルで管理されるため、
 * どのクラスからでも現在ログイン中のユーザーを取得できます。
 *
 * 使用場所：
 *   SensorController の mode=s, mode=j でリージョンのデータを返す前にチェック
 */
@Service
public class RegionAccessService {

    /**
     * 現在ログインしているユーザーが、指定リージョンにアクセスできるかを判定します。
     *
     * @param regionId チェック対象のリージョンID
     * @return アクセス可能なら true、不可なら false
     */
    public boolean canAccess(String regionId) {
        CustomUserDetails user = currentUser();
        if (user == null) return false;             // 未ログインはアクセス不可
        if (user.isAdmin()) return true;            // ADMIN は全リージョンOK
        return user.getAllowedRegions().contains(regionId); // INSPECTOR はリストに含まれればOK
    }

    /**
     * 現在ログイン中のユーザー情報を取得します。
     *
     * SecurityContextHolder → Authentication → Principal（主体）の順に取得します。
     * Principal が CustomUserDetails のインスタンスでなければ null を返します。
     *
     * 【instanceof パターンマッチング（Java 16+）】
     * 従来：if (auth.getPrincipal() instanceof CustomUserDetails) { CustomUserDetails user = (CustomUserDetails) auth.getPrincipal(); ... }
     * 新記法：if (auth.getPrincipal() instanceof CustomUserDetails user) { ... }  ← キャストと変数宣言が1行でできる
     *
     * @return ログイン中ユーザーの情報。未ログインまたは取得失敗の場合は null
     */
    public CustomUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            return null;
        }
        return user;
    }
}
