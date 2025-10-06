package com.kevindai.git.helper.mr.service;

import org.springframework.stereotype.Component;

@Component
public class GitLabRequestContext {

    private final InheritableThreadLocal<String> groupFullPath = new InheritableThreadLocal<>();
    private final InheritableThreadLocal<String> token = new InheritableThreadLocal<>();

    public void setGroupFullPath(String value) {
        groupFullPath.set(value);
    }

    public String getGroupFullPath() {
        return groupFullPath.get();
    }

    public void setToken(String value) {
        token.set(value);
    }

    public String getToken() {
        return token.get();
    }

    public void clear() {
        groupFullPath.remove();
        token.remove();
    }
}

