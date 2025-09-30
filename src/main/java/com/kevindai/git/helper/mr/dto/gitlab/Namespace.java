package com.kevindai.git.helper.mr.dto.gitlab;

import lombok.Data;

/**
 * GitLab namespace payload (subset).
 */
@Data
public class Namespace {
    private long id;
    private String path;
    private String full_path;
    private String kind;
}

