package engineering.pierre.linter;

import java.nio.file.Path;
import java.util.Objects;

/// One problem found by a [Rule] in a source file.
///
/// Positions are 1-based; a line or column of `0` means "not applicable"
/// (for example a whole-file finding).
public record LintViolation(
        Path path,
        String ruleName,
        Severity severity,
        int line,
        int column,
        String message
) {
    public LintViolation {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(ruleName, "ruleName");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
    }

    @Override
    public String toString() {
        return "%s:%d:%d: %s [%s] %s"
                .formatted(path, line, column, severity, ruleName, message);
    }
}
