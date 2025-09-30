package com.kevindai.git.helper.mr.prompt.strategy;

import com.kevindai.git.helper.mr.prompt.PromptType;
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
    public PromptType type() {
        return PromptType.PYTHON;
    }

    @Override
    public double score(MrContext context) {
        return scoreCalculator.scoreForExtensions(context, EXTS);
    }

}
