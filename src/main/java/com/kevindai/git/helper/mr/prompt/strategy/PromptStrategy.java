package com.kevindai.git.helper.mr.prompt.strategy;

/**
 * Strategy for selecting a system prompt based on MR context.
 */
public interface PromptStrategy {

    /**
     * Unique identifier for the strategy (e.g., "generic", "java", "python", "javascript").
     */
    String id();

    /**
     * Compute a relevance score for this strategy given the MR context.
     * Higher score means the strategy is a better fit.
     */
    double score(MrContext context);

    /**
     * Return the system prompt to use when this strategy is selected.
     */
    String systemPrompt(MrContext context);

    /**
     * Whether this strategy supports hybrid composition with another strategy.
     */
    default boolean supportsHybrid() { return false; }
}

