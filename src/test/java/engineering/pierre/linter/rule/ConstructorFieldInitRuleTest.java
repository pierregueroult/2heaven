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

class ConstructorFieldInitRuleTest {

    private final Path tempDir;
    private final ConstructorFieldInitRule rule;

    ConstructorFieldInitRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new ConstructorFieldInitRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void inlineInitializerIsFlaggedEvenForPrimitives() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    int count = 5;
                    String name = "x";

                    Sample() {
                    }
                }
                """);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().allMatch(v -> v.ruleName().equals("constructor-field-init")));
        assertTrue(violations.stream().anyMatch(v -> v.message().contains("count")));
        assertTrue(violations.stream().anyMatch(v -> v.message().contains("name")));
    }

    @Test
    void primitiveFieldWithDefaultIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    int count;
                    boolean ready;

                    Sample() {
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void referenceFieldAssignedInConstructorIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    String name;

                    Sample(String name) {
                        this.name = name;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void referenceFieldAssignedWithoutThisIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    String name;

                    Sample(String name) {
                        this.name = name;
                    }

                    Sample() {
                        name = "default";
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void referenceFieldNeverAssignedIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    String name;
                    int count;

                    Sample() {
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("name"));
        assertEquals(2, violations.getFirst().line());
    }

    @Test
    void staticFieldsAreExempt() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    static int count = 5;
                    static final String NAME = "x";
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void recordsAreExempt() throws IOException {
        List<LintViolation> violations = check("""
                record Point(int x, int y) {
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void nestedClassFieldsAreChecked() throws IOException {
        List<LintViolation> violations = check("""
                class Outer {
                    String outer;

                    static class Inner {
                        String inner = "bad";
                    }
                }
                """);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.message().contains("outer")));
        assertTrue(violations.stream().anyMatch(v -> v.message().contains("inner")));
    }
}
