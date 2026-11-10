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

final class RequireFinalLockObjectRuleTest {

    private final Path tempDir;
    private final RequireFinalLockObjectRule rule;

    RequireFinalLockObjectRuleTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new RequireFinalLockObjectRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void privateFinalObjectLockIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    private final Object lock = new Object();

                    void run() {
                        synchronized (lock) {
                        }
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void privateFinalReentrantLockIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                import java.util.concurrent.locks.ReentrantLock;

                class Sample {
                    private final ReentrantLock lock = new ReentrantLock();

                    void run() {
                        lock.lock();
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }

    @Test
    void nonFinalLockFieldIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    private Object lock = new Object();

                    void run() {
                        synchronized (lock) {
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("require-final-lock-object", violations.getFirst().ruleName());
        assertEquals(5, violations.getFirst().line());
    }

    @Test
    void nonPrivateLockFieldIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    final Object lock = new Object();

                    void run() {
                        synchronized (lock) {
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void wrongLockTypeIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    private final String lock = "lock";

                    void run() {
                        synchronized (lock) {
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void synchronizedOnThisIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    void run() {
                        synchronized (this) {
                        }
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void synchronizedMethodIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    synchronized void run() {
                    }
                }
                """);

        assertEquals(1, violations.size());
    }

    @Test
    void thisDotPrivateFinalLockIsAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    private final Object lock = new Object();

                    void run() {
                        synchronized (this.lock) {
                        }
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
