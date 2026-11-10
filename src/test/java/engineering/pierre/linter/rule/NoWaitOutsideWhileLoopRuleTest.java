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

final class NoWaitOutsideWhileLoopRuleTest {

    private final Path tempDir;
    private final NoWaitOutsideWhileLoopRule rule;

    NoWaitOutsideWhileLoopRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoWaitOutsideWhileLoopRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void bareWaitOutsideWhileIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    private final Object lock = new Object();

                    void run() throws InterruptedException {
                        synchronized (lock) {
                            lock.wait();
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-wait-outside-while-loop", violations.getFirst().ruleName());
        assertEquals(6, violations.getFirst().line());
    }

    @Test
    void waitInsideWhileLoopIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    private final Object lock = new Object();
                    private boolean done;

                    void run() throws InterruptedException {
                        synchronized (lock) {
                            while (!done) {
                                lock.wait();
                            }
                        }
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void waitInsideIfButNotWhileIsStillFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    private final Object lock = new Object();

                    void run() throws InterruptedException {
                        synchronized (lock) {
                            if (System.currentTimeMillis() > 0) {
                                lock.wait(1000);
                            }
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void timedWaitInsideWhileIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    private final Object lock = new Object();
                    private boolean done;

                    void run() throws InterruptedException {
                        synchronized (lock) {
                            while (!done) {
                                lock.wait(100);
                            }
                        }
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
