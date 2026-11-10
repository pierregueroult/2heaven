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

final class PreferDefensiveCopyRuleTest {

    private final Path tempDir;
    private final PreferDefensiveCopyRule rule;

    PreferDefensiveCopyRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new PreferDefensiveCopyRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void returningMutableFieldIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.ArrayList;
                import java.util.List;

                class Sample {
                    private final List<String> books = new ArrayList<>();

                    List<String> all() {
                        return books;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("prefer-defensive-copy", violations.getFirst().ruleName());
    }

    @Test
    void returningCopyIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.ArrayList;
                import java.util.List;

                class Sample {
                    private final List<String> books = new ArrayList<>();

                    List<String> all() {
                        return List.copyOf(books);
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
