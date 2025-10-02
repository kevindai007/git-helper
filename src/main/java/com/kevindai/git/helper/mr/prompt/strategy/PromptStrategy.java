package com.kevindai.git.helper.mr.prompt.strategy;

import com.kevindai.git.helper.mr.prompt.PromptType;

/**
 * Strategy to compute a relevance score for a prompt type based on MR context.
 */
public interface PromptStrategy {

    /**
     * The prompt type/category this strategy represents.
     */
    PromptType type();

    /**
     * Compute a relevance score for this type given the MR context.
     * Higher score means the type is a better fit.
     */
    double score(MrContext context);

    /**
     * Whether this strategy supports hybrid composition with another strategy.
     */
    default boolean supportsHybrid() { return false; }
}
