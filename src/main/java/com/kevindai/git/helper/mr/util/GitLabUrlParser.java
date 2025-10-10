package com.kevindai.git.helper.mr.util;

import com.kevindai.git.helper.mr.dto.ParsedMrUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Centralized parsing utilities for GitLab MR-related URLs.
 */
@Slf4j
@Component
public class GitLabUrlParser {

    /**
     * Parse an existing MR URL, extracting group namespace, project path, and MR IID.
     * Example: https://gitlab.com/group/subgroup/project/-/merge_requests/123
     */
    public ParsedMrUrl parseExistingMrUrl(String mrUrl) {
        try {
            URI uri = URI.create(mrUrl.trim());
            String rawPath = uri.getPath();
            if (!StringUtils.hasText(rawPath)) {
                throw new IllegalArgumentException("Empty path");
            }
            List<String> parts = Arrays.stream(rawPath.split("/"))
                    .filter(StringUtils::hasText)
                    .toList();

            int dashIdx = -1;
            for (int i = 0; i < parts.size() - 2; i++) {
                if ("-".equals(parts.get(i)) && "merge_requests".equals(parts.get(i + 1))) {
                    dashIdx = i;
                    break;
                }
            }
            if (dashIdx < 0 || dashIdx + 2 >= parts.size()) {
                log.error("Parts: {}", parts);
                throw new IllegalArgumentException("Invalid MR URL pattern");
            }

            int projectIdx = dashIdx - 1;
            if (projectIdx < 1) {
                log.error("Parts: {}", parts);
                throw new IllegalArgumentException("Cannot determine project path");
            }

            int mrId = Integer.parseInt(parts.get(dashIdx + 2));
            String projectPath = parts.get(projectIdx);
            List<String> groupSegments = parts.subList(0, projectIdx);
            if (groupSegments.isEmpty()) {
                throw new IllegalArgumentException("Group path missing");
            }

            String groupFullPath = String.join("/", groupSegments); // full group hierarchy (no leading slash)
            String groupPath = groupSegments.get(groupSegments.size() - 1); // immediate parent only

            ParsedMrUrl parsed = new ParsedMrUrl();
            parsed.setGroupPath(groupPath);
            parsed.setProjectPath(projectPath);
            // Backward-compat: projectFullPath historically meant group full path
            parsed.setProjectFullPath(groupFullPath);
            parsed.setGroupFullPath(groupFullPath);
            parsed.setMrId(mrId);
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse MR URL: " + e.getMessage(), e);
        }
    }

    /**
     * Parsed context for the "new MR" page where compare params exist in the query.
     */
    public record NewMrContext(Long projectId, String sourceBranch, String targetBranch, String groupFullPath) {}

    /**
     * Parse the \"new MR\" URL containing compare params.
     * Example: https://gitlab.com/group/subgroup/project/-/merge_requests/new?merge_request[source_project_id]=123&merge_request[target_branch]=main&merge_request[source_branch]=feat
     */
    public NewMrContext parseNewMrUrl(String url) {
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

