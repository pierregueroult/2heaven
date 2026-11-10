package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import javax.lang.model.element.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Flags monitor objects used in `synchronized` blocks that are not a
/// dedicated `private final Object` (or a `ReentrantLock`). Course sources:
/// 11-Concurrence-Section critiques.pdf (a monitor that is not private/final
/// can be locked by other code -> deadlock; synchronizing on an interned
/// object such as a String is dangerous) and 14-Memory-model.pdf ("si pas
/// final, problème de publication").
public final class RequireFinalLockObjectRule implements Rule {

    private static final Set<String> LOCK_TYPE_NAMES = Set.of("Object", "Lock", "ReentrantLock");

    @Override
    public String name() {
        return "require-final-lock-object";
    }

    @Override
    public String description() {
        return "Synchronized blocks must lock on a dedicated `private final Object` "
                + "(or ReentrantLock), never on `this`, a class literal, or a "
                + "non-final/non-private field.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class LockScanner extends TreeScanner<Void, Void> {
            private final Deque<Map<String, VariableTree>> classFields;

            LockScanner() {
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
            public Void visitMethod(MethodTree node, Void unused) {
                if (node.getModifiers().getFlags().contains(Modifier.SYNCHRONIZED)) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "synchronized method locks on 'this', which is publicly reachable; "
                                    + "use a `private final Object` lock or a ReentrantLock"));
                }
                return super.visitMethod(node, unused);
            }

            @Override
            public Void visitSynchronized(SynchronizedTree node, Void unused) {
                var expression = unwrap(node.getExpression());
                String fieldName = null;
                if (expression instanceof IdentifierTree id) {
                    fieldName = id.getName().toString();
                } else if (expression instanceof MemberSelectTree memberSelect
                        && memberSelect.getExpression() instanceof IdentifierTree self
                        && self.getName().contentEquals("this")) {
                    fieldName = memberSelect.getIdentifier().toString();
                }
                if (fieldName == null || fieldName.equals("this")) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "synchronized on '" + expression + "' is not a dedicated lock; "
                                    + "use a `private final Object` lock or a ReentrantLock"));
                } else {
                    VariableTree field = findField(fieldName);
                    if (field != null) {
                        var flags = field.getModifiers().getFlags();
                        boolean privateFinal = flags.contains(Modifier.PRIVATE)
                                && flags.contains(Modifier.FINAL);
                        boolean lockType = LOCK_TYPE_NAMES.contains(
                                TypeNames.simpleName(field.getType()));
                        if (!privateFinal || !lockType) {
                            violations.add(violation(context, node, Severity.WARNING,
                                    "lock field '" + fieldName + "' must be `private final` with type "
                                            + "Object or ReentrantLock (current: "
                                            + field.getModifiers().getFlags().toString().toLowerCase()
                                            + " " + TypeNames.simpleName(field.getType()) + ")"));
                        }
                    }
                    // A local variable used as monitor cannot be shared; not a bug.
                }
                return super.visitSynchronized(node, unused);
            }

            private Tree unwrap(Tree tree) {
                while (tree instanceof com.sun.source.tree.ParenthesizedTree parenthesized) {
                    tree = parenthesized.getExpression();
                }
                return tree;
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
        new LockScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
