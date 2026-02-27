package com.example.mygaeel.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false)
    private String role;  // "ADMIN" or "INSPECTOR"

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_allowed_regions",
                     joinColumns = @JoinColumn(name = "email"))
    @Column(name = "region_id")
    private List<String> allowedRegions = new ArrayList<>();

    protected UserEntity() {}

    public UserEntity(String email, String passwordHash, String role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : "INSPECTOR";
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public List<String> getAllowedRegions() { return allowedRegions; }

    public void setRole(String role) { this.role = role; }
    public void setAllowedRegions(List<String> allowedRegions) {
        this.allowedRegions = allowedRegions != null ? allowedRegions : new ArrayList<>();
    }
}
