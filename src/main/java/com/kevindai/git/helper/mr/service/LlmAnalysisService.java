package com.kevindai.git.helper.mr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmAnalysisService {

    private final ChatClient chatClient;

    public String analyzeDiff(String formattedDiffContent) {
        String prompt = """
                请对以下 GitLab Merge Request 的代码改动进行专业的代码审查。
                找出潜在的 Bug、性能问题、安全漏洞、以及代码风格不规范的地方。
                请分文件列出发现的问题，并给出修改建议。

                --- 代码改动详情 ---
                """ + formattedDiffContent;

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .chatResponse()
                .getResults()
                .getFirst()
                .getOutput()
                .getText();
    }
}

