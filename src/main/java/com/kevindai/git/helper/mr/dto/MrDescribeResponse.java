package com.kevindai.git.helper.mr.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MrDescribeResponse {
    private AnalysisStatus status;
    private String mrNewUrl;
    private Long projectId;
    private String sourceBranch;
    private String targetBranch;
    private String description;   // generated markdown
    private String errorMessage;  // on failure
}

