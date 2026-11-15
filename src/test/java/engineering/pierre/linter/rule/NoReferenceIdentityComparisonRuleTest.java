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

final class NoReferenceIdentityComparisonRuleTest {

    private final Path tempDir;
    private final NoReferenceIdentityComparisonRule rule;

    NoReferenceIdentityComparisonRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoReferenceIdentityComparisonRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void stringIdentityComparisonIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    boolean same(String a, String b) {
                        return a == b;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-reference-identity-comparison", violations.getFirst().ruleName());
        assertEquals(3, violations.getFirst().line());
    }

    @Test
    void stringLiteralComparisonIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    boolean check(String s) {
                        return s == "toto";
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void notEqualsOnObjectsIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    boolean diff(List<String> a, List<String> b) {
                        return a != b;
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void equalsComparisonIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    boolean same(String a, String b) {
                        return a.equals(b);
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void primitiveComparisonIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    boolean same(int a, int b) {
                        return a == b;
                    }

                    boolean boxed(int a, Integer b) {
                        return a == b; // unboxing comparison, not identity
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void nullCheckIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    boolean isNull(Object o) {
                        return o == null;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void enumComparisonIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    enum Color { RED, BLUE }

                    boolean isRed(Color c) {
                        return c == Color.RED;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void classTokensAndReflectiveTypesAreAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.lang.reflect.ParameterizedType;
                import java.lang.reflect.Type;
                import java.util.List;

                class Sample {
                    boolean matches(Type type, ParameterizedType parameterized) {
                        return type == String.class || int.class == type || type != double.class
                                || List.class != parameterized.getRawType();
                    }
                    boolean same(Class<?> left, Class<?> right) {
                        return left == right || left != right;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void userDefinedClassNamedClassIsStillFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    static class Class {}
                    boolean same(Class left, Class right) {
                        return left == right;
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

}
