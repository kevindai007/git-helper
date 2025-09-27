package com.kevindai.git.helper.confg;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AIConfig {
    private final ChatClient.Builder chatClientBuilder;

    @Bean
    public ChatClient chatClient() {
        return chatClientBuilder.build();
    }
}
