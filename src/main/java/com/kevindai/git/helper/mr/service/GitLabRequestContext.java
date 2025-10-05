package com.kevindai.git.helper.mr.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Getter
@Setter
@Component
@RequestScope
public class GitLabRequestContext {
    private String groupFullPath;
    private String token;
}

