package com.kevindai.git.helper.config;

import com.kevindai.git.helper.mr.service.GitLabRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientTokenConfig {

    private final GitLabRequestContext requestContext;
    private final GitConfig gitConfig;
    public static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";

    @Bean
    public RestClientCustomizer tokenInjectingRestClientCustomizer() {
        return builder -> builder.requestInterceptor(tokenInterceptor());
    }

    private ClientHttpRequestInterceptor tokenInterceptor() {
        return (request, body, execution) -> {
            String token = requestContext.getToken();
            if (token == null || token.isBlank()) {
                token = gitConfig.getToken();
            }
            if (token != null && !token.isBlank()) {
                request.getHeaders().set(PRIVATE_TOKEN, token);
            }
            return execution.execute(request, body);
        };
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        // The builder will be customized by RestClientCustomizer above to inject token header
        return builder.build();
    }
}
