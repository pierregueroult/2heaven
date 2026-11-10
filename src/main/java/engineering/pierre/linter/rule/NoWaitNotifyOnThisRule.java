package engineering.pierre.linter.rule;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Flags `wait()`, `notify()` and `notifyAll()` invoked on `this` — either
/// explicitly (`this.wait()`) or implicitly (a bare `wait()` inside the
/// class). Course source: 12-Concurrence-Rendez-vous.pdf — wait/notify must
/// be called on a dedicated, shared lock object, inside matching
/// `synchronized` blocks ("Attention il faut bien les 2 blocs synchronized").
public final class NoWaitNotifyOnThisRule implements Rule {

    private static final Set<String> MONITOR_METHODS = Set.of("wait", "notify", "notifyAll");

    @Override
    public String name() {
        return "no-wait-notify-on-this";
    }

    @Override
    public String description() {
        return "Do not call wait/notify/notifyAll on 'this'; use a dedicated lock object.";
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
                Tree select = node.getMethodSelect();
                if (select instanceof MemberSelectTree memberSelect) {
                    if (MONITOR_METHODS.contains(memberSelect.getIdentifier().toString())
                            && memberSelect.getExpression() instanceof IdentifierTree self
                            && self.getName().contentEquals("this")) {
                        violations.add(violation(context, node, Severity.WARNING,
                                memberSelect.getIdentifier() + "() on 'this'; use a dedicated lock object"));
                    }
                } else if (select instanceof IdentifierTree id
                        && MONITOR_METHODS.contains(id.getName().toString())) {
                    // bare call: implicitly this.wait()/notify()/notifyAll()
                    violations.add(violation(context, node, Severity.WARNING,
                            id.getName() + "() is an implicit this." + id.getName()
                                    + "(); use a dedicated lock object"));
                }
                return super.visitMethodInvocation(node, unused);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
