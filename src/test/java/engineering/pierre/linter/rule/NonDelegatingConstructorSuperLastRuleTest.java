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

final class NonDelegatingConstructorSuperLastRuleTest {

    private final Path tempDir;
    private final NonDelegatingConstructorSuperLastRule rule;

    NonDelegatingConstructorSuperLastRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NonDelegatingConstructorSuperLastRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void constructorEndingWithSuperIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    Sample() {
                        int x = 1;
                        super();
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void superWithArgumentsAsLastStatementIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample extends Base {
                    Sample(String name) {
                        System.identityHashCode(name);
                        super(name);
                    }
                }
                class Base {
                    Base(String name) {
                        super();
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void superNotLastIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    Sample() {
                        super();
                        int x = 1;
                    }
                }
                """);

        assertEquals(1, violations.size());
        LintViolation violation = violations.getFirst();
        assertEquals("non-delegating-constructor-super-last", violation.ruleName());
        assertEquals(2, violation.line());
        assertEquals(5, violation.column());
    }

    @Test
    void emptyConstructorIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    Sample() {
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void constructorEndingWithOtherStatementIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    Sample() {
                        int x = 1;
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void delegatingConstructorIsSkipped() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    Sample() {
                        this(1);
                    }

                    Sample(int x) {
                        super();
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void onlyTheViolatingConstructorIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    Sample() {
                        this(1);
                    }

                    Sample(int x) {
                        int y = x;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals(6, violations.getFirst().line());
    }

    @Test
    void enumConstructorsAreExempt() throws IOException {
        List<LintViolation> violations = check("""
                enum Color {
                    RED, GREEN;

                    Color() {
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void recordConstructorsAreExempt() throws IOException {
        List<LintViolation> violations = check("""
                record Point(int x, int y) {
                    Point {
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void constructorsInNestedClassesAreChecked() throws IOException {
        List<LintViolation> violations = check("""
                class Outer {
                    static class Inner {
                        Inner() {
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals(3, violations.getFirst().line());
    }
}
