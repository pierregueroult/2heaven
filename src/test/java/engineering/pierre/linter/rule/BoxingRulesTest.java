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

/// Tests for no-boxed-long-or-int and no-stream-of-integer.
final class BoxingRulesTest {

    private final Path tempDir;

    BoxingRulesTest() throws IOException {
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
    void boxedTypesInDeclarationsAreFlagged() throws IOException {
        List<LintViolation> violations = check(new NoBoxedLongOrIntRule(), """
                class Sample {
                    Integer count;
                    Long total;

                    Long sum(Integer a, long b) {
                        int local = 0;
                        Integer other = a;
                        return total;
                    }
                }
                """);

        // count, total, return type Long, parameter Integer, local Integer
        assertEquals(5, violations.size());
        for (LintViolation violation : violations) {
            assertEquals("no-boxed-long-or-int", violation.ruleName());
        }
    }

    @Test
    void qualifiedBoxedTypesAndCastAndConstructorAreFlagged() throws IOException {
        List<LintViolation> violations = check(new NoBoxedLongOrIntRule(), """
                class Sample {
                    void run(Object o) {
                        java.lang.Integer a = (java.lang.Integer) o;
                        Long b = new Long(42L);
                        Integer[] c = new Integer[3];
                    }
                }
                """);

        // declaration, cast, declaration, constructor, array element type
        assertEquals(5, violations.size());
    }

    @Test
    void primitivesAreAccepted() throws IOException {
        List<LintViolation> violations = check(new NoBoxedLongOrIntRule(), """
                class Sample {
                    int count;
                    long total;

                    long sum(int a, long b) {
                        long local = a + b;
                        int[] values = new int[3];
                        return local;
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void boxedTypeInsideOtherGenericsIsNotFlaggedByThisRule() throws IOException {
        List<LintViolation> violations = check(new NoBoxedLongOrIntRule(), """
                import java.util.List;

                class Sample {
                    List<Integer> counts;
                    List<Long> totals;
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void streamOfIntegerIsNotFlaggedByBoxedRule() throws IOException {
        List<LintViolation> violations = check(new NoBoxedLongOrIntRule(), """
                import java.util.stream.Stream;

                class Sample {
                    Stream<Integer> numbers;
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void streamOfIntegerIsFlaggedWhereverWritten() throws IOException {
        List<LintViolation> violations = check(new NoStreamOfIntegerRule(), """
                import java.util.stream.Stream;

                class Sample {
                    Stream<Integer> numbers;

                    Stream<Integer> all(java.util.List<Integer> source) {
                        Stream<Integer> intermediate = source.stream();
                        Stream<Integer> cast = (Stream<Integer>) null;
                        return intermediate;
                    }
                }
                """);

        // field, return type, two declarations, and the cast
        assertEquals(5, violations.size());
        for (LintViolation violation : violations) {
            assertEquals("no-stream-of-integer", violation.ruleName());
        }
    }

    @Test
    void streamOfIntegerAsIntermediateStepAndTypeWitnessAreFlagged() throws IOException {
        List<LintViolation> violations = check(new NoStreamOfIntegerRule(), """
                import java.util.List;
                import java.util.stream.Stream;

                class Sample {
                    void run(List<Integer> source) {
                        var boxed = Stream.<Integer>of(1, 2, 3);
                        List<Stream<Integer>> nested = List.of(source.stream());
                    }
                }
                """);

        assertEquals(2, violations.size());
    }

    @Test
    void otherStreamsAndIntStreamAreAccepted() throws IOException {
        List<LintViolation> violations = check(new NoStreamOfIntegerRule(), """
                import java.util.List;
                import java.util.stream.IntStream;
                import java.util.stream.Stream;

                class Sample {
                    Stream<String> names;
                    IntStream numbers;

                    IntStream run(List<String> source) {
                        Stream.<String>of("a", "b");
                        return IntStream.range(0, 10);
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
