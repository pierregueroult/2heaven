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

final class NoPublicMutableFieldsRuleTest {

    private final Path tempDir;
    private final NoPublicMutableFieldsRule rule;

    NoPublicMutableFieldsRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoPublicMutableFieldsRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void publicMutableFieldIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    public int count;
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-public-mutable-fields", violations.getFirst().ruleName());
    }

    @Test
    void privateAndFinalFieldsAreAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    public static final int MAX = 10;
                    private int count;
                    public final String name = "x";
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
