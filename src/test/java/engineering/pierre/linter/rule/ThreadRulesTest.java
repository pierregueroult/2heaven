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

/// Tests for no-deprecated-thread-control.
class ThreadRulesTest {

    private final Path tempDir;
    private final Rule rule;

    ThreadRulesTest() throws IOException {
        this.tempDir = Files.createTempDirectory("lint-test");
        this.rule = new NoDeprecatedThreadControlRule();
        super();
    }

    private List<LintViolation> check(String source) throws IOException {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, source);
        LintContext context = SourceParser.parse(file);
        return rule.check(context);
    }

    @Test
    void staticThreadStopIsFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    void run() {
                        Thread.stop();
                    }
                }
                """);

        assertEquals(1, violations.size());
        assertEquals("no-deprecated-thread-control", violations.getFirst().ruleName());
    }

    @Test
    void instanceThreadStopAndSuspendResumeAreFlagged() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    void run(Thread workerThread) {
                        workerThread.stop();
                        workerThread.suspend();
                        workerThread.resume();
                    }
                }
                """);

        assertEquals(3, violations.size());
    }

    @Test
    void interruptAndUnrelatedStopAreAccepted() throws IOException {
        List<LintViolation> violations = check("""
                class Sample {
                    void run(Thread workerThread) {
                        workerThread.interrupt();
                        service.stop();
                    }
                    void service() {
                    }
                }
                """);

        assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
    }
}
