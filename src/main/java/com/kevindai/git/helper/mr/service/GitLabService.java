package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.confg.GitConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GitLabService {

    private final GitConfig gitConfig;

    private final RestClient restClient = RestClient.create();

    public ParsedMrUrl parseMrUrl(String mrUrl) {
        // Expected: https://gitlab.com/<groupPath>/<projectPath>/-/merge_requests/<mrId>
        try {
            URI uri = URI.create(mrUrl);
            String[] parts = uri.getPath().split("/");
            // [ , <group...>, <project>, -, merge_requests, <id> ]
            if (parts.length < 6) {
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
                throw new IllegalArgumentException("Invalid MR URL format (missing -/merge_requests)");
            }
            if (!"merge_requests".equals(parts[dashIdx + 1])) {
                throw new IllegalArgumentException("Invalid MR URL: not a merge_request path");
            }
            String mrIdStr = parts[dashIdx + 2];
            int mrId = Integer.parseInt(mrIdStr);

            // group path may be multi-segment between index 1 and last project index-1
            int projectIdx = dashIdx - 1;
            if (projectIdx <= 1) {
                throw new IllegalArgumentException("Invalid MR URL: cannot infer project path");
            }
            String projectPath = parts[projectIdx];
            String groupPath = String.join("/", Arrays.asList(parts).subList(1, projectIdx));

            ParsedMrUrl parsed = new ParsedMrUrl();
            parsed.setGroupPath(groupPath);
            parsed.setProjectPath(projectPath);
            parsed.setMrId(mrId);
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse MR URL: " + e.getMessage(), e);
        }
    }

    public long fetchGroupId(String groupPath) {
        // GET /namespaces?search={GROUP_PATH}
        Namespace[] namespaces = restClient.get()
                .uri(gitConfig.getUrl() + "/namespaces?search={q}", Map.of("q", groupPath))
                .accept(MediaType.APPLICATION_JSON)
                .header("PRIVATE-TOKEN", gitConfig.getToken())
                .retrieve()
                .body(Namespace[].class);

        if (namespaces == null || namespaces.length == 0) {
            throw new IllegalStateException("Group not found: " + groupPath);
        }

        Optional<Namespace> exact = Arrays.stream(namespaces)
                .filter(ns -> groupPath.equals(ns.getFull_path()) || groupPath.equals(ns.getPath()))
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

    public MrDetail fetchMrDetails(long projectId, int mrId) {
        // GET /projects/{PROJECT_ID}/merge_requests/{MR_ID}
        return restClient.get().uri(gitConfig.getUrl() + "/projects/{pid}/merge_requests/{mr}", Map.of("pid", projectId, "mr", mrId))
                .accept(MediaType.APPLICATION_JSON)
                .header("PRIVATE-TOKEN", gitConfig.getToken())
                .retrieve()
                .body(MrDetail.class);

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
        private String groupPath;
        private String projectPath;
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
    public static class MrDetail {
        private long id;
        private long project_id;
        private long iid;
        private String title;
        private String state;
        private String target_branch;
        private String source_branch;
        private String sha;
        private String web_url;

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

