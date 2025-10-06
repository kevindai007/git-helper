package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.config.GitConfig;
import com.kevindai.git.helper.mr.dto.ParsedMrUrl;
import com.kevindai.git.helper.mr.dto.gitlab.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitLabService {

    private final GitConfig gitConfig;
    private final RestClient restClient;

    // java
    public ParsedMrUrl parseMrUrl(String mrUrl) {
        // Example URL: https://gitlab.com/group/subgroup/project/-/merge_requests/123
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

            String projectFullPath = String.join("/", groupSegments); // full group hierarchy (no leading slash)
            String groupPath = groupSegments.get(groupSegments.size() - 1); // immediate parent only

            ParsedMrUrl parsed = new ParsedMrUrl();
            parsed.setGroupPath(groupPath);
            parsed.setProjectPath(projectPath);
            parsed.setProjectFullPath(projectFullPath);
            parsed.setMrId(mrId);
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse MR URL: " + e.getMessage(), e);
        }
    }


    public long fetchGroupId(ParsedMrUrl parsedMrUrl) {
        Namespace[] namespaces = restClient.get()
                .uri(gitConfig.getUrl() + "/namespaces?search={q}", Map.of("q", parsedMrUrl.getGroupPath()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Namespace[].class);

        if (namespaces == null || namespaces.length == 0) {
            log.error("Namespaces for group {}: {}", parsedMrUrl.getGroupPath(), Arrays.toString(namespaces));
            throw new IllegalStateException("Group not found: " + parsedMrUrl.getGroupPath());
        }

        Optional<Namespace> exact = Arrays.stream(namespaces)
                .filter(ns -> parsedMrUrl.getProjectFullPath().equals(ns.getFull_path()))
                .findFirst();
        return exact.orElse(namespaces[0]).getId();
    }

    public long fetchProjectId(long groupId, String projectPath) {
        Project[] projects = restClient.get()
                .uri(gitConfig.getUrl() + "/groups/{gid}/projects?search={q}", Map.of("gid", groupId, "q", projectPath))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Project[].class);

        if (projects == null || projects.length == 0) {
            log.error("Projects under group {}: {}", groupId, Arrays.toString(projects));
            throw new IllegalStateException("Project not found under group: " + projectPath);
        }

        Optional<Project> exact = Arrays.stream(projects)
                .filter(p -> projectPath.equals(p.getPath()) || projectPath.equals(p.getName()))
                .findFirst();
        return exact.orElse(projects[0]).getId();
    }

    public MrDetail fetchMrDetails(long projectId, int mrId) {
        return restClient.get().uri(gitConfig.getUrl() + "/projects/{pid}/merge_requests/{mr}", Map.of("pid", projectId, "mr", mrId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MrDetail.class);
    }

    public List<MrDiff> fetchMrDiffs(long projectId, int mrId) {
        MrDiff[] diffs = restClient.get()
                .uri(gitConfig.getUrl() + "/projects/{pid}/merge_requests/{mr}/diffs", Map.of("pid", projectId, "mr", mrId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MrDiff[].class);
        return diffs == null ? List.of() : Arrays.asList(diffs);
    }

    public List<MrVersion> fetchMrVersions(long projectId, int mrId) {
        MrVersion[] versions = restClient.get()
                .uri(gitConfig.getUrl() + "/projects/{pid}/merge_requests/{mr}/versions", Map.of("pid", projectId, "mr", mrId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MrVersion[].class);
        return versions == null ? List.of() : Arrays.asList(versions);
    }

    public void createMrDiscussion(long projectId,
                                   int mrId,
                                   String baseSha,
                                   String headSha,
                                   String startSha,
                                   String filePath,
                                   Integer newLine,
                                   Integer oldLine,
                                   String body) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("position[position_type]", "text");
        form.add("position[base_sha]", baseSha);
        form.add("position[head_sha]", headSha);
        form.add("position[start_sha]", startSha);
        form.add("position[new_path]", filePath);
        form.add("position[old_path]", filePath);
        if (newLine != null) {
            form.add("position[new_line]", String.valueOf(newLine));
        }
        if (oldLine != null) {
            form.add("position[old_line]", String.valueOf(oldLine));
        }
        form.add("body", body == null ? "" : body);

        restClient.post()
                .uri(gitConfig.getUrl() + "/projects/{pid}/merge_requests/{mr}/discussions", Map.of("pid", projectId, "mr", mrId))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }


}