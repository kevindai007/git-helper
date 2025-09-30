package com.kevindai.git.helper.mr.prompt.strategy;

import com.kevindai.git.helper.mr.dto.gitlab.MrDiff;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrategySelectionTest {

    private ScoreCalculator newCalculator() {
        PromptSelectionProperties props = new PromptSelectionProperties();
        return new ScoreCalculator(props);
    }

    private static MrDiff newDiff(String path, int added, int removed) {
        MrDiff d = new MrDiff();
        d.setNew_path(path);
        StringBuilder sb = new StringBuilder();
        sb.append("@@ -1,1 +1,1 @@\n");
        for (int i = 0; i < added; i++) sb.append("+line\n");
        for (int i = 0; i < removed; i++) sb.append("-line\n");
        d.setDiff(sb.toString());
        return d;
    }

    @Test
    void javaBeatsJavascriptWhenDominant() {
        MrContext ctx = new MrContext(List.of(
                newDiff("src/Main.java", 50, 10),
                newDiff("src/Service.java", 30, 5),
                newDiff("web/app.js", 5, 2)
        ));

        ScoreCalculator calc = newCalculator();
        JavaStrategy java = new JavaStrategy(calc);
        JavaScriptStrategy js = new JavaScriptStrategy(calc);
        GenericStrategy generic = new GenericStrategy();

        double sJava = java.score(ctx);
        double sJs = js.score(ctx);
        double sGen = generic.score(ctx);

        assertTrue(sJava > sJs, "Java score should be higher than JS");
        assertTrue(sJava > sGen, "Java score should beat generic fallback");
    }

    @Test
    void fallbackToGenericWhenUnknownExt() {
        MrContext ctx = new MrContext(List.of(
                newDiff("README.md", 10, 2),
                newDiff("docs/guide.adoc", 5, 1)
        ));

        ScoreCalculator calc = newCalculator();
        JavaStrategy java = new JavaStrategy(calc);
        PythonStrategy py = new PythonStrategy(calc);
        JavaScriptStrategy js = new JavaScriptStrategy(calc);
        GenericStrategy generic = new GenericStrategy();

        assertEquals(0.0, java.score(ctx));
        assertEquals(0.0, py.score(ctx));
        assertEquals(0.0, js.score(ctx));
        assertTrue(generic.score(ctx) > 0.0, "Generic should have minimal positive score");
    }
}
