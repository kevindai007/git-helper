package com.kevindai.git.helper.mr.prompt.strategy;

import com.kevindai.git.helper.mr.prompt.GeneralBestPractice;
import org.springframework.stereotype.Component;

@Component
public class GenericStrategy implements PromptStrategy {
    @Override
    public String id() { return "generic"; }

    @Override
    public double score(MrContext context) {
        // Very small baseline to act as fallback when nothing else matches.
        return context.totalFiles() > 0 ? 0.01 : 0.0;
    }

    @Override
    public String systemPrompt(MrContext context) {
        return GeneralBestPractice.SYSTEM_PROMPT;
    }
}

