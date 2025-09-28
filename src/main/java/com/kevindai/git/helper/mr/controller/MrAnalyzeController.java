package com.kevindai.git.helper.mr.controller;

import com.kevindai.git.helper.mr.dto.AnalysisStatus;
import com.kevindai.git.helper.mr.dto.MrAnalyzeRequest;
import com.kevindai.git.helper.mr.dto.MrAnalyzeResponse;
import com.kevindai.git.helper.mr.service.MrAnalyzeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/mr", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class MrAnalyzeController {

    private final MrAnalyzeService mrAnalyzeService;

    @PostMapping(path = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MrAnalyzeResponse analyze(@Valid @RequestBody MrAnalyzeRequest req) {
        try {
            return mrAnalyzeService.analyzeMr(req);
        } catch (Exception e) {
            return MrAnalyzeResponse.builder()
                    .status(AnalysisStatus.FAILURE)
                    .mrUrl(req.getMrUrl())
                    .analysisResult(null)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
