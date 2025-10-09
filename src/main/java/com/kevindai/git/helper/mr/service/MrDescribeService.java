package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.mr.dto.AnalysisStatus;
import com.kevindai.git.helper.mr.dto.MrDescribeResponse;
import com.kevindai.git.helper.mr.dto.gitlab.CompareResponse;
import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import com.kevindai.git.helper.mr.prompt.MrDescriptionPrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MrDescribeService {

    private final GitLabService gitLabService;
    private final ChatClient chatClient;
    private final AddressableDiffBuilder addressableDiffBuilder;
    private final GitTokenService gitTokenService;
    private final GitLabRequestContext gitLabRequestContext;

    public MrDescribeResponse generateDescription(String mrNewUrl) {
        // 1) Parse project/group context and query params
        var ctx = parseNewMrUrl(mrNewUrl);

        // Resolve token by group full path when possible
        if (StringUtils.hasText(ctx.groupFullPath)) {
            String token = gitTokenService.resolveTokenForGroup(ctx.groupFullPath);
            gitLabRequestContext.setGroupFullPath(ctx.groupFullPath);
            gitLabRequestContext.setToken(token);
        }

        // 2) Call compare API
        CompareResponse compare = gitLabService.compare(ctx.projectId, ctx.sourceBranch, ctx.targetBranch, true, true);
        List<MrDiff> diffs = Optional.ofNullable(compare.getDiffs()).orElse(List.of());

        // 3) Merge diffs into a single annotated content block
        String merged = addressableDiffBuilder.buildAnnotatedWithIndex(diffs).getContent();
        if (!StringUtils.hasText(merged)) {
            merged = mergePlain(diffs);
        }

        // 4) Send to LLM to produce description
        String description = chatClient
                .prompt(MrDescriptionPrompt.SYSTEM_PROMPT)
                .user(merged)
                .call()
                .chatResponse().getResults().getFirst().getOutput().getText();

        return MrDescribeResponse.builder()
                .status(AnalysisStatus.SUCCESS)
                .mrNewUrl(mrNewUrl)
                .projectId(ctx.projectId)
                .sourceBranch(ctx.sourceBranch)
                .targetBranch(ctx.targetBranch)
                .description(description)
                .build();
    }

    private String mergePlain(List<MrDiff> diffs) {
        StringBuilder sb = new StringBuilder();
        for (MrDiff d : diffs) {
            String path = StringUtils.hasText(d.getNew_path()) ? d.getNew_path() : d.getOld_path();
            if (StringUtils.hasText(path)) {
                sb.append("===== ").append(path).append(" =====\n");
            }
            if (StringUtils.hasText(d.getDiff())) {
                sb.append(d.getDiff()).append("\n\n");
            }
        }
        return sb.toString();
    }

    private record NewMrContext(Long projectId, String sourceBranch, String targetBranch, String groupFullPath) {
    }

    private NewMrContext parseNewMrUrl(String url) {
        try {
            URI uri = URI.create(url.trim());
            String rawPath = uri.getPath();
            List<String> parts = Arrays.stream(rawPath.split("/"))
                    .filter(StringUtils::hasText)
                    .toList();

            // Identify group/subgroup/project path before "/-/merge_requests/new"
            int dashIdx = -1;
            for (int i = 0; i < parts.size() - 2; i++) {
                if ("-".equals(parts.get(i)) && "merge_requests".equals(parts.get(i + 1)) &&
                        "new".equals(parts.get(i + 2))) {
                    dashIdx = i;
                    break;
                }
            }
            String groupFullPath = null;
            if (dashIdx > 0) {
                int projectIdx = dashIdx - 1;
                if (projectIdx >= 1) {
                    List<String> groupSegments = parts.subList(0, projectIdx);
                    groupFullPath = String.join("/", groupSegments);
                }
            }

            Map<String, String> q = parseQuery(uri.getRawQuery());
            String spid = firstNonBlank(q.get("merge_request[source_project_id]"), q.get("merge_request[target_project_id]"));
            String from = q.get("merge_request[source_branch]");
            String to = q.get("merge_request[target_branch]");
            if (!StringUtils.hasText(spid) || !StringUtils.hasText(from) || !StringUtils.hasText(to)) {
                throw new IllegalArgumentException("Missing required query params: source_project_id/target_project_id, source_branch, target_branch");
            }

            return new NewMrContext(Long.parseLong(spid), from, to, groupFullPath);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse new MR URL: " + e.getMessage(), e);
        }
    }

    private Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!StringUtils.hasText(raw)) return map;
        String[] pairs = raw.split("&");
        for (String p : pairs) {
            int idx = p.indexOf('=');
            String k = idx >= 0 ? p.substring(0, idx) : p;
            String v = idx >= 0 ? p.substring(idx + 1) : "";
            k = URLDecoder.decode(k, StandardCharsets.UTF_8);
            v = URLDecoder.decode(v, StandardCharsets.UTF_8);
            map.put(k, v);
        }
        return map;
    }

    private String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }
}
