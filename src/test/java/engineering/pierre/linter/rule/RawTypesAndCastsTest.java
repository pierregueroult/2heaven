package engineering.pierre.linter.rule;

import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.SourceParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for no-raw-types and no-unchecked-cast.
class RawTypesAndCastsTest {

    @TempDir
    Path tempDir;

    private List<LintViolation> check(Rule rule, String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void rawVariableDeclarationIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoRawTypesRule(), """
                import java.util.List;

                class Sample {
                    List names;
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-raw-types", violations.getFirst().ruleName());
        assertEquals(4, violations.getFirst().line());
    }

    @Test
    void rawConstructorAndRawCastAreFlagged() throws IOException {
        List<LintViolation> violations = check(new NoRawTypesRule(), """
                import java.util.ArrayList;
                import java.util.List;

                class Sample {
                    void run(Object o) {
                        List list = new ArrayList();
                        List other = (List) o;
                    }
                }
                """);

        assertEquals(4, violations.size());
    }

    @Test
    void parameterizedTypesAreAccepted() throws IOException {
        List<LintViolation> violations = check(new NoRawTypesRule(), """
                import java.util.ArrayList;
                import java.util.List;

                class Sample {
                    List<String> run(Object o) {
                        List<String> list = new ArrayList<String>();
                        return list;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void nonGenericRawTypesAreAccepted() throws IOException {
        List<LintViolation> violations = check(new NoRawTypesRule(), """
                class Sample {
                    String name;
                    Object run() {
                        return new Object();
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void castToParameterizedTypeIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoUncheckedCastRule(), """
                import java.util.List;

                class Sample {
                    void run(Object o) {
                        List<String> list = (List<String>) o;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-unchecked-cast", violations.getFirst().ruleName());
    }

    @Test
    void plainCastsAreAccepted() throws IOException {
        List<LintViolation> violations = check(new NoUncheckedCastRule(), """
                class Sample {
                    void run(Object o) {
                        String s = (String) o;
                        long n = (long) 42;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
