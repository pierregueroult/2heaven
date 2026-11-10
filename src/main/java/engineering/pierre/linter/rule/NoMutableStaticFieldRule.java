package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
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

/// Flags non-`final` `static` fields: they are shared by all threads without
/// any visibility guarantee unless the field is also `volatile` or access is
/// synchronized. Course source: 14-Memory-model.pdf (only final/volatile/
/// synchronized give cross-thread visibility). Enum classes are exempt
/// (their constants are implicitly final).
public final class NoMutableStaticFieldRule implements Rule {

    @Override
    public String name() {
        return "no-mutable-static-field";
    }

    @Override
    public String description() {
        return "Static fields shared across threads must be final (or volatile); "
                + "a mutable static field has no visibility guarantees.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        class ClassScanner extends TreeScanner<Void, Void> {
            @Override
            public Void visitClass(ClassTree node, Void unused) {
                if (node.getKind() != Tree.Kind.ENUM) {
                    for (Tree member : node.getMembers()) {
                        if (member instanceof VariableTree field) {
                            var flags = field.getModifiers().getFlags();
                            if (flags.contains(Modifier.STATIC)
                                    && !flags.contains(Modifier.FINAL)
                                    && !flags.contains(Modifier.VOLATILE)) {
                                violations.add(violation(context, field, Severity.WARNING,
                                        "Mutable static field '" + field.getName()
                                                + "' is shared across threads without visibility guarantees"));
                            }
                        }
                    }
                }
                return super.visitClass(node, unused);
            }
        }
        new ClassScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
