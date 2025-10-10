package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.mr.dto.gitlab.CompareResponse;
import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import com.kevindai.git.helper.mr.prompt.MrDescriptionPrompt;
import com.kevindai.git.helper.mr.util.GitLabUrlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

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

    public Flux<String> streamDescription(String mrNewUrl) {
        return Flux.defer(() -> {
            try {
                var ctx = urlParser.parseNewMrUrl(mrNewUrl);
                if (StringUtils.hasText(ctx.groupFullPath())) {
                    String token = gitTokenService.resolveTokenForGroup(ctx.groupFullPath());
                    gitLabRequestContext.setGroupFullPath(ctx.groupFullPath());
                    gitLabRequestContext.setToken(token);
                }
                CompareResponse compare = gitLabService.compare(ctx.projectId(), ctx.sourceBranch(), ctx.targetBranch(), true, true);
                List<MrDiff> diffs = Optional.ofNullable(compare.getDiffs()).orElse(List.of());
                String merged = addressableDiffBuilder.buildAnnotatedWithIndex(diffs).getContent();
                if (!StringUtils.hasText(merged)) {
                    merged = mergePlain(diffs);
                }
                // Stream tokens/content from LLM
                return chatClient
                        .prompt(MrDescriptionPrompt.SYSTEM_PROMPT)
                        .user(merged)
                        .stream()
                        .content()
                        // map 4xx/5xx into textual SSE so the writer doesn't try to serialize a map
                        .onErrorResume(WebClientResponseException.class, e -> {
                            log.error("LLM call failed [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
                            return Flux.just("LLM request failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
                        })
                        .onErrorResume(Throwable.class, e -> {
                            log.error("LLM call failed: {}", e.getMessage(), e);
                            return Flux.just("LLM request failed: " + e.getMessage());
                        });
            } catch (Exception e) {
                log.error("streamDescription setup failed: {}", e.getMessage(), e);
                return Flux.error(e);
            }
        });
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
