package com.kevindai.git.helper.mr.controller;

import com.kevindai.git.helper.mr.dto.AnalysisStatus;
import com.kevindai.git.helper.mr.dto.MrDescribeRequest;
import com.kevindai.git.helper.mr.dto.MrDescribeResponse;
import com.kevindai.git.helper.mr.service.MrDescribeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/mr", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MrDescribeController {

    private final MrDescribeService mrDescribeService;

    @PostMapping(path = "/describe", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MrDescribeResponse describe(@Valid @RequestBody MrDescribeRequest req) {
        try {
            return mrDescribeService.generateDescription(req.getMrNewUrl());
        } catch (Exception e) {
            return MrDescribeResponse.builder()
                    .status(AnalysisStatus.FAILURE)
                    .mrNewUrl(req.getMrNewUrl())
                    .description(null)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}

