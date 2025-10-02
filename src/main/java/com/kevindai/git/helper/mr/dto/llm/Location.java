package com.kevindai.git.helper.mr.dto.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Location {
    private String file;
    private String lineType; // new_line|old_line
    private Integer startLine;
    private Integer endLine;
    private Integer startCol;
    private Integer endCol;

    @Getter
    @AllArgsConstructor
    public enum LineType {
        NEW_LINE("new_line"),
        OLD_LINE("old_line");

        private final String value;
    }
}

