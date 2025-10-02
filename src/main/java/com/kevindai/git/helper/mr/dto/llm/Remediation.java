package com.kevindai.git.helper.mr.dto.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Remediation {
    private String steps;
    private String diff; // unified diff text (optional)
}

