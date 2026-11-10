package engineering.pierre.linter.cli;

import engineering.pierre.linter.Linter;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Severity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/// Command-line entry point: lints the given files or directories (searched
/// recursively for `.java` files) with all built-in rules.
///
/// Usage: `java -jar 2heaven.jar <path>...`
/// Exits with code 1 when any ERROR-severity violation is found (warnings and
/// info are reported but do not fail), 2 on usage or IO errors.
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar 2heaven.jar <file-or-directory>...");
            System.exit(2);
        }
        List<Path> files = collectJavaFiles(args);
        if (files.isEmpty()) {
            System.err.println("No .java files found under the given paths.");
            System.exit(2);
        }
        List<LintViolation> violations = Linter.withBuiltInRules().lint(files);
        violations.forEach(v -> System.out.println(v.toString()));
        long errors = violations.stream()
                .filter(v -> v.severity() == Severity.ERROR)
                .count();
        System.out.printf("%d violation(s) (%d error(s)) in %d file(s)%n",
                violations.size(), errors, files.size());
        if (errors > 0) {
            System.exit(1);
        }
    }

    private static List<Path> collectJavaFiles(String[] args) {
        return Stream.of(args)
                .map(Path::of)
                .flatMap(Main::walk)
                .filter(p -> p.toString().endsWith(".java"))
                .toList();
    }

    private static Stream<Path> walk(Path root) {
        if (Files.isRegularFile(root)) {
            return Stream.of(root);
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + root, e);
        }
    }
}
