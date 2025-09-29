package com.kevindai.git.helper.mr.service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kevindai.git.helper.confg.GitConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GitLabService {

    private final GitConfig gitConfig;

    private final RestClient restClient = RestClient.create();

    public ParsedMrUrl parseMrUrl(String mrUrl) {
        // Format: https://<host>/<namespace full path>/<project>/-/merge_requests/<mrId>
        // Example: https://gitlab-ultimate.nationalcloud.ae/presight/r100/platform/r100-task-api/-/merge_requests/541
        try {
            URI uri = URI.create(mrUrl);
            String[] parts = uri.getPath().split("/");
            // parts example: ["", "presight", "r100", "platform", "r100-task-api", "-", "merge_requests", "541"]
            if (parts.length < 7) {
                throw new IllegalArgumentException("Invalid MR URL format");
            }
            int dashIdx = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("-".equals(parts[i])) {
                    dashIdx = i;
                    break;
                }
            }
            if (dashIdx < 0 || dashIdx + 2 >= parts.length) {
                throw new IllegalArgumentException("Invalid MR URL format (missing -/merge_requests/<id>)");
            }
            if (!"merge_requests".equals(parts[dashIdx + 1])) {
                throw new IllegalArgumentException("Invalid MR URL: not a merge_requests path");
            }
            int projectIdx = dashIdx - 1;
            if (projectIdx <= 1) {
                throw new IllegalArgumentException("Invalid MR URL: cannot infer project path");
            }
            String projectPath = parts[projectIdx];
            String groupPath = parts[projectIdx - 1]; // namespace name (last segment before project)
            if (projectIdx - 1 < 1) {
                throw new IllegalArgumentException("Invalid MR URL: cannot infer namespace");
            }
            String groupFullPath = String.join("/", Arrays.asList(parts).subList(1, projectIdx)); // full namespace path
            int mrId = Integer.parseInt(parts[dashIdx + 2]);

            ParsedMrUrl parsed = new ParsedMrUrl();
            parsed.setGroupPath(groupPath);
            parsed.setGroupFullPath(groupFullPath);
            parsed.setProjectPath(projectPath);
            parsed.setMrId(mrId);
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse MR URL: " + e.getMessage(), e);
        }
    }

    public long fetchGroupId(ParsedMrUrl parsedMrUrl) {
        // GET /namespaces?search={GROUP_PATH}
        Namespace[] namespaces = restClient.get()
                .uri(gitConfig.getUrl() + "/namespaces?search={q}", Map.of("q", parsedMrUrl.getGroupPath()))
                .accept(MediaType.APPLICATION_JSON)
                .header("PRIVATE-TOKEN", gitConfig.getToken())
                .retrieve()
                .body(Namespace[].class);

        if (namespaces == null || namespaces.length == 0) {
            throw new IllegalStateException("Group not found: " + parsedMrUrl.getGroupPath());
        }

        Optional<Namespace> exact = Arrays.stream(namespaces)
                .filter(ns -> parsedMrUrl.getGroupFullPath().equals(ns.getFull_path()))
                .findFirst();
        return exact.orElse(namespaces[0]).getId();
    }

    public long fetchProjectId(long groupId, String projectPath) {
        // GET /groups/{GROUP_ID}/projects?search={PROJECT_PATH}
        Project[] projects = restClient.get()
                .uri(gitConfig.getUrl() + "/groups/{gid}/projects?search={q}", Map.of("gid", groupId, "q", projectPath))
                .accept(MediaType.APPLICATION_JSON)
                .header("PRIVATE-TOKEN", gitConfig.getToken())
                .retrieve()
                .body(Project[].class);

        if (projects == null || projects.length == 0) {
            throw new IllegalStateException("Project not found under group: " + projectPath);
        }

        Optional<Project> exact = Arrays.stream(projects)
                .filter(p -> projectPath.equals(p.getPath()) || projectPath.equals(p.getName()))
                .findFirst();
        return exact.orElse(projects[0]).getId();
    }

    public List<MrDiff> fetchMrDiffs(long projectId, int mrId) {
        // GET /projects/{PROJECT_ID}/merge_requests/{MR_ID}/diffs
        MrDiff[] diffs = restClient.get()
                .uri(gitConfig.getUrl() + "/projects/{pid}/merge_requests/{mr}/diffs", Map.of("pid", projectId, "mr", mrId))
                .accept(MediaType.APPLICATION_JSON)
                .header("PRIVATE-TOKEN", gitConfig.getToken())
                .retrieve()
                .body(MrDiff[].class);
        return diffs == null ? List.of() : Arrays.asList(diffs);
    }

    public String formatDiffs(List<MrDiff> diffs) {
        if (diffs == null || diffs.isEmpty()) {
            return "(No diffs found)";
        }
        StringBuilder sb = new StringBuilder();
        for (MrDiff d : diffs) {
            String file = StringUtils.hasText(d.getNew_path()) ? d.getNew_path() : "<unknown>";
            sb.append("--- 文件: ").append(file).append(" ---\n");
            if (d.getDiff() != null) {
                sb.append(d.getDiff()).append("\n");
            }
            sb.append("...\n");
        }
        return sb.toString();
    }

    @Data
    public static class ParsedMrUrl {

        private String groupPath;       // namespace name (last segment)
        private String groupFullPath;   // namespace full path
        private String projectPath;     // project/app name
        private int mrId;
    }

    @Data
    public static class Namespace {

        private long id;
        private String path;
        private String full_path;
        private String kind;
    }

    @Data
    public static class Project {

        private long id;
        private String name;
        private String path;
    }

    @Data
    public static class MrDiff {

        private String old_path;
        private String new_path;
        private String a_mode;
        private String b_mode;
        private boolean new_file;
        private boolean renamed_file;
        private boolean deleted_file;
        private String diff; // unified diff format
    }
}

