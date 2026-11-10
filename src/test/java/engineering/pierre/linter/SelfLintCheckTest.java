package engineering.pierre.linter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Mirrors the CI self-lint (`java -jar target/2heaven-*.jar src/main/java
/// src/test/java`): all production and test sources must be clean.
final class SelfLintCheckTest {

    @Test
    void selfLintIsClean() {
        List<Path> files = Stream.of(Path.of("src/main/java"), Path.of("src/test/java"))
                .flatMap(SelfLintCheckTest::javaFiles)
                .toList();
        List<LintViolation> violations = Linter.withBuiltInRules().lint(files);
        assertTrue(violations.isEmpty(), "Self-lint violations: " + violations);
    }

    private static Stream<Path> javaFiles(Path dir) {
        try {
            return Files.walk(dir).filter(p -> p.toString().endsWith(".java"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
