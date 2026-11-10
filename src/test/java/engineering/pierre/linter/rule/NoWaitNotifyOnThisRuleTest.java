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

class NoWaitNotifyOnThisRuleTest {

    @TempDir
    Path tempDir;

    private final NoWaitNotifyOnThisRule rule = new NoWaitNotifyOnThisRule();

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void explicitThisWaitIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    synchronized void run() throws InterruptedException {
                        this.wait();
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-wait-notify-on-this", violations.getFirst().ruleName());
        assertEquals(3, violations.getFirst().line());
    }

    @Test
    void bareWaitNotifyNotifyAllAreFlaggedAsImplicitThis() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    synchronized void run() throws InterruptedException {
                        wait();
                        notify();
                        notifyAll();
                    }
                }
                """);

        assertEquals(3, violations.size());
    }

    @Test
    void callsOnDedicatedLockAreAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    private final Object lock = new Object();

                    void run() throws InterruptedException {
                        synchronized (lock) {
                            lock.wait();
                            lock.notifyAll();
                        }
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
