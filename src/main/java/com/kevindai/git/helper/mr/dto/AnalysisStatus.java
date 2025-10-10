package com.kevindai.git.helper.mr.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Analysis status for MR analysis responses.
 */
public enum AnalysisStatus {
    IN_PROGRESS,
    SUCCESS,
    FAILURE,
    NO_CHANGE;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
