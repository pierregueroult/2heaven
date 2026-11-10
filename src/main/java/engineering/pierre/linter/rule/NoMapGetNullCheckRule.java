package engineering.pierre.linter.rule;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.LiteralTree;
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

/// Flags `if (map.get(key) == null)` / `!= null`: `Map.get` returning null is
/// ambiguous — prefer `getOrDefault`, `computeIfAbsent` or `containsKey`.
/// Course source: 7-Collections.pdf ("map.get(clé) renvoie null, getOrDefault()
/// est mieux").
public final class NoMapGetNullCheckRule implements Rule {

    @Override
    public String name() {
        return "no-map-get-null-check";
    }

    @Override
    public String description() {
        return "Do not null-check map.get(key); use getOrDefault/computeIfAbsent/containsKey.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class GetNullCheckScanner extends TreeScanner<Void, Void> {
            GetNullCheckScanner() {
                super();
            }

            @Override
            public Void visitBinary(BinaryTree node, Void unused) {
                if (node.getKind() == Tree.Kind.EQUAL_TO
                        || node.getKind() == Tree.Kind.NOT_EQUAL_TO) {
                    Tree left = node.getLeftOperand();
                    Tree right = node.getRightOperand();
                    if (isNullLiteral(left) && isGetCall(right) || isNullLiteral(right) && isGetCall(left)) {
                        violations.add(violation(context, node, Severity.WARNING,
                                "null-checking get(...) hides the Map.get-returns-null contract; "
                                        + "use getOrDefault/computeIfAbsent/containsKey"));
                    }
                }
                return super.visitBinary(node, unused);
            }

            private boolean isNullLiteral(Tree tree) {
                return tree instanceof LiteralTree literal && literal.getValue() == null;
            }

            private boolean isGetCall(Tree tree) {
                return tree instanceof MethodInvocationTree invocation
                        && invocation.getMethodSelect() instanceof MemberSelectTree select
                        && select.getIdentifier().contentEquals("get");
            }
        }
        new GetNullCheckScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
