package com.kevindai.git.helper.mr.dto.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmAnalysisReport {
    private String schemaVersion;
    private String promptType; // e.g., JAVA, PYTHON, JAVASCRIPT, GENERIC
    private List<Finding> findings;
    private Stats stats; // optional
    private String summaryMarkdown; // optional
}

