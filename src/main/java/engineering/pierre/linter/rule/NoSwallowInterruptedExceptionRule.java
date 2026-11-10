package engineering.pierre.linter.rule;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags `catch (InterruptedException)` blocks that neither rethrow nor
/// restore the interrupt status (`Thread.currentThread().interrupt()`).
/// Course source: 11b-Concurrence-Interruption-Exceptions.pdf.
public final class NoSwallowInterruptedExceptionRule implements Rule {

    @Override
    public String name() {
        return "no-swallow-interrupted-exception";
    }

    @Override
    public String description() {
        return "An InterruptedException must be rethrown or the interrupt status restored "
                + "with Thread.currentThread().interrupt(), never swallowed.";
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
                if (TypeNames.simpleName(node.getParameter().getType())
                        .equals("InterruptedException")) {
                    checkBody(context, node, violations);
                }
                return super.visitCatch(node, unused);
            }

            private void checkBody(LintContext context, CatchTree node,
                                   List<LintViolation> violations) {
                boolean[] handled = {false};
                new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitThrow(ThrowTree throwTree, Void unused) {
                        handled[0] = true;
                        return super.visitThrow(throwTree, unused);
                    }

                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
                        if (invocation.getMethodSelect() instanceof MemberSelectTree select
                                && select.getIdentifier().contentEquals("interrupt")) {
                            handled[0] = true;
                        }
                        return super.visitMethodInvocation(invocation, unused);
                    }
                }.scan(node.getBlock(), null);
                if (!handled[0]) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "InterruptedException is swallowed; rethrow it or call "
                                    + "Thread.currentThread().interrupt()"));
                }
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
