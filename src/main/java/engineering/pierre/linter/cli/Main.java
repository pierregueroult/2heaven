package engineering.pierre.linter.cli;

import engineering.pierre.linter.Linter;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Severity;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

/// Command-line runner with explicit operational failures and configurable policy.
public final class Main {
    private Main() {
        super();
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /// Returns 0 for success, 1 for findings at the threshold, 2 for invalid input or analysis failure.
    public static int run(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(args);
        Objects.requireNonNull(out);
        Objects.requireNonNull(err);
        try {
            return execute(args, out);
        } catch (IOException | UncheckedIOException | IllegalArgumentException | Linter.LintException e) {
            err.println(e.getMessage());
            return 2;
        }
    }

    private static int execute(String[] args, PrintStream out) throws IOException {
        List<String> roots = new ArrayList<>();
        Path configFile = Path.of(".2heaven.properties");
        boolean explicitConfig = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help")) {
                out.println("Usage: java -jar 2heaven.jar [--config <properties-file>] <path>...");
                return 0;
            }
            if (args[i].equals("--config")) {
                if (++i == args.length) {
                    throw new IllegalArgumentException("--config requires a file");
                }
                configFile = Path.of(args[i]);
                explicitConfig = true;
            } else if (args[i].startsWith("--")) {
                throw new IllegalArgumentException("Unknown option: " + args[i]);
            } else {
                roots.add(args[i]);
            }
        }
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("Usage: java -jar 2heaven.jar [--config <file>] <path>...");
        }
        Properties properties = new Properties();
        if (explicitConfig || Files.exists(configFile)) {
            try (var reader = Files.newBufferedReader(configFile)) {
                properties.load(reader);
            }
        }
        var builtIn = Linter.withBuiltInRules();
        Set<String> names = new HashSet<>();
        builtIn.rules().forEach(rule -> names.add(rule.name()));
        Map<String, Severity> severities = new HashMap<>();
        Set<String> disabled = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("rule.") && names.contains(key.substring(5))) {
                String value = properties.getProperty(key).trim();
                if (value.equalsIgnoreCase("off")) {
                    disabled.add(key.substring(5));
                } else {
                    severities.put(key.substring(5), severity(value));
                }
            } else if (!key.equals("failOn") && !key.equals("exclude")) {
                throw new IllegalArgumentException("Unknown configuration key: " + key);
            }
        }
        Severity threshold = severity(properties.getProperty("failOn", "ERROR"));
        List<java.nio.file.PathMatcher> exclusions = Stream.of(properties.getProperty("exclude", "").split(","))
                .map(String::trim).filter(value -> !value.isEmpty())
                .map(value -> java.nio.file.FileSystems.getDefault().getPathMatcher("glob:" + value))
                .toList();
        List<Path> files = new ArrayList<>();
        for (String root : roots) {
            try (Stream<Path> paths = Files.walk(Path.of(root))) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .map(path -> path.toAbsolutePath().normalize())
                        .filter(path -> exclusions.stream().noneMatch(matcher -> matcher.matches(relative(path))))
                        .forEach(files::add);
            }
        }
        List<Path> uniqueFiles = files.stream().distinct().sorted().toList();
        if (uniqueFiles.isEmpty()) {
            throw new IllegalArgumentException("No .java files found under the given paths after exclusions.");
        }
        var linter = new Linter(builtIn.rules().stream().filter(rule -> !disabled.contains(rule.name())).toList());
        List<LintViolation> violations = linter.lint(uniqueFiles).stream()
                .map(v -> new LintViolation(v.path(), v.ruleName(), severities.getOrDefault(v.ruleName(), v.severity()),
                        v.line(), v.column(), v.message()))
                .toList();
        violations.forEach(out::println);
        long failures = violations.stream().filter(v -> v.severity().compareTo(threshold) >= 0).count();
        out.printf("%d violation(s) (%d at or above %s) in %d file(s)%n",
                violations.size(), failures, threshold, uniqueFiles.size());
        return failures > 0 ? 1 : 0;
    }

    private static Severity severity(String value) {
        return Severity.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static Path relative(Path path) {
        return Path.of("").toAbsolutePath().normalize().relativize(path);
    }
}
