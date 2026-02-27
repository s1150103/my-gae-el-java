package com.example.mygaeel.service;

import com.example.mygaeel.entity.ElTargetEntity;
import com.example.mygaeel.repository.ElTargetRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ElTargetService {

    private final ElTargetRepository repository;

    public ElTargetService(ElTargetRepository repository) {
        this.repository = repository;
    }

    public void save(ElTargetEntity entity) {
        repository.save(entity);
        System.out.println("ElTarget 登録: regionId=" + entity.getRegionId()
                + ", targetId=" + entity.getTargetId());
    }

    public Optional<ElTargetEntity> getTargetBySysId(String sysId) {
        return repository.findByTargetId(sysId);
    }

    public Optional<ElTargetEntity> getTarget(String targetId) {
        return repository.findByTargetId(targetId);
    }

    public String getRegionIdByTarget(String targetId) {
        return getTarget(targetId).map(ElTargetEntity::getRegionId).orElse(null);
    }

    public Optional<ElTargetEntity> getTargetByKey(String regionId, String targetId) {
        return repository.findById(regionId + "#" + targetId);
    }
}
