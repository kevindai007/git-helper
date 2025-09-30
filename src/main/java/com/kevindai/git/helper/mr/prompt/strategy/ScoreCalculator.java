package com.kevindai.git.helper.mr.prompt.strategy;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
public class ScoreCalculator {
    private final PromptSelectionProperties props;

    public ScoreCalculator(PromptSelectionProperties props) {
        this.props = props;
    }

    public double scoreForExtensions(MrContext ctx, Set<String> exts) {
        double score = 0.0;
        for (String ext : exts) {
            String e = Objects.requireNonNullElse(ext, "").toLowerCase(Locale.ROOT);
            int fileCount = ctx.fileCountByExt().getOrDefault(e, 0);
            int added = ctx.addedLinesByExt().getOrDefault(e, 0);
            int removed = ctx.removedLinesByExt().getOrDefault(e, 0);
            score += fileCount * props.getFileCountWeight() + (added + removed) * props.getLineWeight();
        }
        return score;
    }
}

