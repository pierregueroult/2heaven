package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags classes that override `equals(Object)` without also overriding
/// `hashCode()` (and vice versa) — they break the equals/hashCode contract.
/// Course source: 4-objets predefinis.pdf. Records and enums are exempt
/// (their members are generated/final).
public final class EqualsWithoutHashCodeRule implements Rule {

    @Override
    public String name() {
        return "equals-without-hashcode";
    }

    @Override
    public String description() {
        return "A class overriding equals(Object) must also override hashCode(), and vice versa.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void unused) {
                if (node.getKind() == Tree.Kind.CLASS) {
                    checkClass(context, node, violations);
                }
                return super.visitClass(node, unused);
            }

            private void checkClass(LintContext context, ClassTree node,
                                    List<LintViolation> violations) {
                boolean hasEquals = false;
                boolean hasHashCode = false;
                for (Tree member : node.getMembers()) {
                    if (member instanceof MethodTree method) {
                        if (method.getName().contentEquals("equals") && method.getParameters().size() == 1) {
                            hasEquals = true;
                        }
                        if (method.getName().contentEquals("hashCode") && method.getParameters().isEmpty()) {
                            hasHashCode = true;
                        }
                    }
                }
                if (hasEquals && !hasHashCode) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Class overrides equals(Object) but not hashCode()"));
                } else if (hasHashCode && !hasEquals) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Class overrides hashCode() but not equals(Object)"));
                }
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
