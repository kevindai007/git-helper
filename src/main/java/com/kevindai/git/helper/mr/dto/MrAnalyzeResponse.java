package com.kevindai.git.helper.mr.dto;

import com.kevindai.git.helper.mr.dto.llm.LlmAnalysisReport;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MrAnalyzeResponse {
    private AnalysisStatus status;
    private String mrUrl;
    private LlmAnalysisReport analysisResult; // LLM result
    private String errorMessage;   // optional on failure
}
