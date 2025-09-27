package com.kevindai.git.helper.mr.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MrAnalyzeResponse {
    private String status; // success | failure
    private String mrUrl;
    private String analysisResult; // LLM result text
    private String errorMessage;   // optional on failure
}

