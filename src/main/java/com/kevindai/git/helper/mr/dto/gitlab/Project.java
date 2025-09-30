package com.kevindai.git.helper.mr.dto.gitlab;

import lombok.Data;

/**
 * GitLab project payload (subset).
 */
@Data
public class Project {
    private long id;
    private String name;
    private String path;
}

