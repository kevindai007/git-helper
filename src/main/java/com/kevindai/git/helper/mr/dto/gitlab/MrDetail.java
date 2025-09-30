package com.kevindai.git.helper.mr.dto.gitlab;

import lombok.Data;

/**
 * GitLab MR detail payload (subset).
 */
@Data
public class MrDetail {
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

