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

/// Tests for prefer-final-class.
final class PreferFinalClassRuleTest {

    private final Path tempDir;

    PreferFinalClassRuleTest() throws IOException {
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
    void nonFinalClassIsFlagged() throws IOException {
        List<LintViolation> violations = check(new PreferFinalClassRule(), """
                class Service {
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("prefer-final-class", violations.getFirst().ruleName());
        assertTrue(violations.getFirst().message().contains("Service"));
        assertEquals(1, violations.getFirst().line());
    }

    @Test
    void finalClassIsAccepted() throws IOException {
        List<LintViolation> violations = check(new PreferFinalClassRule(), """
                public final class Service {
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void nestedAndLocalClassesAreFlagged() throws IOException {
        List<LintViolation> violations = check(new PreferFinalClassRule(), """
                class Outer {
                    static class Inner {
                    }

                    void run() {
                        class Local {
                        }
                    }
                }
                """);

        assertEquals(3, violations.size());
    }

    @Test
    void abstractClassIsExempt() throws IOException {
        List<LintViolation> violations = check(new PreferFinalClassRule(), """
                abstract class Base {
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void sealedAndNonSealedClassesAreExempt() throws IOException {
        List<LintViolation> violations = check(new PreferFinalClassRule(), """
                sealed class Shape permits Circle {
                }

                non-sealed class AbstractShape extends Shape {
                }

                final class Circle extends Shape {
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void classExtendedInSameFileIsExemptButSubclassMustBeFinal() throws IOException {
        List<LintViolation> violations = check(new PreferFinalClassRule(), """
                class Base {
                }

                class Child extends Base {
                }
                """);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("Child"));
    }

    @Test
    void enumsRecordsInterfacesAndAnnotationsAreExempt() throws IOException {
        List<LintViolation> violations = check(new PreferFinalClassRule(), """
                enum Color {
                    RED, GREEN
                }

                record Point(int x, int y) {
                }

                interface Service {
                }

                @interface Marker {
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void anonymousClassIsExempt() throws IOException {
        List<LintViolation> violations = check(new PreferFinalClassRule(), """
                final class Sample {
                    Runnable run() {
                        return new Runnable() {
                            @Override
                            public void run() {
                            }
                        };
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
