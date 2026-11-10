package engineering.pierre.linter.rule;

import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags casts to a parameterized type, e.g. `(List<String>) obj` — due to
/// type erasure such casts are unchecked. Course source: 6- type et methode
/// parametre.pdf (erasure).
public final class NoUncheckedCastRule implements Rule {

    @Override
    public String name() {
        return "no-unchecked-cast";
    }

    @Override
    public String description() {
        return "Casts to a parameterized type are unchecked (type erasure); avoid them.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitTypeCast(TypeCastTree node, Void unused) {
                if (node.getType() instanceof ParameterizedTypeTree parameterized) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Cast to '" + TypeNames.simpleName(parameterized)
                                    + "<...>' is unchecked due to type erasure"));
                }
                return super.visitTypeCast(node, unused);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
