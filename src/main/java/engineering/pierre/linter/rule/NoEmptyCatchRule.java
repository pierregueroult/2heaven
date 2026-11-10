package engineering.pierre.linter.rule;

import com.sun.source.tree.CatchTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags `catch` blocks with an empty body — the exception is silently
/// swallowed. Course source: 5-Exceptions.pdf ("throw early, catch late").
public final class NoEmptyCatchRule implements Rule {

    @Override
    public String name() {
        return "no-empty-catch";
    }

    @Override
    public String description() {
        return "Catch blocks must not be empty; handle the exception or rethrow it.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitCatch(CatchTree node, Void unused) {
                if (node.getBlock().getStatements().isEmpty()) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Empty catch block swallows '" + node.getParameter().getName()
                                    + "'; handle it or rethrow"));
                }
                return super.visitCatch(node, unused);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
