package com.kevindai.git.helper.mr.dto.gitlab;

import lombok.Data;

@Data
public class MrVersion {
    private long id;
    private String head_commit_sha;
    private String base_commit_sha;
    private String start_commit_sha;
    private String created_at; // ISO8601 string
    private long merge_request_id;
    private String state;
    private String real_size;
    private String patch_id_sha;
}

