package engineering.pierre.linter.rule;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Flags the deprecated, unsafe thread control methods `stop()`, `suspend()`
/// and `resume()`. Course source: 11b-Concurrence-Interruption-Exceptions.pdf —
/// use `interrupt()` instead.
///
/// Without type attribution the receiver type is unknown, so `stop()` is only
/// flagged on receivers named `Thread` or containing "thread"; `suspend()` and
/// `resume()` are flagged on any receiver.
public final class NoDeprecatedThreadControlRule implements Rule {

    private static final Set<String> ALWAYS_FLAGGED = Set.of("suspend", "resume");

    @Override
    public String name() {
        return "no-deprecated-thread-control";
    }

    @Override
    public String description() {
        return "Do not call Thread.stop/suspend/resume (deprecated for removal); use interrupt().";
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
                if (node.getMethodSelect() instanceof MemberSelectTree select) {
                    String method = select.getIdentifier().toString();
                    if (ALWAYS_FLAGGED.contains(method) || isThreadStop(select, method)) {
                        violations.add(violation(context, node, Severity.ERROR,
                                "Thread." + method + "() is deprecated and unsafe; use interrupt()"));
                    }
                }
                return super.visitMethodInvocation(node, unused);
            }

            private boolean isThreadStop(MemberSelectTree select, String method) {
                if (!method.equals("stop")) {
                    return false;
                }
                if (select.getExpression() instanceof IdentifierTree id) {
                    String receiver = id.getName().toString();
                    return receiver.equals("Thread")
                            || receiver.toLowerCase().contains("thread");
                }
                return false;
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
