package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/// Flags non-`final` instance fields in a class that overrides `hashCode()`:
/// mutating a key after inserting it into a `HashSet`/`HashMap` makes the
/// element unfindable ("mutation ahhh"). Course source: 7-Collections.pdf.
public final class NoMutableHashcodeFieldsRule implements Rule {

    @Override
    public String name() {
        return "no-mutable-hashcode-fields";
    }

    @Override
    public String description() {
        return "Fields used by hashCode() should be final; mutating a hash key "
                + "breaks HashSet/HashMap lookups.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class NoMutableHashcodeFieldsRuleScanner extends TreeScanner<Void, Void> {
            NoMutableHashcodeFieldsRuleScanner() {
                super();
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                boolean overridesHashCode = node.getMembers().stream()
                        .filter(member -> member instanceof MethodTree method)
                        .map(member -> (MethodTree) member)
                        .anyMatch(method -> method.getName().contentEquals("hashCode")
                                && method.getParameters().isEmpty());
                if (overridesHashCode) {
                    for (Tree member : node.getMembers()) {
                        if (member instanceof VariableTree field) {
                            var flags = field.getModifiers().getFlags();
                            if (!flags.contains(Modifier.STATIC) && !flags.contains(Modifier.FINAL)) {
                                violations.add(violation(context, field, Severity.WARNING,
                                        "Field '" + field.getName() + "' is mutable but the class overrides "
                                                + "hashCode(); mutating it breaks hash-based collections "
                                                + "(make it final or use an immutable key)"));
                            }
                        }
                    }
                }
                return super.visitClass(node, unused);
            }
        }
        new NoMutableHashcodeFieldsRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
