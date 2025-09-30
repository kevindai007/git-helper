package com.kevindai.git.helper.mr.prompt.strategy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "prompt.selection")
public class PromptSelectionProperties {
    /**
     * Weight applied per file of a given extension.
     */
    private double fileCountWeight = 20.0;
    /**
     * Weight applied per changed line (added + removed).
     */
    private double lineWeight = 1.0;
}

