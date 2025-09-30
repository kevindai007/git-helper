package com.kevindai.git.helper.mr.prompt.strategy;

import com.kevindai.git.helper.mr.prompt.JavaBestPractice;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JavaStrategy implements PromptStrategy {
    private static final Set<String> EXTS = Set.of("java");
    private final ScoreCalculator scoreCalculator;

    public JavaStrategy(ScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    @Override
    public String id() { return "java"; }

    @Override
    public double score(MrContext context) {
        return scoreCalculator.scoreForExtensions(context, EXTS);
    }

    @Override
    public String systemPrompt(MrContext context) {
        return JavaBestPractice.SYSTEM_PROMPT;
    }
}
