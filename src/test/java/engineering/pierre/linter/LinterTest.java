package engineering.pierre.linter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class LinterTest {

    private final Path tempDir;

    LinterTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        super();
    }

    private Path writeSource(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }

    @Test
    void cleanFileProducesNoViolations() throws IOException {
        Path file = writeSource("Clean.java", """
                package demo;

                public final class Clean {
                    public static int add(int a, int b) {
                        return a + b;
                    }
                }
                """);

        List<LintViolation> violations = Linter.withBuiltInRules().lint(file);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void builtInRuleFlagsUnguardedObjectParam() throws IOException {
        Path file = writeSource("Dirty.java", """
                package demo;

                public final class Dirty {
                    public void run(Object input) {
                    }
                }
                """);

        List<LintViolation> violations = Linter.withBuiltInRules().lint(file);

        assertEquals(1, violations.size());
        assertEquals("require-non-null-object-param", violations.getFirst().ruleName());
    }

    @Test
    void customRuleCanBeAddedEasily() throws IOException {
        Path file = writeSource("Demo.java", """
                package demo;

                public final class Demo {
                    public static final int MAGIC = 42;
                }
                """);
        Rule noMagicNumbers = new Rule() {
            @Override
            public String name() {
                return "no-magic-numbers";
            }

            @Override
            public String description() {
                return "No literal numbers in code.";
            }

            @Override
            public List<LintViolation> check(LintContext context) {
                return context.lines().stream()
                        .filter(line -> line.contains("42"))
                        .map(line -> new LintViolation(context.path(), name(),
                                Severity.INFO, 1, 1, "Magic number found"))
                        .toList();
            }
        };

        List<LintViolation> violations = new Linter(List.of(noMagicNumbers)).lint(file);

        assertEquals(1, violations.size());
        assertEquals("no-magic-numbers", violations.getFirst().ruleName());
    }

    @Test
    void resultsAreSortedAcrossFiles() throws IOException {
        Path b = writeSource("B.java", """
                final class B {
                    public void run(Object input) {
                    }
                }
                """);
        Path a = writeSource("A.java", """
                final class A {
                    public void run(Object input) {
                    }
                }
                """);

        List<LintViolation> violations = Linter.withBuiltInRules().lint(List.of(b, a));

        assertEquals(2, violations.size());
        assertTrue(violations.getFirst().path().endsWith("A.java"));
        assertTrue(violations.getLast().path().endsWith("B.java"));
    }

    @Test
    void unparsableFileFailsExplicitly() throws IOException {
        Path file = writeSource("Broken.java", "}{\n");

        var error = assertThrows(Linter.LintException.class, () -> Linter.withBuiltInRules().lint(file));
        assertTrue(error.getMessage().contains("Broken.java:1:1"));
    }
}
