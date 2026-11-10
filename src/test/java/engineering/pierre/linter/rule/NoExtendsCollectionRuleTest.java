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

final class NoExtendsCollectionRuleTest {

    private final Path tempDir;
    private final NoExtendsCollectionRule rule;

    NoExtendsCollectionRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoExtendsCollectionRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void extendingArrayListIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.ArrayList;

                class Sample extends ArrayList<String> {
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-extends-collection", violations.getFirst().ruleName());
    }

    @Test
    void compositionIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.ArrayList;
                import java.util.List;

                class Sample {
                    private final List<String> items = new ArrayList<>();
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
