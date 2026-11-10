package engineering.pierre.linter.rule;

import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.SourceParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoStreamSideEffectsRuleTest {

    @TempDir
    Path tempDir;

    private final NoStreamSideEffectsRule rule = new NoStreamSideEffectsRule();

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void forEachOnStreamIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    void run(List<String> list) {
                        list.stream().forEach(System.out::println);
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-stream-side-effects", violations.getFirst().ruleName());
        assertEquals(5, violations.getFirst().line());
    }

    @Test
    void forEachOnParallelStreamIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    void run(List<String> list) {
                        list.parallelStream().forEachOrdered(x -> System.out.println(x));
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("forEachOrdered"));
    }

    @Test
    void peekOnStreamIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    void run(List<String> list) {
                        list.stream().peek(x -> x.length()).count();
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("peek"));
    }

    @Test
    void streamStaticFactoriesAreDetected() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.stream.Stream;

                class Sample {
                    void run() {
                        Stream.of(1, 2, 3).forEach(x -> System.out.println(x));
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void arraysStreamIsDetected() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.Arrays;

                class Sample {
                    void run(int[] values) {
                        Arrays.stream(values).forEach(x -> System.out.println(x));
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void mapForEachOnNonStreamIsIgnored() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.Map;

                class Sample {
                    void run(Map<String, Integer> map) {
                        map.forEach((k, v) -> System.out.println(k));
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void sideEffectFreePipelineIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    long run(List<String> list) {
                        return list.stream()
                                .filter(s -> s.length() > 2)
                                .map(String::length)
                                .count();
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void mutationOfCapturedFieldInStreamLambdaIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    int counter;

                    List<Integer> run(List<Integer> list) {
                        return list.stream().map(x -> {
                            counter = x;
                            return x;
                        }).toList();
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("counter"));
        assertEquals(8, violations.getFirst().line());
    }

    @Test
    void incrementOfCapturedFieldInStreamLambdaIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    int counter;

                    void run(List<Integer> list) {
                        list.stream().map(x -> {
                            counter++;
                            return x;
                        });
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("counter"));
    }

    @Test
    void mutationOfLocalDeclaredInsideLambdaIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    List<Integer> run(List<Integer> list) {
                        return list.stream().map(x -> {
                            int y = x + 1;
                            y = y * 2;
                            return y;
                        }).toList();
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void arrayElementMutationInStreamLambdaIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    void run(List<Integer> list, int[] first) {
                        list.stream().map(x -> {
                            first[0] = x;
                            return x;
                        });
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("array"));
    }

    @Test
    void compoundAssignmentOnCapturedVariableIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.List;

                class Sample {
                    int total;

                    void run(List<Integer> list) {
                        list.parallelStream().map(x -> {
                            total += x;
                            return x;
                        });
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("total"));
    }
}
