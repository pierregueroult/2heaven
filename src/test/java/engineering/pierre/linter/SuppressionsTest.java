package engineering.pierre.linter;

import engineering.pierre.linter.rule.RequireNonNullObjectParamRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SuppressionsTest {
    @Test
    void suppressionIsNamespacedAndScoped(@TempDir Path directory) throws IOException {
        Objects.requireNonNull(directory);
        Path source = directory.resolve("Example.java");
        Files.writeString(source, """
                final class Example {
                    @SuppressWarnings({"unused", "2heaven:require-non-null-object-param"})
                    public void suppressed(Object input) {}
                    @SuppressWarnings("require-non-null-object-param")
                    public void unsuppressed(Object input) {}
                    public void sibling(Object input) {}
                }
                """);
        var violations = new Linter(List.of(new RequireNonNullObjectParamRule())).lint(source);
        assertEquals(2, violations.size());
    }

    @Test
    void classSuppressionCoversNestedDeclarations(@TempDir Path directory) throws IOException {
        Objects.requireNonNull(directory);
        Path source = directory.resolve("Example.java");
        Files.writeString(source, """
                @java.lang.SuppressWarnings("2heaven:all")
                final class Example {
                    public void run(Object input) {}
                    final class Nested { public void run(Object input) {} }
                }
                final class Sibling { public void run(Object input) {} }
                """);
        var violations = new Linter(List.of(new RequireNonNullObjectParamRule())).lint(source);
        assertEquals(1, violations.size());
    }

    @Test
    void suppressionDoesNotLeakToSiblingOnSameLine(@TempDir Path directory) throws IOException {
        Objects.requireNonNull(directory);
        Path source = directory.resolve("Example.java");
        Files.writeString(source, """
                final class Example {
                    @SuppressWarnings("2heaven:all") public void a(Object input) {} public void b(Object input) {}
                }
                """);
        var violations = new Linter(List.of(new RequireNonNullObjectParamRule())).lint(source);
        assertEquals(1, violations.size());
    }

}
