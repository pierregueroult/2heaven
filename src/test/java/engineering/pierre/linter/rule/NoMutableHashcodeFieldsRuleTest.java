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

final class NoMutableHashcodeFieldsRuleTest {

    private final Path tempDir;
    private final NoMutableHashcodeFieldsRule rule;

    NoMutableHashcodeFieldsRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoMutableHashcodeFieldsRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void mutableFieldInHashcodeClassIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    int age;

                    @Override
                    public int hashCode() {
                        return age;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-mutable-hashcode-fields", violations.getFirst().ruleName());
    }

    @Test
    void finalFieldsInHashcodeClassAreAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    final int age;

                    Sample(int age) {
                        this.age = age;
                        super();
                    }

                    @Override
                    public int hashCode() {
                        return age;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void mutableFieldWithoutHashcodeIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    int age;
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
