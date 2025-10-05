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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

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

    public MrAnalyzeResponse analyzeMr(MrAnalyzeRequest req) {
        var parsedUrl = gitLabService.parseMrUrl(req.getMrUrl());
        long groupId = gitLabService.fetchGroupId(parsedUrl);
        long projectId = gitLabService.fetchProjectId(groupId, parsedUrl.getProjectPath());
        MrDetail mrDetail = gitLabService.fetchMrDetails(projectId, parsedUrl.getMrId());
        if (mrDetail == null) {
            throw new IllegalArgumentException("Cannot find MR details for MR ID: " + parsedUrl.getMrId());
        }

        var existingMrInfo = mrInfoEntityRepository.findByProjectIdAndMrIdAndSha(projectId, (long) parsedUrl.getMrId(), mrDetail.getSha());
        MrInfoEntity targetInfo;
        if (existingMrInfo.isPresent()) {
            targetInfo = existingMrInfo.get();
            var details = analysisDetailRepository.findByMrInfoId(targetInfo.getId());
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

        var diffs = gitLabService.fetchMrDiffs(projectId, parsedUrl.getMrId());
        var annotated = addressableDiffBuilder.buildAnnotatedWithIndex(diffs);
        String instruction = "Use anchors to reference locations. Copy an existing anchor id exactly as shown (e.g., A#12). Do not invent or compute line numbers. Provide location.anchorId (A#num) and anchorSide (new|old|context).\n\n";
        String userContent = instruction + annotated.getContent();
        LlmAnalysisReport analysis = llmAnalysisService.analyzeDiff(userContent, diffs);
        targetInfo.setUpdatedAt(Instant.now());
        targetInfo.setSummaryMarkdown(analysis.getSummaryMarkdown());
        mrInfoEntityRepository.save(targetInfo);
        // Persist structured findings to detail table via dedicated service
        mrAnalysisDetailService.persist(targetInfo, analysis, annotated.getIndex());

        // Re-read details to build response (ensures ids are correct)
        var savedDetails = analysisDetailRepository.findByMrInfoId(targetInfo.getId());
        LlmAnalysisReport responseReport = buildReportFromDetails(targetInfo, savedDetails);
        return MrAnalyzeResponse.builder()
                .status(AnalysisStatus.SUCCESS)
                .mrUrl(req.getMrUrl())
                .analysisResult(responseReport)
                .build();
    }

    private LlmAnalysisReport buildReportFromDetails(MrInfoEntity mrInfo, List<MrAnalysisDetailEntity> details) {
        if (details == null || details.isEmpty()) {
            return null;
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
