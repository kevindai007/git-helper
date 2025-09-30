package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import com.kevindai.git.helper.mr.prompt.GeneralBestPractice;
import com.kevindai.git.helper.mr.prompt.JavaBestPractice;
import com.kevindai.git.helper.mr.prompt.JavaScriptBestPractice;
import com.kevindai.git.helper.mr.prompt.PythonBestPractice;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LlmAnalysisService {

    private final ChatClient chatClient;


    public String analyzeDiff(String formattedDiffContent, List<MrDiff> diffs) {
        String prompt = selectPromptForFiles(diffs);
//        return chatClient
//                .prompt(GeneralBestPractice.SYSTEM_PROMPT)
//                .user(formattedDiffContent)
//                .call()
//                .chatResponse()
//                .getResults()
//                .getFirst()
//                .getOutput()
//                .getText();
        return "";
    }

    private String selectPromptForFiles(List<MrDiff> diffs) {
        if (diffs == null || diffs.isEmpty()) {
            return GeneralBestPractice.SYSTEM_PROMPT;
        }
        for (MrDiff d : diffs) {
            String path = d.getNew_path() != null ? d.getNew_path() : d.getOld_path();
            if (path == null) continue;
            String p = path.toLowerCase(Locale.ROOT);
            if (p.endsWith(".java")) {
                return JavaBestPractice.SYSTEM_PROMPT;
            } else if (p.endsWith(".py")) {
                return PythonBestPractice.SYSTEM_PROMPT;
            } else if (p.endsWith(".js") || p.endsWith(".mjs") || p.endsWith(".cjs") || p.endsWith(".jsx") || p.endsWith(".ts") || p.endsWith(".tsx")) {
                return JavaScriptBestPractice.SYSTEM_PROMPT;
            }
        }

        return GeneralBestPractice.SYSTEM_PROMPT;
    }
}
