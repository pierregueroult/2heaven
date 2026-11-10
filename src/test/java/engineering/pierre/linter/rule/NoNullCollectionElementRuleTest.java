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

final class NoNullCollectionElementRuleTest {

    private final Path tempDir;
    private final NoNullCollectionElementRule rule;

    NoNullCollectionElementRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoNullCollectionElementRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void addNullIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    void run(List<String> list) {
                        list.add(null);
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-null-collection-element", violations.getFirst().ruleName());
    }

    @Test
    void addNonNullIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    void run(List<String> list) {
                        list.add("x");
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
