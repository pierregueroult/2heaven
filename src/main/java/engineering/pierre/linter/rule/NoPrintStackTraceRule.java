package engineering.pierre.linter.rule;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags `printStackTrace()` calls. Course source: 5-Exceptions.pdf —
/// propagate exceptions with `throws` or log them properly instead.
public final class NoPrintStackTraceRule implements Rule {

    @Override
    public String name() {
        return "no-print-stack-trace";
    }

    @Override
    public String description() {
        return "Do not call printStackTrace(); log the exception or propagate it with throws.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                if (node.getMethodSelect() instanceof MemberSelectTree select
                        && select.getIdentifier().contentEquals("printStackTrace")) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "printStackTrace() hides the failure; use a logger or rethrow"));
                }
                return super.visitMethodInvocation(node, unused);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
