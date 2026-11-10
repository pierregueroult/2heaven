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

final class EqualsOverloadNotOverrideRuleTest {

    private final Path tempDir;
    private final EqualsOverloadNotOverrideRule rule;

    EqualsOverloadNotOverrideRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new EqualsOverloadNotOverrideRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void equalsWithSpecificTypeIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    boolean equals(Sample other) {
                        return true;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("equals-overload-not-override", violations.getFirst().ruleName());
    }

    @Test
    void equalsObjectIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    @Override
                    public boolean equals(Object o) {
                        return o instanceof Sample;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
