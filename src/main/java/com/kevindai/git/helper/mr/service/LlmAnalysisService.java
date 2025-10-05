package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import com.kevindai.git.helper.mr.dto.llm.LlmAnalysisReport;
import com.kevindai.git.helper.mr.prompt.GeneralBestPractice;
import com.kevindai.git.helper.mr.prompt.PromptProvider;
import com.kevindai.git.helper.mr.prompt.PromptType;
import com.kevindai.git.helper.mr.prompt.strategy.MrContext;
import com.kevindai.git.helper.mr.prompt.strategy.PromptStrategy;
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

    public LlmAnalysisReport analyzeDiff(String content, List<MrDiff> diffs) {
        String prompt = selectPromptForFiles(diffs);
        return chatClient
                .prompt(prompt)
                .user(content)
                .call()
                .entity(LlmAnalysisReport.class);
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
