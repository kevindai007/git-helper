package com.kevindai.git.helper.mr.prompt.strategy;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

final class StrategyUtils {
    private StrategyUtils() {}

    static String extensionOf(String path) {
        if (path == null) return "";
        String p = path.toLowerCase(Locale.ROOT);
        int lastSlash = p.lastIndexOf('/');
        String name = lastSlash >= 0 ? p.substring(lastSlash + 1) : p;
        int idx = name.lastIndexOf('.');
        if (idx < 0) return "";
        return name.substring(idx + 1);
    }

    static LineCounts countDiffLines(String diff) {
        int added = 0, removed = 0;
        if (diff == null || diff.isEmpty()) return new LineCounts(0, 0);
        String[] lines = diff.split("\n");
        for (String line : lines) {
            if (line.isEmpty()) continue;
            // Skip diff metadata markers
            if (line.startsWith("+++") || line.startsWith("---") || line.startsWith("@@")) continue;
            char c = line.charAt(0);
            if (c == '+') added++;
            else if (c == '-') removed++;
        }
        return new LineCounts(added, removed);
    }

    static double scoreForExtensions(MrContext ctx, Set<String> exts) {
        double score = 0.0;
        for (String ext : exts) {
            String e = Objects.requireNonNullElse(ext, "").toLowerCase(Locale.ROOT);
            int fileCount = ctx.fileCountByExt().getOrDefault(e, 0);
            int added = ctx.addedLinesByExt().getOrDefault(e, 0);
            int removed = ctx.removedLinesByExt().getOrDefault(e, 0);
            // Simple baseline: file count weighted more than line count
            score += fileCount * 20.0 + (added + removed) * 1.0;
        }
        return score;
    }

    static final class LineCounts {
        final int added;
        final int removed;

        LineCounts(int added, int removed) {
            this.added = added;
            this.removed = removed;
        }
    }
}

