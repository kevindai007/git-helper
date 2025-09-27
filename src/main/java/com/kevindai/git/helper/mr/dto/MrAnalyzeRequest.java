package com.kevindai.git.helper.mr.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class MrAnalyzeRequest {

    @NotBlank(message = "Merge Request URL must not be blank")
    private String mrUrl;
}

