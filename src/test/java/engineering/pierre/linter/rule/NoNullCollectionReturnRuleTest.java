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

final class NoNullCollectionReturnRuleTest {

    private final Path tempDir;
    private final NoNullCollectionReturnRule rule;

    NoNullCollectionReturnRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoNullCollectionReturnRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void nullReturnOfListIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    List<String> find(boolean ok) {
                        if (!ok) {
                            return null;
                        }
                        return List.of();
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-null-collection-return", violations.getFirst().ruleName());
    }

    @Test
    void emptyCollectionReturnIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    List<String> find(boolean ok) {
                        return ok ? List.of("x") : List.of();
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void nullReturnOfStringIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    String find(boolean ok) {
                        return ok ? "x" : null;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
