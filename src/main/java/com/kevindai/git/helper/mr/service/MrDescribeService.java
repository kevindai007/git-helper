package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.mr.dto.AnalysisStatus;
import com.kevindai.git.helper.mr.dto.MrDescribeResponse;
import com.kevindai.git.helper.mr.dto.gitlab.CompareResponse;
import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import com.kevindai.git.helper.mr.prompt.MrDescriptionPrompt;
import com.kevindai.git.helper.mr.util.GitLabUrlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MrDescribeService {

    private final GitLabService gitLabService;
    private final ChatClient chatClient;
    private final AddressableDiffBuilder addressableDiffBuilder;
    private final GitTokenService gitTokenService;
    private final GitLabRequestContext gitLabRequestContext;
    private final GitLabUrlParser urlParser;

    public MrDescribeResponse generateDescription(String mrNewUrl) {
        // 1) Parse project/group context and query params
        var ctx = urlParser.parseNewMrUrl(mrNewUrl);

        // Resolve token by group full path when possible
        if (StringUtils.hasText(ctx.groupFullPath())) {
            String token = gitTokenService.resolveTokenForGroup(ctx.groupFullPath());
            gitLabRequestContext.setGroupFullPath(ctx.groupFullPath());
            gitLabRequestContext.setToken(token);
        }

        // 2) Call compare API
        CompareResponse compare = gitLabService.compare(ctx.projectId(), ctx.sourceBranch(), ctx.targetBranch(), true, true);
        List<MrDiff> diffs = Optional.ofNullable(compare.getDiffs()).orElse(List.of());

        // 3) Merge diffs into a single annotated content block
        String merged = addressableDiffBuilder.buildAnnotatedWithIndex(diffs).getContent();
        if (!StringUtils.hasText(merged)) {
            merged = mergePlain(diffs);
        }

        // 4) Send to LLM to produce description
        String description = chatClient
                .prompt(MrDescriptionPrompt.SYSTEM_PROMPT)
                .user(merged)
                .call()
                .chatResponse().getResults().getFirst().getOutput().getText();

        return MrDescribeResponse.builder()
                .status(AnalysisStatus.SUCCESS)
                .mrNewUrl(mrNewUrl)
                .projectId(ctx.projectId())
                .sourceBranch(ctx.sourceBranch())
                .targetBranch(ctx.targetBranch())
                .description(description)
                .build();
    }

    private String mergePlain(List<MrDiff> diffs) {
        StringBuilder sb = new StringBuilder();
        for (MrDiff d : diffs) {
            String path = StringUtils.hasText(d.getNew_path()) ? d.getNew_path() : d.getOld_path();
            if (StringUtils.hasText(path)) {
                sb.append("===== ").append(path).append(" =====\n");
            }
            if (StringUtils.hasText(d.getDiff())) {
                sb.append(d.getDiff()).append("\n\n");
            }
        }
        return sb.toString();
    }

}
