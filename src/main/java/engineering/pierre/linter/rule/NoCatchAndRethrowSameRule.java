package engineering.pierre.linter.rule;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags `catch (FooException e) { throw e; }`: a catch that only rethrows the
/// same exception is useless — declare `throws` instead ("Throw early, catch
/// late"). Course source: 5-Exceptions.pdf.
public final class NoCatchAndRethrowSameRule implements Rule {

    @Override
    public String name() {
        return "no-catch-and-rethrow-same";
    }

    @Override
    public String description() {
        return "A catch block that only rethrows the caught exception is useless; "
                + "propagate with throws instead.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class NoCatchAndRethrowSameRuleScanner extends TreeScanner<Void, Void> {
            NoCatchAndRethrowSameRuleScanner() {
                super();
            }

            @Override
            public Void visitCatch(CatchTree node, Void unused) {
                var statements = node.getBlock().getStatements();
                if (statements.size() == 1
                        && statements.getFirst() instanceof ThrowTree throwTree
                        && throwTree.getExpression() instanceof IdentifierTree id
                        && id.getName().contentEquals(node.getParameter().getName())) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "catch (" + node.getParameter().getType() + " " + id.getName()
                                    + ") only rethrows; declare throws on the method instead"));
                }
                return super.visitCatch(node, unused);
            }
        }
        new NoCatchAndRethrowSameRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
