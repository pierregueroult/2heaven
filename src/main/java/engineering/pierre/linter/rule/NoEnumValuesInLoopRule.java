package engineering.pierre.linter.rule;

import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags `for (... : SomeEnum.values())`: `values()` clones the constant array
/// on every call, so calling it in a loop condition is a performance trap.
/// Course source: 3-classe-internes-enumeration.pdf ("comme values() renvoie un
/// [nouveau tableau] à chaque appel, c'est une très mauvaise idée").
public final class NoEnumValuesInLoopRule implements Rule {

    @Override
    public String name() {
        return "no-enum-values-in-loop";
    }

    @Override
    public String description() {
        return "Do not call Enum.values() inside a loop; cache the array or use EnumSet.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class NoEnumValuesInLoopRuleScanner extends TreeScanner<Void, Void> {
            NoEnumValuesInLoopRuleScanner() {
                super();
            }

            @Override
            public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
                if (node.getExpression() instanceof MethodInvocationTree invocation
                        && invocation.getMethodSelect() instanceof MemberSelectTree select
                        && select.getIdentifier().contentEquals("values")
                        && invocation.getArguments().isEmpty()) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Enum.values() clones the constants array on every call; "
                                    + "hoist it out of the loop or use EnumSet.allOf(...)"));
                }
                return super.visitEnhancedForLoop(node, unused);
            }
        }
        new NoEnumValuesInLoopRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
