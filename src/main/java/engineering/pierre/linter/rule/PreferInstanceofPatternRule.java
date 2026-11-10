package engineering.pierre.linter.rule;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags the classic `if (o instanceof T) { T t = (T) o; ... }` dance: with
/// pattern matching, `if (o instanceof T t)` binds the cast variable directly
/// and removes the error-prone manual cast. Course source: 6-type et methode
/// parametre.pdf ("on détecte le problème à l'endroit du cast, pas à l'endroit
/// où l'on a fait l'erreur") and 4-Pattern Matching.pdf.
public final class PreferInstanceofPatternRule implements Rule {

    @Override
    public String name() {
        return "prefer-instanceof-pattern";
    }

    @Override
    public String description() {
        return "Use `instanceof T t` pattern binding instead of instanceof + manual cast.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class PreferInstanceofPatternRuleScanner extends TreeScanner<Void, Void> {
            PreferInstanceofPatternRuleScanner() {
                super();
            }

            @Override
            public Void visitIf(IfTree node, Void unused) {
                var condition = node.getCondition();
                while (condition instanceof com.sun.source.tree.ParenthesizedTree parens) {
                    condition = parens.getExpression();
                }
                if (condition instanceof InstanceOfTree instanceOf
                        && instanceOf.getPattern() == null
                        && node.getThenStatement() instanceof BlockTree block
                        && hasManualCastOf(block, instanceOf.getExpression().toString())) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "instanceof + manual cast: use a pattern variable, "
                                    + "`if (" + instanceOf.getExpression() + " instanceof "
                                    + instanceOf.getType() + " t)`"));
                }
                return super.visitIf(node, unused);
            }

            private boolean hasManualCastOf(BlockTree block, String expression) {
                for (StatementTree statement : block.getStatements()) {
                    if (statement instanceof com.sun.source.tree.VariableTree var
                            && var.getInitializer() instanceof TypeCastTree cast
                            && cast.getExpression().toString().equals(expression)) {
                        return true;
                    }
                }
                return false;
            }
        }
        new PreferInstanceofPatternRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
