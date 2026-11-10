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

class RequireNonNullObjectParamRuleTest {

    private final Path tempDir;
    private final RequireNonNullObjectParamRule rule;

    RequireNonNullObjectParamRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new RequireNonNullObjectParamRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void guardedObjectParamIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.Objects;

                public class Sample {
                    public void run(Object input) {
                        Objects.requireNonNull(input);
                        System.identityHashCode(input);
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void unguardedObjectParamIsFlaggedAtParameterPosition() throws IOException {
        List<LintViolation> violations = check("""
                public class Sample {
                    public void run(Object input) {
                        System.identityHashCode(input);
                    }
                }
                """);

        assertEquals(1, violations.size());
        LintViolation violation = violations.getFirst();
        assertEquals("require-non-null-object-param", violation.ruleName());
        assertEquals(2, violation.line());
        assertEquals(21, violation.column());
        assertTrue(violation.message().contains("input"));
    }

    @Test
    void nonPublicMethodsAreIgnored() throws IOException {
        List<LintViolation> violations = check("""
                public class Sample {
                    void run(Object input) {
                    }

                    private void other(Object input) {
                    }

                    protected void third(Object input) {
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void nonObjectParamsAreIgnored() throws IOException {
        List<LintViolation> violations = check("""
                public class Sample {
                    public void run(String input, int count) {
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void eachUnguardedObjectParamIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.Objects;

                public class Sample {
                    public void run(Object first, Object second, Object third) {
                        Objects.requireNonNull(second);
                    }
                }
                """);

        assertEquals(2, violations.size());
        assertEquals(List.of(4, 4), violations.stream().map(LintViolation::line).toList());
    }

    @Test
    void requireNonNullWithMessageStillCounts() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.Objects;

                public class Sample {
                    public void run(Object input) {
                        Objects.requireNonNull(input, "input must not be null");
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void requireNonNullOnAnotherVariableDoesNotCount() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.Objects;

                public class Sample {
                    public void run(Object input) {
                        Object other = new Object();
                        Objects.requireNonNull(other);
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void fullyQualifiedJavaLangObjectIsDetected() throws IOException {
        List<LintViolation> violations = check("""
                public class Sample {
                    public void run(java.lang.Object input) {
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void publicMethodsInNestedClassesAreChecked() throws IOException {
        List<LintViolation> violations = check("""
                public class Sample {
                    public static class Inner {
                        public void run(Object input) {
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void interfaceAndAbstractMethodsWithoutBodyAreSkipped() throws IOException {
        List<LintViolation> violations = check("""
                public interface Sample {
                    void run(Object input);
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
