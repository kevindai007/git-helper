package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.entity.MrAnalysisDetailEntity;
import com.kevindai.git.helper.entity.MrInfoEntity;
import com.kevindai.git.helper.mr.dto.gitlab.MrVersion;
import com.kevindai.git.helper.repository.MrAnalysisDetailRepository;
import com.kevindai.git.helper.repository.MrInfoEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MrAdoptService {

    private final MrAnalysisDetailRepository detailRepository;
    private final MrInfoEntityRepository mrInfoRepository;
    private final GitLabService gitLabService;

    public void adoptRecommendation(long detailId) {
        MrAnalysisDetailEntity detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("Detail not found: " + detailId));

        MrInfoEntity mrInfo = mrInfoRepository.findById(detail.getMrInfoId())
                .orElseThrow(() -> new IllegalStateException("MR info not found for id: " + detail.getMrInfoId()));

        long projectId = detail.getProjectId();
        int mrId = Math.toIntExact(detail.getMrId());

        List<MrVersion> versions = gitLabService.fetchMrVersions(projectId, mrId);
        if (versions == null || versions.isEmpty()) {
            throw new IllegalStateException("No versions found for MR " + mrId);
        }

        // Pick the latest by created_at
        MrVersion latest = versions.stream().max(Comparator.comparing(v -> parseTimeSafe(v.getCreated_at()))).orElse(versions.getFirst());

        if (!safeEq(latest.getHead_commit_sha(), mrInfo.getSha())) {
            throw new IllegalStateException("MR head SHA mismatch. Expected=" + mrInfo.getSha() + ", latest=" + latest.getHead_commit_sha());
        }

        gitLabService.createMrDiscussion(
                projectId,
                mrId,
                latest.getBase_commit_sha(),
                latest.getHead_commit_sha(),
                latest.getStart_commit_sha(),
                detail.getFile(),
                detail.getStartLine(),
                detail.getRemediationSteps()
        );
    }

    private static Instant parseTimeSafe(String iso) {
        if (iso == null) return Instant.EPOCH;
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (DateTimeParseException e) {
            return Instant.EPOCH;
        }
    }

    private static boolean safeEq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}

