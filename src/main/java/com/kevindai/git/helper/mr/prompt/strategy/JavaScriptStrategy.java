package com.kevindai.git.helper.mr.prompt.strategy;

import com.kevindai.git.helper.mr.prompt.JavaScriptBestPractice;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JavaScriptStrategy implements PromptStrategy {
    private static final Set<String> EXTS = Set.of("js", "mjs", "cjs", "jsx", "ts", "tsx");
    private final ScoreCalculator scoreCalculator;

    public JavaScriptStrategy(ScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    @Override
    public String id() { return "javascript"; }

    @Override
    public double score(MrContext context) {
        return scoreCalculator.scoreForExtensions(context, EXTS);
    }

    @Override
    public String systemPrompt(MrContext context) {
        return JavaScriptBestPractice.SYSTEM_PROMPT;
    }
}
