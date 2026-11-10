package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Flags classes that extend a collection class (`ArrayList`, `HashMap`, ...):
/// inheritance exposes the whole collection API on the subclass ("my public
/// API gets all the methods of ArrayList, ahhhh"). Course source:
/// 1-concept.pdf — prefer composition (a private field + delegation).
public final class NoExtendsCollectionRule implements Rule {

    private static final Set<String> COLLECTION_CLASSES = Set.of(
            "ArrayList", "LinkedList", "ArrayDeque", "Vector", "Stack", "PriorityQueue",
            "HashSet", "LinkedHashSet", "TreeSet", "HashMap", "LinkedHashMap", "TreeMap",
            "Hashtable", "ConcurrentHashMap", "CopyOnWriteArrayList", "CopyOnWriteArraySet",
            "AbstractList", "AbstractMap", "AbstractCollection", "AbstractSet", "AbstractQueue");

    @Override
    public String name() {
        return "no-extends-collection";
    }

    @Override
    public String description() {
        return "Do not extend a collection class; wrap it in a private field and delegate.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class NoExtendsCollectionRuleScanner extends TreeScanner<Void, Void> {
            NoExtendsCollectionRuleScanner() {
                super();
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                var extendsClause = node.getExtendsClause();
                if (extendsClause != null
                        && COLLECTION_CLASSES.contains(TypeNames.simpleName(extendsClause))) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Class '" + node.getSimpleName() + "' extends "
                                    + TypeNames.simpleName(extendsClause)
                                    + " and exposes its whole API; prefer composition"));
                }
                return super.visitClass(node, unused);
            }
        }
        new NoExtendsCollectionRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
