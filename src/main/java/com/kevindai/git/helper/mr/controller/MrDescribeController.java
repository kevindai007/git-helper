package com.kevindai.git.helper.mr.controller;

import com.kevindai.git.helper.mr.dto.AnalysisStatus;
import com.kevindai.git.helper.mr.dto.MrDescribeRequest;
import com.kevindai.git.helper.mr.dto.MrDescribeResponse;
import com.kevindai.git.helper.mr.service.MrDescribeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/mr")
@RequiredArgsConstructor
@Slf4j
public class MrDescribeController {

    private final MrDescribeService mrDescribeService;

    // SSE streaming version of describe using reactive token stream
    @PostMapping(path = "/describe", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter describe(@Valid @RequestBody MrDescribeRequest req) {
        // 0 means no timeout; adjust if needed (e.g., 120_000L)
        SseEmitter emitter = new SseEmitter(0L);
        String correlationId = UUID.randomUUID().toString();

        // Start event with metadata
        try {
            Map<String, Object> start = new HashMap<>();
            start.put("mrNewUrl", req.getMrNewUrl());
            start.put("status", AnalysisStatus.IN_PROGRESS);
            start.put("ts", Instant.now().toString());
            start.put("correlationId", correlationId);
            emitter.send(SseEmitter.event().name("start").id(correlationId).data(start));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        StringBuilder aggregate = new StringBuilder();

        Disposable subscription = mrDescribeService.streamDescription(req.getMrNewUrl())
                .subscribe(
                        chunk -> {
                            try {
                                aggregate.append(chunk);
                                emitter.send(SseEmitter.event().name("delta").id(correlationId).data(chunk));
                            } catch (IOException io) {
                                log.warn("Failed to send SSE delta: {}", io.getMessage());
                            }
                        },
                        err -> {
                            try {
                                Map<String, Object> errPayload = new HashMap<>();
                                errPayload.put("message", err.getMessage());
                                errPayload.put("correlationId", correlationId);
                                errPayload.put("ts", Instant.now().toString());
                                emitter.send(SseEmitter.event().name("error").id(correlationId).data(errPayload));
                            } catch (IOException io) {
                                log.warn("Failed to send SSE error frame: {}", io.getMessage());
                            }
                            emitter.completeWithError(err);
                        },
                        () -> {
                            try {
                                MrDescribeResponse resp = MrDescribeResponse.builder()
                                        .status(AnalysisStatus.SUCCESS)
                                        .mrNewUrl(req.getMrNewUrl())
                                        .description(aggregate.toString())
                                        .build();
                                emitter.send(SseEmitter.event().name("done").id(correlationId).data(resp));
                            } catch (IOException e) {
                                log.warn("Failed to send SSE done frame: {}", e.getMessage());
                            }
                            emitter.complete();
                        }
                );

        // Cleanup when client disconnects or timeout
        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(() -> {
            try {
                Map<String, Object> errPayload = new HashMap<>();
                errPayload.put("message", "SSE timeout");
                errPayload.put("correlationId", correlationId);
                errPayload.put("ts", Instant.now().toString());
                emitter.send(SseEmitter.event().name("error").id(correlationId).data(errPayload));
            } catch (IOException ignored) {
            }
            subscription.dispose();
        });

        return emitter;
    }
}
