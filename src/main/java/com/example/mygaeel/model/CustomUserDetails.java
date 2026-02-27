package com.example.mygaeel.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final String email;
    private final String password;
    private final String role;
    private final List<String> allowedRegions;

    public CustomUserDetails(String email, String password, String role, List<String> allowedRegions) {
        this.email = email;
        this.password = password;
        this.role = role != null ? role : "INSPECTOR";
        this.allowedRegions = allowedRegions != null ? allowedRegions : List.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    public String getRole() { return role; }
    public List<String> getAllowedRegions() { return allowedRegions; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
