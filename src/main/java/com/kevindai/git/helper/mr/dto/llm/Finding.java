package com.kevindai.git.helper.mr.dto.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Finding {
    private String id;
    private String severity; // blocker|high|medium|low|info
    private String category; // correctness|performance|security|maintainability|style|docs|tests
    private String ruleId;
    private String title;
    private String description;
    private Location location;
    private String evidence;
    private Remediation remediation;
    private Double confidence;
    private List<String> tags;
}

