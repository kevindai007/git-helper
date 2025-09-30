package com.kevindai.git.helper.mr.prompt.strategy;

import com.kevindai.git.helper.mr.prompt.PythonBestPractice;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PythonStrategy implements PromptStrategy {
    private static final Set<String> EXTS = Set.of("py");
    private final ScoreCalculator scoreCalculator;

    public PythonStrategy(ScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    @Override
    public String id() { return "python"; }

    @Override
    public double score(MrContext context) {
        return scoreCalculator.scoreForExtensions(context, EXTS);
    }

    @Override
    public String systemPrompt(MrContext context) {
        return PythonBestPractice.SYSTEM_PROMPT;
    }
}
