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

    public LlmAnalysisReport analyzeDiff(String content, List<MrDiff> diffs) {
        String prompt = selectPromptForFiles(diffs);
        return chatClient
                .prompt(prompt)
                .user(content)
                .call()
                .entity(LlmAnalysisReport.class);
    }

    public void persistAnalysisDetails(MrInfoEntity mrInfo,
                                       LlmAnalysisReport report,
                                       java.util.Map<String, AddressableDiffBuilder.AnchorEntry> anchorIndex) {
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
                String anchorId = f.getLocation().getAnchorId();
                String anchorSide = f.getLocation().getAnchorSide();
                e.setAnchorId(anchorId);
                e.setAnchorSide(anchorSide);

                AddressableDiffBuilder.AnchorEntry ae = anchorId != null && anchorIndex != null ? anchorIndex.get(anchorId) : null;
                if (ae != null) {
                    if (ae.side == 'N') {
                        e.setFile(ae.newPath);
                        e.setLineType("new_line");
                        e.setStartLine(ae.newLine);
                        e.setAnchorSide("new");
                    } else {
                        e.setFile(ae.oldPath);
                        e.setLineType("old_line");
                        e.setStartLine(ae.oldLine);
                        e.setAnchorSide("old");
                    }
                } else {
                    e.setFile(f.getLocation().getFile());
                    e.setStartLine(f.getLocation().getStartLine());
                    e.setLineType(f.getLocation().getLineType());
                }
                // endLine/startCol/endCol removed from entity; keep only startLine/lineType
            }
            e.setEvidence(f.getEvidence());
            if (f.getRemediation() != null) {
                e.setRemediationSteps(f.getRemediation().getSteps());
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
