package com.kevindai.git.helper.mr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LineLocatorService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a tool that parses unified diffs.
            Task: Return the **line number** (1-based) in the *new file (after the patch)* where the target string appears.
            
            Rules (must follow):
            1) Count line numbers only for the new file: in the hunk header @@ -l1,s1 +l2,s2 @@, +l2 is the starting line number in the new file.
            2) When scanning the hunk line by line:
               - Start counting from +l2 for the first displayed line;
               - Lines starting with a single space (context) and lines starting with '+' (added) **are counted** toward the new-file line numbers (increment by 1 per such line);
               - Lines starting with '-' (deleted) are **not counted** (line number does not change).
            3) The target text appears exactly once within the hunk; as soon as you match it, return that line number.
            4) Output only a single integer; do **not** include any other words or explanations.
            
            Example:
            <<<TARGET_START>>>
            this is some for testing; what's the
            <<<TARGET_END>>>
            
            Unified diff:
            <<<DIFF_START>>>
            @@ -10,9 +10,9 @@ import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
             public class WebConfig implements WebMvcConfigurer {
                 private final TenantInterceptor tenantInterceptor;
            
            -
                 @Override
                 public void addInterceptors(InterceptorRegistry registry) {
                     registry.addInterceptor(tenantInterceptor);
                 }
            +    this is some for testing; what's the
             }
            <<<DIFF_END>>>
            
            Output: 17
            """;


    public int locateNewLine(String evidenceText, String unifiedDiff) {
        String target = StringUtils.hasText(evidenceText) ? evidenceText : "";
        if (!StringUtils.hasText(unifiedDiff)) {
            return -1;
        }
        String userMessage = "Target text:\n<<<TARGET_START>>>\n" + target + "\n<<<TARGET_END>>>\n\n" +
                "Unified diff:\n<<<DIFF_START>>>\n" + unifiedDiff + "\n<<<DIFF_END>>>\n\n";

        String text = chatClient.prompt(SYSTEM_PROMPT).user(userMessage).call().content();
        int line = parseFirstInteger(text);
        log.debug("Line locator raw='{}' parsedLine={}", text, line);
        return line;
    }

    private static int parseFirstInteger(String s) {
        if (s == null) return -1;
        Matcher m = Pattern.compile("-?\\d+").matcher(s);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group());
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }
}

