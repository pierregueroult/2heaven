package engineering.pierre.linter.rule;

import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.SourceParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NoStreamJustForForEachRuleTest {

    private final Path tempDir;
    private final NoStreamJustForForEachRule rule;

    NoStreamJustForForEachRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoStreamJustForForEachRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void streamThenForEachIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    void run(List<String> list) {
                        list.stream().forEach(s -> { });
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-stream-just-for-foreach", violations.getFirst().ruleName());
    }

    @Test
    void directForEachIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    void run(List<String> list) {
                        list.forEach(s -> { });
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
