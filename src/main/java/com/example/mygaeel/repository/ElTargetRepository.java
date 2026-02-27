package com.example.mygaeel.repository;

import com.example.mygaeel.entity.ElTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ElTargetRepository extends JpaRepository<ElTargetEntity, String> {

    Optional<ElTargetEntity> findByTargetId(String targetId);
}
