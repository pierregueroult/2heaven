package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Flags `return someList;` where `someList` is a mutable collection field:
/// the caller can then mutate the object's internal state. Course source:
/// 7-Collections.pdf — return `List.copyOf(...)` / `Collections.unmodifiableList(...)`
/// or make a defensive copy.
public final class PreferDefensiveCopyRule implements Rule {

    private static final Set<String> COLLECTION_TYPES = Set.of(
            "Collection", "List", "Set", "Map", "Queue", "Deque",
            "ArrayList", "LinkedList", "ArrayDeque", "HashSet", "LinkedHashSet", "TreeSet",
            "HashMap", "LinkedHashMap", "TreeMap", "Vector", "Stack", "Hashtable");

    @Override
    public String name() {
        return "prefer-defensive-copy";
    }

    @Override
    public String description() {
        return "Do not return a mutable collection field directly; return List.copyOf "
                + "or an unmodifiable view.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class CopyScanner extends TreeScanner<Void, Void> {
            private final Deque<Map<String, VariableTree>> classFields;

            CopyScanner() {
                this.classFields = new ArrayDeque<>();
                super();
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                Map<String, VariableTree> fields = new HashMap<>();
                for (Tree member : node.getMembers()) {
                    if (member instanceof VariableTree field) {
                        fields.put(field.getName().toString(), field);
                    }
                }
                classFields.push(fields);
                try {
                    return super.visitClass(node, unused);
                } finally {
                    classFields.pop();
                }
            }

            @Override
            public Void visitReturn(ReturnTree node, Void unused) {
                if (node.getExpression() instanceof IdentifierTree id) {
                    VariableTree field = findField(id.getName().toString());
                    if (field != null
                            && COLLECTION_TYPES.contains(TypeNames.simpleName(field.getType()))) {
                        violations.add(violation(context, node, Severity.WARNING,
                                "returning field '" + id.getName() + "' exposes the internal mutable "
                                        + TypeNames.simpleName(field.getType())
                                        + "; return a copy (List.copyOf) or an unmodifiable view"));
                    }
                }
                return super.visitReturn(node, unused);
            }

            private VariableTree findField(String name) {
                for (Map<String, VariableTree> fields : classFields) {
                    VariableTree field = fields.get(name);
                    if (field != null) {
                        return field;
                    }
                }
                return null;
            }
        }
        new CopyScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
