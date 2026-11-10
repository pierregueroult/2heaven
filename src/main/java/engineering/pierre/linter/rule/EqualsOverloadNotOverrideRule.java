package engineering.pierre.linter.rule;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags an `equals(...)` method whose single parameter is not `Object`:
/// that is an overload, not an override — `equals(Point)` does not replace
/// `Object.equals(Object)`, so `contains()`/`HashMap` still use identity.
/// Course source: 4-objets prédéfinis.pdf ("Ce n'est pas le même equals(),
/// car il n'y a pas eut [d'override]").
public final class EqualsOverloadNotOverrideRule implements Rule {

    @Override
    public String name() {
        return "equals-overload-not-override";
    }

    @Override
    public String description() {
        return "equals(X) with X != Object is an overload, not an override; "
                + "collections will keep using Object.equals(Object).";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class EqualsOverloadNotOverrideRuleScanner extends TreeScanner<Void, Void> {
            EqualsOverloadNotOverrideRuleScanner() {
                super();
            }

            @Override
            public Void visitMethod(MethodTree node, Void unused) {
                if (node.getName().contentEquals("equals")
                        && node.getParameters().size() == 1
                        && !"Object".equals(TypeNames.simpleName(
                                node.getParameters().getFirst().getType()))) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "equals(" + TypeNames.simpleName(node.getParameters().getFirst().getType())
                                    + ") overloads but does not override equals(Object); "
                                    + "use equals(Object) with a pattern so collections work"));
                }
                return super.visitMethod(node, unused);
            }
        }
        new EqualsOverloadNotOverrideRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
