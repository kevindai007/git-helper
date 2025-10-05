package com.kevindai.git.helper.mr.service;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Factory to create RestClient instances bound with a specific GitLab token.
 */
@Component
public class GitLabClientFactory {

    public RestClient forToken(String token) {
        // Use a builder to set a default header for PRIVATE-TOKEN
        return RestClient.builder()
                .defaultHeader("PRIVATE-TOKEN", token == null ? "" : token)
                // Use the JDK client to avoid extra deps; can be swapped/configured later
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
    }
}

