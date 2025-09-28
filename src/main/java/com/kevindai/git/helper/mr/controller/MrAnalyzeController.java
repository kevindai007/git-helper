package com.kevindai.git.helper.mr.controller;

import com.kevindai.git.helper.mr.dto.MrAnalyzeRequest;
import com.kevindai.git.helper.mr.dto.MrAnalyzeResponse;
import com.kevindai.git.helper.mr.service.GitLabService;
import com.kevindai.git.helper.mr.service.LlmAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/mr", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MrAnalyzeController {

    private final GitLabService gitLabService;
    private final LlmAnalysisService llmAnalysisService;

    @PostMapping(path = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MrAnalyzeResponse analyze(@RequestBody MrAnalyzeRequest req) {
        try {
            var parsed = gitLabService.parseMrUrl(req.getMrUrl());
            long groupId = gitLabService.fetchGroupId(parsed.getGroupPath());
            long projectId = gitLabService.fetchProjectId(groupId, parsed.getProjectPath());
            var diffs = gitLabService.fetchMrDiffs(projectId, parsed.getMrId());
            String formatted = gitLabService.formatDiffs(diffs);
            String analysis = llmAnalysisService.analyzeDiff(formatted, diffs);
            return MrAnalyzeResponse.builder()
                    .status("success")
                    .mrUrl(req.getMrUrl())
                    .analysisResult(analysis)
                    .build();
        } catch (Exception e) {
            return MrAnalyzeResponse.builder()
                    .status("failure")
                    .mrUrl(req.getMrUrl())
                    .analysisResult(null)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}

