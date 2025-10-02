package com.kevindai.git.helper.mr.dto.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Stats {
    private Map<String, Integer> countBySeverity;
    private Map<String, Integer> countByCategory;
}

