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

/// Tests for no-string-concat-in-loop and prefer-try-with-resources.
final class LoopAndResourcesRulesTest {

    private final Path tempDir;

    LoopAndResourcesRulesTest() throws IOException {
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
    void stringConcatPlusEqualsInForLoopIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoStringConcatInLoopRule(), """
                class Sample {
                    String run(String[] parts) {
                        String result = "";
                        for (String part : parts) {
                            result += part;
                        }
                        return result;
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-string-concat-in-loop", violations.getFirst().ruleName());
        assertEquals(5, violations.getFirst().line());
    }

    @Test
    void stringConcatWithPlusAssignInWhileLoopIsFlagged() throws IOException {
        List<LintViolation> violations = check(new NoStringConcatInLoopRule(), """
                class Sample {
                    String run(int n) {
                        String result = "";
                        int i = 0;
                        while (i < n) {
                            result = result + i;
                            i++;
                        }
                        return result;
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void numericConcatInLoopIsAccepted() throws IOException {
        List<LintViolation> violations = check(new NoStringConcatInLoopRule(), """
                class Sample {
                    int run(int n) {
                        int total = 0;
                        for (int i = 0; i < n; i++) {
                            total += i;
                        }
                        return total;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void stringConcatOutsideLoopIsAccepted() throws IOException {
        List<LintViolation> violations = check(new NoStringConcatInLoopRule(), """
                class Sample {
                    String run(String a, String b) {
                        String result = a + b;
                        return result;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void closeInFinallyIsFlagged() throws IOException {
        List<LintViolation> violations = check(new PreferTryWithResourcesRule(), """
                import java.io.FileReader;

                class Sample {
                    void run(String path) throws Exception {
                        FileReader reader = new FileReader(path);
                        try {
                            reader.read();
                        } finally {
                            reader.close();
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("prefer-try-with-resources", violations.getFirst().ruleName());
        assertEquals(9, violations.getFirst().line());
    }

    @Test
    void tryWithResourcesIsAccepted() throws IOException {
        List<LintViolation> violations = check(new PreferTryWithResourcesRule(), """
                import java.io.FileReader;

                class Sample {
                    void run(String path) throws Exception {
                        try (FileReader reader = new FileReader(path)) {
                            reader.read();
                        }
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
