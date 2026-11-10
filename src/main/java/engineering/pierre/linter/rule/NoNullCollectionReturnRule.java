package engineering.pierre.linter.rule;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/// Flags `return null;` in a method whose return type is a collection/map:
/// callers then need null checks everywhere — return an empty collection
/// instead. Course source: 7-Collections.pdf ("On ne renvoie pas null ! Une
/// méthode qui renvoie une Collection/Map ne renvoie pas null mais une
/// Collection/Map vide").
public final class NoNullCollectionReturnRule implements Rule {

    private static final Set<String> COLLECTION_TYPES = Set.of(
            "Collection", "List", "Set", "Map", "Queue", "Deque", "Iterable",
            "ArrayList", "LinkedList", "ArrayDeque", "HashSet", "LinkedHashSet", "TreeSet",
            "HashMap", "LinkedHashMap", "TreeMap", "Vector", "Stack", "Hashtable");

    @Override
    public String name() {
        return "no-null-collection-return";
    }

    @Override
    public String description() {
        return "A method returning a Collection/Map must return an empty one, never null.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class ReturnScanner extends TreeScanner<Void, Void> {
            private final Deque<MethodTree> methods;

            ReturnScanner() {
                this.methods = new ArrayDeque<>();
                super();
            }

            @Override
            public Void visitMethod(MethodTree node, Void unused) {
                methods.push(node);
                try {
                    return super.visitMethod(node, unused);
                } finally {
                    methods.pop();
                }
            }

            @Override
            public Void visitReturn(ReturnTree node, Void unused) {
                if (node.getExpression() instanceof LiteralTree literal
                        && literal.getValue() == null
                        && !methods.isEmpty()) {
                    var returnType = methods.peek().getReturnType();
                    if (returnType != null
                            && COLLECTION_TYPES.contains(TypeNames.simpleName(returnType))) {
                        violations.add(violation(context, node, Severity.WARNING,
                                "returning null from a " + TypeNames.simpleName(returnType)
                                        + " method forces null checks on every caller; "
                                        + "return an empty collection instead"));
                    }
                }
                return super.visitReturn(node, unused);
            }
        }
        new ReturnScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
