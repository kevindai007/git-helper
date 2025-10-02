package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.entity.MrInfoEntity;
import com.kevindai.git.helper.mr.dto.AnalysisStatus;
import com.kevindai.git.helper.mr.dto.MrAnalyzeRequest;
import com.kevindai.git.helper.mr.dto.MrAnalyzeResponse;
import com.kevindai.git.helper.mr.dto.gitlab.MrDetail;
import com.kevindai.git.helper.mr.dto.llm.LlmAnalysisReport;
import com.kevindai.git.helper.repository.MrInfoEntityRepository;
import com.kevindai.git.helper.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class MrAnalyzeService {
    private final GitLabService gitLabService;
    private final LlmAnalysisService llmAnalysisService;
    private final MrInfoEntityRepository mrInfoEntityRepository;

    public MrAnalyzeResponse analyzeMr(MrAnalyzeRequest req) {
        var parsedUrl = gitLabService.parseMrUrl(req.getMrUrl());
        long groupId = gitLabService.fetchGroupId(parsedUrl);
        long projectId = gitLabService.fetchProjectId(groupId, parsedUrl.getProjectPath());
        MrDetail mrDetail = gitLabService.fetchMrDetails(projectId, parsedUrl.getMrId());
        if (mrDetail == null) {
            throw new IllegalArgumentException("Cannot find MR details for MR ID: " + parsedUrl.getMrId());
        }
        var existingOpt = mrInfoEntityRepository.findByProjectIdAndMrId(projectId, (long) parsedUrl.getMrId());
        if (existingOpt.isPresent()) {
            MrInfoEntity existing = existingOpt.get();
            if (existing.getSha() != null && existing.getSha().equals(mrDetail.getSha()) && StringUtils.hasText(existing.getAnalysisResult())) {
                log.info("MR unchanged, skip analysis. projectId={}, mrId={}, sha={}", projectId, parsedUrl.getMrId(), mrDetail.getSha());
                return MrAnalyzeResponse.builder()
                        .status(AnalysisStatus.SUCCESS)
                        .mrUrl(req.getMrUrl())
                        .analysisResult(JsonUtils.parseObject(existing.getAnalysisResult(), LlmAnalysisReport.class))
                        .build();
            }
            // sha has changed -> update
            MrInfoEntity updated = converter(mrDetail);
            updated.setId(existing.getId());
            updated.setCreatedAt(existing.getCreatedAt());
            mrInfoEntityRepository.save(updated);
            log.info("MR updated due to sha change. projectId={}, mrId={}, oldSha={}, newSha={}",
                    projectId, parsedUrl.getMrId(), existing.getSha(), mrDetail.getSha());
        } else {
            // not exist -> insert
            MrInfoEntity newEntity = converter(mrDetail);
            mrInfoEntityRepository.save(newEntity);
            log.info("MR inserted. projectId={}, mrId={}, sha={}", projectId, parsedUrl.getMrId(), mrDetail.getSha());
        }

        var diffs = gitLabService.fetchMrDiffs(projectId, parsedUrl.getMrId());
        String formatted = gitLabService.formatDiffs(diffs);
        LlmAnalysisReport analysis = llmAnalysisService.analyzeDiff(formatted, diffs);
        mrInfoEntityRepository.findByProjectIdAndMrId(projectId, (long) parsedUrl.getMrId()).ifPresent(entity -> {
            entity.setAnalysisResult(JsonUtils.toJSONString(analysis));
            entity.setUpdatedAt(Instant.now());
            mrInfoEntityRepository.save(entity);
        });

        return MrAnalyzeResponse.builder()
                .status(AnalysisStatus.SUCCESS)
                .mrUrl(req.getMrUrl())
                .analysisResult(analysis)
                .build();
    }

    private MrInfoEntity converter(MrDetail mrDetail) {
        if (mrDetail == null) {
            return null;
        }
        MrInfoEntity entity = new MrInfoEntity();
        entity.setProjectId(mrDetail.getProject_id());
        entity.setMrId(mrDetail.getIid());
        entity.setMrTitle(mrDetail.getTitle());
        entity.setState(mrDetail.getState());
        entity.setTargetBranch(mrDetail.getTarget_branch());
        entity.setSourceBranch(mrDetail.getSource_branch());
        entity.setSha(mrDetail.getSha());
        entity.setWebUrl(mrDetail.getWeb_url());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;

    }
}
