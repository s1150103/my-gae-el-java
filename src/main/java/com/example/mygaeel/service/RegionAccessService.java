package com.example.mygaeel.service;

import com.example.mygaeel.model.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * ログイン中のユーザーが指定リージョンにアクセスできるか判定するサービス。
 * - ADMIN: 全リージョンにアクセス可
 * - INSPECTOR: allowedRegions に含まれるリージョンのみアクセス可
 */
@Service
public class RegionAccessService {

    public boolean canAccess(String regionId) {
        CustomUserDetails user = currentUser();
        if (user == null) return false;
        if (user.isAdmin()) return true;
        return user.getAllowedRegions().contains(regionId);
    }

    public CustomUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            return null;
        }
        return user;
    }
}
