package com.kevindai.git.helper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/chat")
public class ChatController {
    private final ChatClient chatClient;

    @PostMapping("/completion")
    public String chat(@RequestParam String message) {
        return chatClient.prompt().user(message).call()
                .chatResponse().getResults().getFirst().getOutput().getText();
    }
}
