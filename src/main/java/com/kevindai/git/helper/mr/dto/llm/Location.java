package com.kevindai.git.helper.mr.dto.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Location {
    private String file;
    private Integer startLine;
    private Integer endLine;
    private Integer startCol;
    private Integer endCol;
}

