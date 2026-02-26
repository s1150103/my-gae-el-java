package com.example.mygaeel.model;

public class User {
    private final String email;
    private final String passwordHash;

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
