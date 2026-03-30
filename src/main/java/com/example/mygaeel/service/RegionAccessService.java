package com.example.mygaeel.service;

import com.example.mygaeel.model.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * ログイン中のユーザーが指定リージョンにアクセスできるか判定するサービス。
 *
 * アクセス制御のルール:
 * - ADMIN: すべてのリージョンにアクセス可
 * - INSPECTOR: allowedRegions に含まれるリージョンのみアクセス可
 */
@Service
public class RegionAccessService {

    /**
     * 現在ログイン中のユーザーが指定リージョンにアクセスできるか判定する。
     *
     * @param regionId アクセスしようとしているリージョンID
     * @return アクセス可能なら true
     */
    public boolean canAccess(String regionId) {
        CustomUserDetails user = currentUser();
        if (user == null) return false;           // 未ログインはアクセス不可
        if (user.isAdmin()) return true;           // ADMIN はすべてのリージョンにアクセス可
        return user.getAllowedRegions().contains(regionId); // INSPECTOR は許可リージョンのみ
    }

    /**
     * Spring Security の SecurityContext から現在ログイン中のユーザーを取得する。
     * ログインしていない場合や認証情報が CustomUserDetails でない場合は null を返す。
     *
     * @return CustomUserDetails または null
     */
    public CustomUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            return null;
        }
        return user;
    }
}
