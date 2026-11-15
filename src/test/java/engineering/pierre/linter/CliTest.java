package engineering.pierre.linter;

import engineering.pierre.linter.cli.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CliTest {
    private static int run(String... args) {
        Objects.requireNonNull(args);
        try (var output = new PrintStream(new ByteArrayOutputStream())) {
            return Main.run(args, output, output);
        }
    }

    @Test
    void warningThresholdAndRuleOverrides(@TempDir Path directory) throws IOException {
        Objects.requireNonNull(directory);
        Path source = directory.resolve("Example.java");
        Files.writeString(source, "final class Example { public void run(Object input) {} }");
        Path config = directory.resolve("lint.properties");
        Files.writeString(config, "failOn=WARNING\n");
        assertEquals(1, run("--config", config.toString(), source.toString()));
        Files.writeString(config, "rule.require-non-null-object-param=off\nfailOn=INFO\n");
        assertEquals(0, run("--config", config.toString(), source.toString()));
        Files.writeString(config, "rule.require-non-null-object-param=ERROR\n");
        assertEquals(1, run("--config", config.toString(), source.toString()));
    }

    @Test
    void invalidInputAndSyntaxFailWithExitTwo(@TempDir Path directory) throws IOException {
        Objects.requireNonNull(directory);
        Path source = directory.resolve("Broken.java");
        Files.writeString(source, "}{");
        assertEquals(2, run(source.toString()));
        assertEquals(2, run(directory.resolve("missing").toString()));
        assertEquals(2, run("--config"));
        assertEquals(2, run("--unknown"));
        Path config = directory.resolve("lint.properties");
        Files.writeString(config, "rule.typo=off\n");
        assertEquals(2, run("--config", config.toString(), source.toString()));
    }

    @Test
    void overlappingRootsAreDeduplicatedAndExclusionsApply(@TempDir Path directory) throws IOException {
        Objects.requireNonNull(directory);
        Path source = directory.resolve("Clean.java");
        Files.writeString(source, "final class Clean {}");
        Path broken = directory.resolve("Broken.java");
        Files.writeString(broken, "}{");
        Path config = directory.resolve("lint.properties");
        Files.writeString(config, "exclude=**/Broken.java\n");
        var bytes = new ByteArrayOutputStream();
        try (var output = new PrintStream(bytes)) {
            assertEquals(0, Main.run(new String[]{"--config", config.toString(), directory.toString(),
                    source.toString()}, output, output));
        }
        assertTrue(bytes.toString().contains("in 1 file(s)"));
    }

    @Test
    void bestEffortTypeAnalysisDoesNotLeakCompilerOutput(@TempDir Path directory) throws IOException {
        Objects.requireNonNull(directory);
        Path source = directory.resolve("Example.java");
        Files.writeString(source, "final class Example { MissingType value; }");
        var errors = new ByteArrayOutputStream();
        try (var output = new PrintStream(new ByteArrayOutputStream()); var error = new PrintStream(errors)) {
            assertEquals(0, Main.run(new String[]{source.toString()}, output, error));
        }
        assertEquals("", errors.toString());
    }

}
