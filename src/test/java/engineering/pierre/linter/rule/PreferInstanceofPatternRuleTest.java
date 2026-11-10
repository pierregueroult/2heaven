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

final class PreferInstanceofPatternRuleTest {

    private final Path tempDir;
    private final PreferInstanceofPatternRule rule;

    PreferInstanceofPatternRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new PreferInstanceofPatternRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void instanceofPlusManualCastIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    int length(Object o) {
                        if (o instanceof String) {
                            String s = (String) o;
                            return s.length();
                        }
                        return 0;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("prefer-instanceof-pattern", violations.getFirst().ruleName());
    }

    @Test
    void instanceofPatternIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    int length(Object o) {
                        if (o instanceof String s) {
                            return s.length();
                        }
                        return 0;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
