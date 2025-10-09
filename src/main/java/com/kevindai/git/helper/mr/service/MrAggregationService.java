package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.entity.MrAnalysisDetailEntity;
import com.kevindai.git.helper.entity.MrInfoEntity;
import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import com.kevindai.git.helper.mr.dto.llm.LlmAnalysisReport;
import com.kevindai.git.helper.mr.model.Severity;
import com.kevindai.git.helper.mr.prompt.CrossFileCoherencePrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MrAggregationService {

    private final ChatClient chatClient;
    private final MrAnalysisDetailService detailService;

    public LlmAnalysisReport aggregate(MrInfoEntity info,
                                       List<MrDiff> diffs,
                                       AddressableDiffBuilder.AnnotatedDiff annotated) {
        var details = detailService.loadDetails(info.getId());
        Map<String, List<MrAnalysisDetailEntity>> byFile = details.stream()
                .filter(d -> StringUtils.hasText(d.getFile()))
                .collect(Collectors.groupingBy(MrAnalysisDetailEntity::getFile));

        StringBuilder summaries = new StringBuilder();
        summaries.append("=== FILE SUMMARIES ===\n");
        byFile.forEach((file, list) -> {
            long high = list.stream().filter(d -> {
                Severity sev = Severity.from(d.getSeverity());
                return sev == Severity.BLOCKER || sev == Severity.HIGH;
            }).count();
            long medium = list.stream().filter(d -> Severity.from(d.getSeverity()) == Severity.MEDIUM).count();
            long low = list.stream().filter(d -> {
                Severity sev = Severity.from(d.getSeverity());
                return sev == Severity.LOW || sev == Severity.INFO;
            }).count();
            summaries.append("- ").append(file).append(" | sev(blocker/high/med/low/info): ")
                    .append(high).append('/').append(medium).append('/').append(low).append("\n");
            int cap = 5;
            for (var d : list.stream()
                    .sorted(Comparator.comparingInt(x -> Severity.from(x.getSeverity()).rank()))
                    .limit(cap).toList()) {
                summaries.append("  • [").append(Optional.ofNullable(d.getSeverity()).orElse("?"))
                        .append("] ").append(Optional.ofNullable(d.getTitle()).orElse(""))
                        .append(" @ ").append(Optional.ofNullable(d.getAnchorId()).orElse("?"))
                        .append("\n");
            }
        });

        StringBuilder bridges = new StringBuilder();
        bridges.append("\n=== BRIDGE SNIPPETS (ANCHORS INCLUDED) ===\n");
        Map<String, Set<String>> tokenToFiles = new HashMap<>();
        for (var e : byFile.entrySet()) {
            String file = e.getKey();
            for (var d : e.getValue()) {
                if (StringUtils.hasText(d.getTitle())) collectTokens(tokenToFiles, d.getTitle(), file);
                if (StringUtils.hasText(d.getEvidence())) collectTokens(tokenToFiles, d.getEvidence(), file);
            }
        }
        List<String> candidates = tokenToFiles.entrySet().stream()
                .filter(en -> en.getValue().size() >= 2)
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .map(Map.Entry::getKey)
                .limit(5)
                .toList();

        String annotatedContent = annotated.getContent();
        for (String tok : candidates) {
            bridges.append("-- token: ").append(tok).append(" --\n");
            int found = 0;
            int idx = 0;
            while (found < 8 && idx >= 0) {
                idx = annotatedContent.indexOf(tok, idx);
                if (idx < 0) break;
                int lineStart = annotatedContent.lastIndexOf('\n', Math.max(0, idx - 1)) + 1;
                int lineEnd = annotatedContent.indexOf('\n', idx);
                if (lineEnd < 0) lineEnd = annotatedContent.length();
                String line = annotatedContent.substring(lineStart, lineEnd);
                if (line.contains("<<A#")) {
                    bridges.append(line).append('\n');
                    found++;
                }
                idx = lineEnd + 1;
            }
        }

        String userContent = summaries.append('\n').append(bridges).toString();
        return chatClient
                .prompt(CrossFileCoherencePrompt.SYSTEM_PROMPT)
                .user(userContent)
                .call()
                .entity(LlmAnalysisReport.class);
    }

    private static void collectTokens(Map<String, Set<String>> map, String text, String file) {
        if (text == null) return;
        String cleaned = text.replaceAll("[^A-Za-z0-9_./-]", " ");
        for (String w : cleaned.split("\\s+")) {
            if (w.length() < 3) continue;
            if (isStopWord(w)) continue;
            map.computeIfAbsent(w, k -> new HashSet<>()).add(file);
        }
    }

    private static boolean isStopWord(String w) {
        String x = w.toLowerCase(Locale.ROOT);
        return switch (x) {
            case "the", "and", "for", "with", "from", "into", "that", "this", "have", "has", "should",
                 "class", "public", "private", "protected", "static", "final", "const", "let", "var",
                 "import", "package", "new", "old", "line", "lines", "at", "on", "path", "file" -> true;
            default -> false;
        };
    }

    public List<MrAnalysisDetailEntity> loadDetails(long mrInfoId) {
        return detailService.loadDetails(mrInfoId);
    }
}

