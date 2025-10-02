package com.kevindai.git.helper.mr.prompt;

import org.springframework.stereotype.Component;

@Component
public class DefaultPromptProvider implements PromptProvider {
    @Override
    public String get(PromptType type) {
        return switch (type) {
            case JAVA -> JavaBestPractice.SYSTEM_PROMPT;
            case PYTHON -> PythonBestPractice.SYSTEM_PROMPT;
            case JAVASCRIPT -> JavaScriptBestPractice.SYSTEM_PROMPT;
            case GENERIC -> GeneralBestPractice.SYSTEM_PROMPT;
        };
    }
}

