package com.example.mygaeel.repository;

import com.example.mygaeel.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    // findById(email) で取得可能。追加のクエリは不要。
}
