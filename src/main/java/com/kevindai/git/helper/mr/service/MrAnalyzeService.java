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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Comparator;
import com.kevindai.git.helper.mr.model.Severity;

@Slf4j
@Service
@RequiredArgsConstructor
public class MrAnalyzeService {
    private final GitLabService gitLabService;
    private final LlmAnalysisService llmAnalysisService;
    private final MrInfoEntityRepository mrInfoEntityRepository;
    private final MrAnalysisDetailRepository analysisDetailRepository;
    private final AddressableDiffBuilder addressableDiffBuilder;
    private final MrAnalysisDetailService mrAnalysisDetailService;
    private final GitTokenService gitTokenService;

    @Transactional
    public MrAnalyzeResponse analyzeMr(MrAnalyzeRequest req) {
        var parsedUrl = gitLabService.parseMrUrl(req.getMrUrl());
        // Resolve token by group full path (ParsedMrUrl.projectFullPath holds the group hierarchy)
        String groupFullPath = parsedUrl.getProjectFullPath();
        String token = gitTokenService.resolveTokenForGroup(groupFullPath);
        long groupId = gitLabService.fetchGroupId(parsedUrl, token);
        long projectId = gitLabService.fetchProjectId(groupId, parsedUrl.getProjectPath(), token);
        MrDetail mrDetail = gitLabService.fetchMrDetails(projectId, parsedUrl.getMrId(), token);
        if (mrDetail == null) {
            throw new IllegalArgumentException("Cannot find MR details for MR ID: " + parsedUrl.getMrId());
        }

        var existingMrInfo = mrInfoEntityRepository.findByProjectIdAndMrIdAndSha(projectId, (long) parsedUrl.getMrId(), mrDetail.getSha());
        MrInfoEntity targetInfo;
        if (existingMrInfo.isPresent()) {
            targetInfo = existingMrInfo.get();
            var details = mrAnalysisDetailService.loadDetails(targetInfo.getId());
            if (details != null && !details.isEmpty()) {
                log.info("MR unchanged with existing details, skip LLM. projectId={}, mrId={}, sha={}", projectId, parsedUrl.getMrId(), mrDetail.getSha());
                LlmAnalysisReport report = buildReportFromDetails(targetInfo, details);
                return MrAnalyzeResponse.builder()
                        .status(AnalysisStatus.SUCCESS)
                        .mrUrl(req.getMrUrl())
                        .analysisResult(report)
                        .build();
            }
        } else {
            // Create a new mr_info row for this sha (keep history by sha)
            targetInfo = converter(mrDetail);
            targetInfo = mrInfoEntityRepository.save(targetInfo);
            log.info("MR info created for new sha. projectId={}, mrId={}, sha={}", projectId, parsedUrl.getMrId(), mrDetail.getSha());
        }

        var diffs = gitLabService.fetchMrDiffs(projectId, parsedUrl.getMrId(), token);
        var annotated = addressableDiffBuilder.buildAnnotatedWithIndex(diffs);
        // Stage 1: analyze per-file to stay within token limits
        for (var d : diffs) {
            String path = StringUtils.hasText(d.getNew_path()) ? d.getNew_path() : d.getOld_path();
            if (!StringUtils.hasText(path)) continue;
            String fileSection = AddressableDiffBuilder.sliceAnnotatedForPath(annotated.getContent(), path);
            if (!StringUtils.hasText(fileSection)) continue;
            // Single-file prompt selection by passing only this diff
            LlmAnalysisReport piece = llmAnalysisService.analyzeDiff(fileSection, java.util.List.of(d));
            mrAnalysisDetailService.persist(targetInfo, piece, annotated.getIndex());
        }

        // Build final report from persisted details (ensures IDs correct) and save summary
        var savedDetails = mrAnalysisDetailService.loadDetails(targetInfo.getId());
        LlmAnalysisReport responseReport = buildReportFromDetails(targetInfo, savedDetails);
        targetInfo.setUpdatedAt(Instant.now());
        targetInfo.setSummaryMarkdown(responseReport.getSummaryMarkdown());
        mrInfoEntityRepository.save(targetInfo);
        return MrAnalyzeResponse.builder()
                .status(AnalysisStatus.SUCCESS)
                .mrUrl(req.getMrUrl())
                .analysisResult(responseReport)
                .build();
    }

    public LlmAnalysisReport buildNoIssuesReport() {
        var report = new LlmAnalysisReport();
        report.setSchemaVersion("1.0");
        report.setFindings(List.of());
        report.setSummaryMarkdown("No issue found.");
        return report;
    }

    private LlmAnalysisReport buildReportFromDetails(MrInfoEntity mrInfo, List<MrAnalysisDetailEntity> details) {
        if (details == null || details.isEmpty()) {
            return buildNoIssuesReport();
        }
        var report = new LlmAnalysisReport();
        report.setSchemaVersion("1.0");
        // promptType may be unknown here; leave null
        var findings = new java.util.ArrayList<Finding>();
        for (MrAnalysisDetailEntity d : details) {
            var f = new Finding();
            if (d.getId() != null) {
                f.setId(String.valueOf(d.getId()));
            }
            f.setSeverity(d.getSeverity());
            f.setCategory(d.getCategory());
            f.setTitle(d.getTitle());
            f.setDescription(d.getDescription());
            f.setStatus(d.getStatus());
            if (d.getFile() != null) {
                var loc = new com.kevindai.git.helper.mr.dto.llm.Location();
                loc.setFile(d.getFile());
                loc.setStartLine(d.getStartLine());
                loc.setLineType(d.getLineType());
                loc.setAnchorId(d.getAnchorId());
                loc.setAnchorSide(d.getAnchorSide());
                f.setLocation(loc);
            }
            f.setEvidence(d.getEvidence());
            if (StringUtils.hasText(d.getRemediationSteps())) {
                var rem = new com.kevindai.git.helper.mr.dto.llm.Remediation();
                rem.setSteps(d.getRemediationSteps());
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
        report.setSummaryMarkdown(mrInfo.getSummaryMarkdown());
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
