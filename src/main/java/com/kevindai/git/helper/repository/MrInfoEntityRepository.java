package com.kevindai.git.helper.repository;

import com.kevindai.git.helper.entity.MrInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MrInfoEntityRepository extends JpaRepository<MrInfoEntity, Long> {
    boolean existsByProjectIdAndMrId(Long projectId, Long mrId);

    boolean existsByProjectIdAndMrIdAndSha(Long projectId, Long mrId, String sha);

    Optional<MrInfoEntity> findByProjectIdAndMrIdAndSha(Long projectId, Long mrId, String sha);

    Optional<MrInfoEntity> findByProjectIdAndMrId(Long projectId, Long mrId);
}