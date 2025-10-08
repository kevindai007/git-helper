package com.kevindai.git.helper.mr.dto.gitlab;

import lombok.Data;

import java.util.List;

@Data
public class CompareResponse {
    private Commit commit;           // optional fields not strictly needed
    private List<Commit> commits;    // optional
    private List<MrDiff> diffs;      // unified diffs
    private Boolean compare_timeout; // optional
    private Boolean compare_same_ref;// optional
    private String web_url;          // optional

    @Data
    public static class Commit {
        private String id;
        private String short_id;
        private String title;
        private String message;
        private String web_url;
        private String created_at;
    }
}

