package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.entity.MrAnalysisDetailEntity;
import com.kevindai.git.helper.entity.MrInfoEntity;
import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import com.kevindai.git.helper.mr.dto.llm.Finding;
import com.kevindai.git.helper.mr.dto.llm.LlmAnalysisReport;
import com.kevindai.git.helper.mr.prompt.GeneralBestPractice;
import com.kevindai.git.helper.mr.prompt.PromptProvider;
import com.kevindai.git.helper.mr.prompt.PromptType;
import com.kevindai.git.helper.mr.prompt.strategy.MrContext;
import com.kevindai.git.helper.mr.prompt.strategy.PromptStrategy;
import com.kevindai.git.helper.repository.MrAnalysisDetailRepository;
import com.kevindai.git.helper.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LlmAnalysisService {

    private final ChatClient chatClient;
    private final List<PromptStrategy> strategies;
    private final PromptProvider promptProvider;
    private final MrAnalysisDetailRepository analysisDetailRepository;

    public LlmAnalysisReport analyzeDiff(String formattedDiffContent, List<MrDiff> diffs) {
        String prompt = selectPromptForFiles(diffs);
        return chatClient
                .prompt(prompt)
                .user(formattedDiffContent)
                .call()
                .entity(LlmAnalysisReport.class);
    }

    public void persistAnalysisDetails(MrInfoEntity mrInfo, LlmAnalysisReport report) {
        if (mrInfo == null || report == null || report.getFindings() == null) {
            return;
        }
        try {
            analysisDetailRepository.deleteByMrInfoId(mrInfo.getId());
        } catch (Exception e) {
            log.warn("Failed clearing previous analysis details for mr_info_id={}", mrInfo.getId());
        }
        var now = java.time.Instant.now();
        for (Finding f : report.getFindings()) {
            MrAnalysisDetailEntity e = new MrAnalysisDetailEntity();
            e.setMrInfoId(mrInfo.getId());
            e.setProjectId(mrInfo.getProjectId());
            e.setMrId(mrInfo.getMrId());
            e.setSeverity(f.getSeverity());
            e.setCategory(f.getCategory());
            e.setTitle(f.getTitle());
            e.setDescription(f.getDescription());
            if (f.getLocation() != null) {
                e.setFile(f.getLocation().getFile());
                e.setStartLine(f.getLocation().getStartLine());
                e.setEndLine(f.getLocation().getEndLine());
                e.setStartCol(f.getLocation().getStartCol());
                e.setEndCol(f.getLocation().getEndCol());
                e.setLineType(f.getLocation().getLineType());
            }
            e.setEvidence(f.getEvidence());
            if (f.getRemediation() != null) {
                e.setRemediationSteps(f.getRemediation().getSteps());
                e.setRemediationDiff(f.getRemediation().getDiff());
            }
            e.setConfidence(f.getConfidence());
            if (f.getTags() != null) {
                e.setTagsJson(JsonUtils.toJSONString(f.getTags()));
            }
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            analysisDetailRepository.save(e);
        }
    }

    private String selectPromptForFiles(List<MrDiff> diffs) {
        if (diffs == null || diffs.isEmpty() || strategies == null || strategies.isEmpty()) {
            return GeneralBestPractice.SYSTEM_PROMPT;
        }

        MrContext ctx = new MrContext(diffs);

        // Log scores for visibility and tuning
        strategies.forEach(s -> {
            try {
                double sc = s.score(ctx);
                log.debug("Prompt type '{}' score: {}", s.type(), sc);
            } catch (Exception e) {
                log.warn("Scoring error in strategy {}: {}", s.getClass().getSimpleName(), e.getMessage());
            }
        });

        PromptStrategy best = strategies.stream()
                .max(Comparator.comparingDouble(s -> s.score(ctx)))
                .orElse(null);

        if (best == null) {
            return GeneralBestPractice.SYSTEM_PROMPT;
        }
        PromptType chosenType = best.type();
        String prompt = promptProvider.get(chosenType);
        log.info("Selected prompt type: {}", chosenType);
        return prompt;
    }
}
