package com.kevindai.git.helper.mr.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdoptResponse {
    private String status; // success | failure
    private String message;
}

