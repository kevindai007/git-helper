package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
public class AddressableDiffBuilder {

    public static class AnchorEntry {
        public String id;          // e.g., A#1
        public char side;          // 'N' for new/context, 'O' for old
        public String newPath;
        public String oldPath;
        public Integer newLine;    // present for ' ' and '+'
        public Integer oldLine;    // present for ' ' and '-'
    }

    public static class AnnotatedDiff {
        private final String content;
        private final Map<String, AnchorEntry> index;

        public AnnotatedDiff(String content, Map<String, AnchorEntry> index) {
            this.content = content;
            this.index = index;
        }

        public String getContent() { return content; }
        public Map<String, AnchorEntry> getIndex() { return index; }
    }

    public AnnotatedDiff buildAnnotatedWithIndex(List<MrDiff> diffs) {
        if (diffs == null || diffs.isEmpty()) return new AnnotatedDiff("(No diffs found)", Map.of());
        // Sort by path to make numbering deterministic per sha
        List<MrDiff> sorted = new ArrayList<>(diffs);
        sorted.sort(Comparator.comparing(d -> {
            String p = StringUtils.hasText(d.getNew_path()) ? d.getNew_path() : d.getOld_path();
            return p == null ? "" : p;
        }));

        StringBuilder sb = new StringBuilder();
        Map<String, AnchorEntry> index = new LinkedHashMap<>();
        int counter = 1;

        for (MrDiff d : sorted) {
            String newPath = StringUtils.hasText(d.getNew_path()) ? d.getNew_path() : d.getOld_path();
            sb.append("--- File: ").append(newPath).append(" ---\n");
            if (d.getDiff() == null) { sb.append("...\n"); continue; }
            counter = annotateOne(sb, d, index, counter);
            sb.append("...\n");
        }
        return new AnnotatedDiff(sb.toString(), index);
    }

    private int annotateOne(StringBuilder out, MrDiff d, Map<String, AnchorEntry> index, int start) {
        String diff = d.getDiff();
        String newPath = d.getNew_path();
        String oldPath = d.getOld_path();
        int newLine = 0;
        int oldLine = 0;
        boolean inHunk = false;
        String[] lines = diff.split("\n");
        int counter = start;
        for (String raw : lines) {
            if (raw.startsWith("@@")) {
                // parse hunk header
                int plusIdx = raw.indexOf('+');
                int atatIdx = raw.indexOf("@@", 2);
                if (plusIdx > 0 && atatIdx > plusIdx) {
                    String seg = raw.substring(plusIdx + 1, atatIdx).trim();
                    String[] parts = seg.split(","); // c or c,d
                    try { newLine = Integer.parseInt(parts[0]); } catch (Exception e) { newLine = 1; }

                    int minusIdx = raw.indexOf('-');
                    if (minusIdx >= 0) {
                        String segOld = raw.substring(minusIdx + 1, raw.indexOf('+')).trim();
                        String[] partsOld = segOld.split(",");
                        try { oldLine = Integer.parseInt(partsOld[0]); } catch (Exception e) { oldLine = 1; }
                    }
                    inHunk = true;
                } else {
                    inHunk = false;
                }
                out.append(raw).append('\n');
                continue;
            }
            if (!inHunk) { out.append(raw).append('\n'); continue; }
            if (raw.isEmpty()) { out.append('\n'); continue; }
            char tag = raw.charAt(0);
            String content = raw.length() > 1 ? raw.substring(1) : "";

            switch (tag) {
                case ' ': {
                    String id = "A#" + counter++;
                    AnchorEntry e = new AnchorEntry();
                    e.id = id; e.side = 'N'; e.newPath = newPath; e.oldPath = oldPath; e.newLine = newLine; e.oldLine = oldLine;
                    index.put(id, e);
                    out.append(' ').append("<<").append(id).append("|N|").append(newPath).append('|').append(newLine).append(">> ").append(content).append('\n');
                    newLine++; oldLine++;
                    break;
                }
                case '+': {
                    String id = "A#" + counter++;
                    AnchorEntry e = new AnchorEntry();
                    e.id = id; e.side = 'N'; e.newPath = newPath; e.oldPath = oldPath; e.newLine = newLine; e.oldLine = null;
                    index.put(id, e);
                    out.append('+').append("<<").append(id).append("|N|").append(newPath).append('|').append(newLine).append(">> ").append(content).append('\n');
                    newLine++;
                    break;
                }
                case '-': {
                    String id = "A#" + counter++;
                    AnchorEntry e = new AnchorEntry();
                    e.id = id; e.side = 'O'; e.newPath = newPath; e.oldPath = oldPath; e.newLine = null; e.oldLine = oldLine;
                    index.put(id, e);
                    out.append('-').append("<<").append(id).append("|O|").append(oldPath).append('|').append(oldLine).append(">> ").append(content).append('\n');
                    oldLine++;
                    break;
                }
                default:
                    out.append(raw).append('\n');
            }
        }
        return counter;
    }
}

