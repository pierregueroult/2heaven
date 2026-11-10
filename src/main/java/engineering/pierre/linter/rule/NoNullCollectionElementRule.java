package engineering.pierre.linter.rule;

import com.sun.source.tree.LiteralTree;
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

/// Flags `add(null)` / `put(key, null)` / `offer(null)` on collections: null
/// elements are rejected by several implementations (`List.of`, `ArrayDeque`)
/// and mean "absent" elsewhere. Course source: 7-Collections.pdf ("la bonne
/// pratique est de ne jamais mettre de null", "Null veut dire quelque chose").
public final class NoNullCollectionElementRule implements Rule {

    private static final Set<String> ADDING_METHODS = Set.of(
            "add", "addFirst", "addLast", "offer", "offerFirst", "offerLast", "push", "put");

    @Override
    public String name() {
        return "no-null-collection-element";
    }

    @Override
    public String description() {
        return "Never add null to a collection/map; use Optional or an empty marker instead.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class NoNullCollectionElementRuleScanner extends TreeScanner<Void, Void> {
            NoNullCollectionElementRuleScanner() {
                super();
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                if (node.getMethodSelect() instanceof MemberSelectTree select
                        && ADDING_METHODS.contains(select.getIdentifier().toString())
                        && node.getArguments().stream().anyMatch(arg -> isNullLiteral(arg))) {
                    violations.add(violation(context, node, Severity.WARNING,
                            select.getIdentifier() + "(..., null) stores null in a collection; "
                                    + "use Optional.empty() or a sentinel instead"));
                }
                return super.visitMethodInvocation(node, unused);
            }

            private boolean isNullLiteral(com.sun.source.tree.Tree tree) {
                return tree instanceof LiteralTree literal && literal.getValue() == null;
            }
        }
        new NoNullCollectionElementRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
