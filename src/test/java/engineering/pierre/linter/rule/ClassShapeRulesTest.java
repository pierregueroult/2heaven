package engineering.pierre.linter.rule;

import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.SourceParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for equals-without-hashcode, no-anonymous-class-for-lambda and
/// no-mutable-static-field.
final class ClassShapeRulesTest {

    private final Path tempDir;

    ClassShapeRulesTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        super();
    }

    private List<LintViolation> check(Rule rule, String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void equalsWithoutHashCodeIsFlagged() throws IOException {
        List<LintViolation> violations = check(new EqualsWithoutHashCodeRule(), """
                class Point {
                    int x;

                    @Override
                    public boolean equals(Object o) {
                        return o instanceof Point p && p.x == x;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("equals-without-hashcode", violations.getFirst().ruleName());
        assertTrue(violations.getFirst().message().contains("hashCode"));
    }

    @Test
    void hashCodeWithoutEqualsIsFlagged() throws IOException {
        List<LintViolation> violations = check(new EqualsWithoutHashCodeRule(), """
                class Point {
                    int x;

                    @Override
                    public int hashCode() {
                        return x;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("equals"));
    }

    @Test
    void equalsAndHashCodeTogetherAreAccepted() throws IOException {
        List<LintViolation> violations = check(new EqualsWithoutHashCodeRule(), """
                class Point {
                    int x;

                    @Override
                    public boolean equals(Object o) {
                        return o instanceof Point p && p.x == x;
                    }

                    @Override
                    public int hashCode() {
                        return x;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void recordIsExempt() throws IOException {
        List<LintViolation> violations = check(new EqualsWithoutHashCodeRule(), """
                record Point(int x, int y) {
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void singleMethodAnonymousClassIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoAnonymousClassForLambdaRule(), """
                class Sample {
                    Runnable run() {
                        return new Runnable() {
                            @Override
                            public void run() {
                            }
                        };
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-anonymous-class-for-lambda", violations.getFirst().ruleName());
    }

    @Test
    void anonymousClassWithStateIsAccepted() throws IOException {
        List<LintViolation> violations = check(new NoAnonymousClassForLambdaRule(), """
                class Sample {
                    Object run() {
                        return new java.util.ArrayList<String>() {
                            int count;

                            @Override
                            public boolean add(String s) {
                                count++;
                                return super.add(s);
                            }
                        };
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void mutableStaticFieldIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoMutableStaticFieldRule(), """
                class Counter {
                    static int count;
                    static final int MAX = 10;
                    static volatile boolean running;
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-mutable-static-field", violations.getFirst().ruleName());
        assertTrue(violations.getFirst().message().contains("count"));
        assertEquals(2, violations.getFirst().line());
    }

    @Test
    void enumConstantsAreExempt() throws IOException {
        List<LintViolation> violations = check(new NoMutableStaticFieldRule(), """
                enum Color {
                    RED, GREEN, BLUE
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
