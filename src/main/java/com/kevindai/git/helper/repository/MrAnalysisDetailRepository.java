package com.kevindai.git.helper.repository;

import com.kevindai.git.helper.entity.MrAnalysisDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MrAnalysisDetailRepository extends JpaRepository<MrAnalysisDetailEntity, Long> {
    void deleteByMrInfoId(Long mrInfoId);
    List<MrAnalysisDetailEntity> findByMrInfoId(Long mrInfoId);

    @Query("select d from MrAnalysisDetailEntity d where d.mrInfoId = :mrInfoId order by " +
            "case lower(d.severity) when 'blocker' then 0 when 'high' then 1 when 'medium' then 2 when 'low' then 3 when 'info' then 4 else 98 end asc, d.id asc")
    List<MrAnalysisDetailEntity> findByMrInfoIdOrderBySeverity(@Param("mrInfoId") Long mrInfoId);
}
