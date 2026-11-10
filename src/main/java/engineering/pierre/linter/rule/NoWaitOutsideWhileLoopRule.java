package engineering.pierre.linter.rule;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags `wait()` calls that are not inside a `while` loop guarding a
/// condition. Course source: 12-Concurrence-Rendez-vous.pdf — "Un wait()
/// n'est jamais tout seul, il est toujours dans un while sur une condition":
/// a `notify()` fired before the `wait()` is lost, and a bare `wait()`
/// then blocks forever.
public final class NoWaitOutsideWhileLoopRule implements Rule {

    @Override
    public String name() {
        return "no-wait-outside-while-loop";
    }

    @Override
    public String description() {
        return "Object.wait() must be called inside a while loop on a condition, "
                + "otherwise a lost notification blocks the thread forever.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class WaitScanner extends TreeScanner<Void, Void> {
            private int whileDepth;

            WaitScanner() {
                this.whileDepth = 0;
                super();
            }

            @Override
            public Void visitWhileLoop(WhileLoopTree node, Void unused) {
                whileDepth++;
                try {
                    return super.visitWhileLoop(node, unused);
                } finally {
                    whileDepth--;
                }
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                var select = node.getMethodSelect();
                String name = null;
                if (select instanceof MemberSelectTree memberSelect) {
                    name = memberSelect.getIdentifier().toString();
                } else if (select instanceof IdentifierTree id) {
                    name = id.getName().toString();
                }
                if ("wait".equals(name) && whileDepth == 0) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "wait() outside a while loop: a lost notify() blocks forever; "
                                    + "guard it with `while (condition) { ... wait(); }`"));
                }
                return super.visitMethodInvocation(node, unused);
            }
        }
        new WaitScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
