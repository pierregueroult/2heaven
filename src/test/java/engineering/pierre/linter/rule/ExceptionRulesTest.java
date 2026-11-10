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

/// Tests for the exception-related rules: no-empty-catch,
/// no-catch-generic-exception, no-print-stack-trace,
/// no-swallow-interrupted-exception.
class ExceptionRulesTest {

    @TempDir
    Path tempDir;

    private List<LintViolation> check(Rule rule, String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void emptyCatchIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoEmptyCatchRule(), """
                class Sample {
                    void run() {
                        try {
                            risky();
                        } catch (java.io.IOException e) {
                        }
                    }
                    void risky() throws java.io.IOException {
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-empty-catch", violations.getFirst().ruleName());
    }

    @Test
    void nonEmptyCatchIsAccepted() throws IOException {
        List<LintViolation> violations = check(new NoEmptyCatchRule(), """
                class Sample {
                    void run() {
                        try {
                            risky();
                        } catch (java.io.IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    void risky() throws java.io.IOException {
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void catchingGenericExceptionIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoCatchGenericExceptionRule(), """
                class Sample {
                    void run() {
                        try {
                            risky();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        } catch (RuntimeException e) {
                            throw e;
                        }
                    }
                    void risky() {
                    }
                }
                """);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().allMatch(v -> v.ruleName().equals("no-catch-generic-exception")));
    }

    @Test
    void catchingSpecificExceptionIsAccepted() throws IOException {
        List<LintViolation> violations = check(new NoCatchGenericExceptionRule(), """
                class Sample {
                    void run() {
                        try {
                            risky();
                        } catch (java.io.IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    void risky() throws java.io.IOException {
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void printStackTraceIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoPrintStackTraceRule(), """
                class Sample {
                    void run() {
                        try {
                            risky();
                        } catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
                    }
                    void risky() throws java.io.IOException {
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-print-stack-trace", violations.getFirst().ruleName());
        assertEquals(6, violations.getFirst().line());
    }

    @Test
    void swallowedInterruptedExceptionIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoSwallowInterruptedExceptionRule(), """
                class Sample {
                    void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-swallow-interrupted-exception", violations.getFirst().ruleName());
    }

    @Test
    void rethrownOrInterruptedInterruptedExceptionIsAccepted() throws IOException {
        List<LintViolation> violations = check(new NoSwallowInterruptedExceptionRule(), """
                class Sample {
                    void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
