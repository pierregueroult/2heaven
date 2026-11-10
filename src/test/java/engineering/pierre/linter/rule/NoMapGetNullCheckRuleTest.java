package engineering.pierre.linter.rule;

import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.SourceParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NoMapGetNullCheckRuleTest {

    private final Path tempDir;
    private final NoMapGetNullCheckRule rule;

    NoMapGetNullCheckRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoMapGetNullCheckRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void getComparedToNullIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.Map;

                class Sample {
                    boolean has(Map<String, Integer> map, String key) {
                        return map.get(key) != null;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-map-get-null-check", violations.getFirst().ruleName());
    }

    @Test
    void getOrDefaultIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.Map;

                class Sample {
                    int count(Map<String, Integer> map, String key) {
                        return map.getOrDefault(key, 0);
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
