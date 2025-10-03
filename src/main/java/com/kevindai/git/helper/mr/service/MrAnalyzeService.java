package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.entity.MrAnalysisDetailEntity;
import com.kevindai.git.helper.entity.MrInfoEntity;
import com.kevindai.git.helper.mr.dto.AnalysisStatus;
import com.kevindai.git.helper.mr.dto.MrAnalyzeRequest;
import com.kevindai.git.helper.mr.dto.MrAnalyzeResponse;
import com.kevindai.git.helper.mr.dto.gitlab.MrDetail;
import com.kevindai.git.helper.mr.dto.llm.Finding;
import com.kevindai.git.helper.mr.dto.llm.LlmAnalysisReport;
import com.kevindai.git.helper.repository.MrAnalysisDetailRepository;
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
    private final MrAnalysisDetailRepository analysisDetailRepository;

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
            if (existing.getSha() != null && existing.getSha().equals(mrDetail.getSha())) {
                var details = analysisDetailRepository.findByMrInfoId(existing.getId());
                if (details != null && !details.isEmpty()) {
                    log.info("MR unchanged with existing details, skip LLM. projectId={}, mrId={}, sha={}", projectId, parsedUrl.getMrId(), mrDetail.getSha());
                    LlmAnalysisReport report = buildReportFromDetails(details);
                    return MrAnalyzeResponse.builder()
                            .status(AnalysisStatus.SUCCESS)
                            .mrUrl(req.getMrUrl())
                            .analysisResult(report)
                            .build();
                }
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
        mrInfoEntityRepository.findByProjectIdAndMrIdAndSha(projectId, (long) parsedUrl.getMrId(), mrDetail.getSha()).ifPresent(entity -> {
            entity.setAnalysisResult(JsonUtils.toJSONString(analysis));
            entity.setUpdatedAt(Instant.now());
            mrInfoEntityRepository.save(entity);
            // Persist structured findings to detail table
            llmAnalysisService.persistAnalysisDetails(entity, analysis);
        });

        return MrAnalyzeResponse.builder()
                .status(AnalysisStatus.SUCCESS)
                .mrUrl(req.getMrUrl())
                .analysisResult(analysis)
                .build();
    }

    private LlmAnalysisReport buildReportFromDetails(java.util.List<MrAnalysisDetailEntity> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        var report = new LlmAnalysisReport();
        report.setSchemaVersion("1.0");
        // promptType may be unknown here; leave null
        var findings = new java.util.ArrayList<Finding>();
        for (MrAnalysisDetailEntity d : details) {
            var f = new Finding();
            f.setSeverity(d.getSeverity());
            f.setCategory(d.getCategory());
            f.setTitle(d.getTitle());
            f.setDescription(d.getDescription());
            if (d.getFile() != null) {
                var loc = new com.kevindai.git.helper.mr.dto.llm.Location();
                loc.setFile(d.getFile());
                loc.setStartLine(d.getStartLine());
                loc.setEndLine(d.getEndLine());
                loc.setStartCol(d.getStartCol());
                loc.setEndCol(d.getEndCol());
                f.setLocation(loc);
            }
            f.setEvidence(d.getEvidence());
            if (StringUtils.hasText(d.getRemediationSteps()) || StringUtils.hasText(d.getRemediationDiff())) {
                var rem = new com.kevindai.git.helper.mr.dto.llm.Remediation();
                rem.setSteps(d.getRemediationSteps());
                rem.setDiff(d.getRemediationDiff());
                f.setRemediation(rem);
            }
            f.setConfidence(d.getConfidence());
            if (StringUtils.hasText(d.getTagsJson())) {
                try {
                    var tags = com.kevindai.git.helper.utils.JsonUtils.parseArray(d.getTagsJson(), String.class);
                    f.setTags(tags);
                } catch (Exception ignored) {
                }
            }
            findings.add(f);
        }
        report.setFindings(findings);
        return report;
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
