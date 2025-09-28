package com.kevindai.git.helper.mr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmAnalysisService {

    private final ChatClient chatClient;
    public static final String SYSTEM_PROMPT = """
            You are a professional code review assistant, proficient in multiple programming languages and best practices.
            Your task is to help developers identify potential issues in code and provide improvement suggestions.
            Please ensure your feedback is specific and constructive to help improve code quality.
            Your should focus on understanding the purpose of the code changes and provide actionable insights.
            When analyzing code changes,
            Consider:
            1. Code quality and adherence to best practices
            2. Potential bugs or edge cases
            3. Performance optimizations
            4. Readability and maintainability
            5. Any security concerns
            Suggest improvements and explain your reasoning for each suggestion.
            Your response should focus on the most relevant and impactful feedback.
            Response Example:
            1. (High)Suggestion: Use a more efficient algorithm for sorting.
               Reasoning: The current implementation has a time complexity of O(n^2), which can be improved to O(n log n) using quicksort or mergesort.
            2. (High)Suggestion: Add error handling for null inputs.
               Reasoning: The current code does not handle null inputs, which could lead to runtime exceptions.
            3. (Medium)Suggestion: Refactor the function into smaller, reusable components.
               Reasoning: The current function is too long and complex, making it hard to read and maintain.
            4. (Low)Suggestion: Use parameterized queries to prevent SQL injection.
               Reasoning: The current implementation concatenates user input directly into SQL queries, which is a security risk.
           If the code changes are minimal or do not warrant any suggestions, respond with "No significant issues found."
            """;

    public String analyzeDiff(String formattedDiffContent) {
        return chatClient
                .prompt(SYSTEM_PROMPT)
                .user(formattedDiffContent)
                .call()
                .chatResponse()
                .getResults()
                .getFirst()
                .getOutput()
                .getText();
    }
}

