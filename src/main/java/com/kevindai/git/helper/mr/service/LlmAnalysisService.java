package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import com.kevindai.git.helper.mr.prompt.GeneralBestPractice;
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


    public String analyzeDiff(String formattedDiffContent, List<MrDiff> diffs) {
        String prompt = selectPromptForFiles(diffs);
//        return chatClient
//                .prompt(GeneralBestPractice.SYSTEM_PROMPT)
//                .user(formattedDiffContent)
//                .call()
//                .chatResponse()
//                .getResults()
//                .getFirst()
//                .getOutput()
//                .getText();
        return "";
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
                log.debug("Prompt strategy '{}' score: {}", s.id(), sc);
            } catch (Exception e) {
                log.warn("Scoring error in strategy {}: {}", s.id(), e.getMessage());
            }
        });

        PromptStrategy best = strategies.stream()
                .max(Comparator.comparingDouble(s -> s.score(ctx)))
                .orElse(null);

        if (best == null) {
            return GeneralBestPractice.SYSTEM_PROMPT;
        }
        String chosen = best.systemPrompt(ctx);
        log.info("Selected prompt strategy: {}", best.id());
        return chosen;
    }
}
