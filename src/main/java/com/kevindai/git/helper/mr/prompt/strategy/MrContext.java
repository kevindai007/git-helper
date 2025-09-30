package com.kevindai.git.helper.mr.prompt.strategy;

import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Minimal MR analysis context derived from diffs.
 */
public class MrContext {
    private final List<MrDiff> diffs;

    // Precomputed stats by file extension (lowercase, without dot)
    private final Map<String, Integer> fileCountByExt;
    private final Map<String, Integer> addedLinesByExt;
    private final Map<String, Integer> removedLinesByExt;

    public MrContext(List<MrDiff> diffs) {
        this.diffs = diffs == null ? List.of() : List.copyOf(diffs);
        this.fileCountByExt = new HashMap<>();
        this.addedLinesByExt = new HashMap<>();
        this.removedLinesByExt = new HashMap<>();
        precompute();
    }

    private void precompute() {
        for (MrDiff d : diffs) {
            String path = d.getNew_path() != null ? d.getNew_path() : d.getOld_path();
            String ext = StrategyUtils.extensionOf(path);

            fileCountByExt.merge(ext, 1, Integer::sum);

            var counts = StrategyUtils.countDiffLines(d.getDiff());
            addedLinesByExt.merge(ext, counts.added, Integer::sum);
            removedLinesByExt.merge(ext, counts.removed, Integer::sum);
        }
    }

    public List<MrDiff> diffs() {
        return diffs;
    }

    public Map<String, Integer> fileCountByExt() {
        return Collections.unmodifiableMap(fileCountByExt);
    }

    public Map<String, Integer> addedLinesByExt() {
        return Collections.unmodifiableMap(addedLinesByExt);
    }

    public Map<String, Integer> removedLinesByExt() {
        return Collections.unmodifiableMap(removedLinesByExt);
    }

    public int totalFiles() {
        return diffs.size();
    }

    public int totalAddedLines() {
        return addedLinesByExt.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int totalRemovedLines() {
        return removedLinesByExt.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Set<String> extensions() {
        return fileCountByExt.keySet();
    }

    @Override
    public String toString() {
        return "MrContext{" +
                "exts=" + extensions().stream().sorted().collect(Collectors.joining(",")) +
                ", files=" + totalFiles() +
                ", added=" + totalAddedLines() +
                ", removed=" + totalRemovedLines() +
                '}';
    }
}

