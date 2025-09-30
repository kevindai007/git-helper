package com.kevindai.git.helper.mr.dto.gitlab;

import lombok.Data;

/**
 * GitLab MR diff item (subset of fields).
 */
@Data
public class MrDiff {
    private String old_path;
    private String new_path;
    private String a_mode;
    private String b_mode;
    private boolean new_file;
    private boolean renamed_file;
    private boolean deleted_file;
    private String diff; // unified diff format
}

