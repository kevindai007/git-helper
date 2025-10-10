package com.kevindai.git.helper.mr.util;

import com.kevindai.git.helper.mr.dto.ParsedMrUrl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GitLabUrlParserTest {

    private final GitLabUrlParser parser = new GitLabUrlParser();

    @Test
    void parseExistingMrUrl_withNestedGroup() {
        String url = "https://gitlab.com/group/subgroup/my-proj/-/merge_requests/42";
        ParsedMrUrl p = parser.parseExistingMrUrl(url);
        assertEquals("group/subgroup", p.getGroupFullPath());
        assertEquals("my-proj", p.getProjectPath());
        assertEquals(42, p.getMrId());
        // backward-compat field points to same value
        assertEquals("group/subgroup", p.getProjectFullPath());
    }

    @Test
    void parseNewMrUrl_valid() {
        String url = "https://gitlab.com/g1/g2/p1/-/merge_requests/new?merge_request[source_project_id]=123&merge_request[source_branch]=feat&merge_request[target_branch]=main";
        GitLabUrlParser.NewMrContext ctx = parser.parseNewMrUrl(url);
        assertEquals(123L, ctx.projectId());
        assertEquals("feat", ctx.sourceBranch());
        assertEquals("main", ctx.targetBranch());
        assertEquals("g1/g2", ctx.groupFullPath());
    }

    @Test
    void parseExistingMrUrl_invalid_throws() {
        String url = "https://gitlab.com/group/subgroup/my-proj/merge_requests/42"; // missing "-/"
        assertThrows(IllegalArgumentException.class, () -> parser.parseExistingMrUrl(url));
    }
}
