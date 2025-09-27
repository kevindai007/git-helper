package com.kevindai.git.helper.confg;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "gitlab")
public class GitConfig {
    private String url;
    private String token;

}
