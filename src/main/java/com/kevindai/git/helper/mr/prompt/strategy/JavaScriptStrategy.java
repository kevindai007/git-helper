package com.kevindai.git.helper.mr.prompt.strategy;

import com.kevindai.git.helper.mr.prompt.PromptType;
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
    public PromptType type() {
        return PromptType.JAVASCRIPT;
    }

    @Override
    public double score(MrContext context) {
        return scoreCalculator.scoreForExtensions(context, EXTS);
    }

}
