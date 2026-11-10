package engineering.pierre.linter;

import com.sun.source.tree.Tree;

import java.util.List;

/// A single lint rule.
///
/// Implementations must be stateless and thread-safe: the [Linter] may run
/// the same rule instance concurrently over many files. To add a rule,
/// implement this interface and register the instance with the [Linter].
public interface Rule {

    /// Stable, unique, kebab-case identifier, e.g. `no-system-out`.
    String name();

    /// Human-readable summary of what the rule enforces.
    String description();

    /// Inspects [context] and returns every violation found, possibly none.
    List<LintViolation> check(LintContext context);

    /// Convenience factory for AST-based rules: builds a [LintViolation] of
    /// this rule located at [tree]'s start position.
    default LintViolation violation(LintContext context, Tree tree,
                                    Severity severity, String message) {
        var position = context.lineColumnOf(tree);
        return new LintViolation(context.path(), name(), severity,
                position.line(), position.column(), message);
    }
}
