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

final class EnumSwitchMissingDefaultRuleTest {

    private final Path tempDir;
    private final EnumSwitchMissingDefaultRule rule;

    EnumSwitchMissingDefaultRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new EnumSwitchMissingDefaultRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void nonExhaustiveEnumSwitchWithoutDefaultIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    enum Color { RED, BLUE, GREEN }

                    int code(Color color) {
                        switch (color) {
                            case RED -> { return 1; }
                            case BLUE -> { return 2; }
                        }
                        return 0;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("enum-switch-missing-default", violations.getFirst().ruleName());
    }

    @Test
    void exhaustiveEnumSwitchIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    enum Color { RED, BLUE }

                    int code(Color color) {
                        switch (color) {
                            case RED -> { return 1; }
                            case BLUE -> { return 2; }
                        }
                        return 0;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void switchWithDefaultIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    enum Color { RED, BLUE }

                    int code(Color color) {
                        switch (color) {
                            case RED -> { return 1; }
                            default -> { return 0; }
                        }
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void switchOnNonEnumIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    int code(int x) {
                        switch (x) {
                            case 1 -> { return 1; }
                        }
                        return 0;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
