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

final class NoEnumValuesInLoopRuleTest {

    private final Path tempDir;
    private final NoEnumValuesInLoopRule rule;

    NoEnumValuesInLoopRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoEnumValuesInLoopRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void valuesInLoopIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    enum Color { RED, BLUE }

                    int count() {
                        int n = 0;
                        for (Color c : Color.values()) {
                            n++;
                        }
                        return n;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-enum-values-in-loop", violations.getFirst().ruleName());
    }

    @Test
    void hoistedValuesIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    enum Color { RED, BLUE }

                    int count() {
                        Color[] colors = Color.values();
                        int n = 0;
                        for (Color c : colors) {
                            n++;
                        }
                        return n;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
