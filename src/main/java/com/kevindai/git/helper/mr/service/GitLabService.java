package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.config.GitConfig;
import com.kevindai.git.helper.mr.dto.ParsedMrUrl;
import com.kevindai.git.helper.mr.dto.gitlab.*;
import com.kevindai.git.helper.mr.util.GitLabUrlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

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
    private final GitLabUrlParser urlParser;

    // Centralized MR URL parsing
    public ParsedMrUrl parseMrUrl(String mrUrl) {
        return urlParser.parseExistingMrUrl(mrUrl);
    }


    @Deprecated
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
                .filter(ns -> {
                    String expected = parsedMrUrl.getGroupFullPath() != null ? parsedMrUrl.getGroupFullPath() : parsedMrUrl.getProjectFullPath();
                    return expected != null && expected.equals(ns.getFull_path());
                })
                .findFirst();
        return exact.orElse(namespaces[0]).getId();
    }

    @Deprecated
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

    /**
     * Resolve project ID using full path, avoiding group+search ambiguity.
     * Equivalent to GET /projects/{urlencoded group/path/project} in GitLab API.
     */
    public long resolveProjectId(String groupFullPath, String projectPath) {
        String full = (groupFullPath == null || groupFullPath.isBlank()) ? projectPath : groupFullPath + "/" + projectPath;
        try {
            String encoded = java.net.URLEncoder.encode(full, java.nio.charset.StandardCharsets.UTF_8);
            Project project = restClient.get()
                    .uri(gitConfig.getUrl() + "/projects/{full}", Map.of("full", encoded))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Project.class);
            if (project == null) {
                throw new IllegalStateException("Project not found by full path: " + full);
            }
            return project.getId();
        } catch (Exception e) {
            // Fallback: try group+search if full path resolution fails
            log.warn("Full-path project resolution failed for {}: {}. Falling back to group+search.", full, e.getMessage());
            String groupPathOnly = groupFullPath == null ? null : (groupFullPath.contains("/") ? groupFullPath.substring(groupFullPath.lastIndexOf('/') + 1) : groupFullPath);
            ParsedMrUrl pmu = new ParsedMrUrl();
            pmu.setGroupFullPath(groupFullPath);
            pmu.setProjectFullPath(groupFullPath);
            pmu.setGroupPath(groupPathOnly);
            long groupId = fetchGroupId(pmu);
            return fetchProjectId(groupId, projectPath);
        }
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

    public CompareResponse compare(long projectId, String from, String to,
                                                                         boolean unidiff, boolean straight) {
        return restClient.get()
                .uri(gitConfig.getUrl() + "/projects/{pid}/repository/compare?from={from}&to={to}&unidiff={ud}&straight={st}",
                        Map.of(
                                "pid", projectId,
                                "from", from,
                                "to", to,
                                "ud", String.valueOf(unidiff),
                                "st", String.valueOf(straight)
                        ))
                .headers(h -> h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON)))
                .retrieve()
                .body(com.kevindai.git.helper.mr.dto.gitlab.CompareResponse.class);
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
