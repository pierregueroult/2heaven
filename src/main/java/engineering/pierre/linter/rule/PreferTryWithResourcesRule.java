package engineering.pierre.linter.rule;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags manual `close()` calls in a `finally` block — try-with-resources
/// closes deterministically and is less error-prone. Course source:
/// 20-Input-Output.pdf (try-with-resources since Java 7).
public final class PreferTryWithResourcesRule implements Rule {

    @Override
    public String name() {
        return "prefer-try-with-resources";
    }

    @Override
    public String description() {
        return "Close resources with try-with-resources instead of a manual close() in finally.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitTry(TryTree node, Void unused) {
                BlockTree finallyBlock = node.getFinallyBlock();
                if (finallyBlock != null) {
                    new TreeScanner<Void, Void>() {
                        @Override
                        public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
                            if (invocation.getMethodSelect() instanceof MemberSelectTree select
                                    && select.getIdentifier().contentEquals("close")) {
                                violations.add(violation(context, invocation, Severity.WARNING,
                                        "Manual close() in finally; use try-with-resources"));
                            }
                            return super.visitMethodInvocation(invocation, unused);
                        }
                    }.scan(finallyBlock, null);
                }
                return super.visitTry(node, unused);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
