package com.kevindai.git.helper.repository;

import com.kevindai.git.helper.entity.MrAnalysisDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MrAnalysisDetailRepository extends JpaRepository<MrAnalysisDetailEntity, Long> {
    void deleteByMrInfoId(Long mrInfoId);
    java.util.List<MrAnalysisDetailEntity> findByMrInfoId(Long mrInfoId);
}
