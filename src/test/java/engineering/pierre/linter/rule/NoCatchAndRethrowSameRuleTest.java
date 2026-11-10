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

final class NoCatchAndRethrowSameRuleTest {

    private final Path tempDir;
    private final NoCatchAndRethrowSameRule rule;

    NoCatchAndRethrowSameRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoCatchAndRethrowSameRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void catchThatOnlyRethrowsIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.io.IOException;

                class Sample {
                    void run() throws IOException {
                        try {
                            System.currentTimeMillis();
                        } catch (IOException e) {
                            throw e;
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-catch-and-rethrow-same", violations.getFirst().ruleName());
    }

    @Test
    void catchThatHandlesIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.io.IOException;

                class Sample {
                    void run() {
                        try {
                            System.currentTimeMillis();
                        } catch (IOException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
