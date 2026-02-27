package com.example.mygaeel.model;

import java.util.List;

public class User {
    private final String email;
    private final String passwordHash;
    private final String role;                // "ADMIN" or "INSPECTOR"
    private final List<String> allowedRegions; // 閲覧可能なリージョンID一覧

    public User(String email, String passwordHash, String role, List<String> allowedRegions) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : "INSPECTOR";
        this.allowedRegions = allowedRegions != null ? allowedRegions : List.of();
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public List<String> getAllowedRegions() { return allowedRegions; }
}
