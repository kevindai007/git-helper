package com.kevindai.git.helper.mr.dto;

import lombok.Data;

/**
 * Parsed components from a GitLab MR URL.
 */
@Data
public class ParsedMrUrl {
    private String groupPath;
    private String projectPath;
    private String projectFullPath;
    private int mrId;
}

