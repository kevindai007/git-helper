package com.kevindai.git.helper.mr.dto;

import lombok.Data;

/**
 * Parsed components from a GitLab MR URL.
 */
@Data
public class ParsedMrUrl {
    private String groupPath;
    private String projectPath;
    /**
     * Deprecated: this actually held the full group path (without project).
     * Use {@link #groupFullPath} instead.
     */
    @Deprecated
    private String projectFullPath;

    /**
     * Full group namespace path (supports nested groups), without the project name.
     * Example: "group/subgroup" for URL .../group/subgroup/project/-/merge_requests/123
     */
    private String groupFullPath;
    private int mrId;
}
