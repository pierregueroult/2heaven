package engineering.pierre.linter.rule;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Instance fields must be initialized in a constructor, never with an
/// inline initializer at the declaration site.
///
/// - Any non-static field with an inline initializer (`int x = 5;`) is
///   flagged: initialization belongs in the constructor.
/// - Non-static fields of a reference type (which would default to `null`)
///   must be assigned in a constructor; primitive fields are exempt because
///   they get a default value (`0`, `false`, ...).
///
/// Static fields, records and enums are exempt.
public final class ConstructorFieldInitRule implements Rule {

    @Override
    public String name() {
        return "constructor-field-init";
    }

    @Override
    public String description() {
        return "Initialize instance fields in the constructor, not inline; "
                + "only primitive fields may rely on their default value.";
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
                List<VariableTree> instanceFields = new ArrayList<>();
                Set<String> assignedInConstructor = new HashSet<>();
                for (Tree member : node.getMembers()) {
                    if (member instanceof VariableTree field
                            && !field.getModifiers().getFlags().contains(Modifier.STATIC)) {
                        instanceFields.add(field);
                    } else if (member instanceof MethodTree method
                            && method.getName().contentEquals("<init>")
                            && method.getBody() != null) {
                        assignedInConstructor.addAll(assignedNames(method));
                    }
                }
                for (VariableTree field : instanceFields) {
                    String fieldName = field.getName().toString();
                    if (field.getInitializer() != null) {
                        violations.add(violation(context, field, Severity.WARNING,
                                "Field '" + fieldName + "' must not be initialized inline; "
                                        + "assign it in a constructor"));
                    } else if (!(field.getType() instanceof PrimitiveTypeTree)
                            && !assignedInConstructor.contains(fieldName)) {
                        violations.add(violation(context, field, Severity.WARNING,
                                "Field '" + fieldName + "' of type '"
                                        + TypeNames.simpleName(field.getType())
                                        + "' must be initialized in a constructor"));
                    }
                }
            }

            /// Names assigned (`x = ...` or `this.x = ...`) in a constructor body.
            private Set<String> assignedNames(MethodTree constructor) {
                Set<String> names = new HashSet<>();
                new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitAssignment(AssignmentTree node, Void unused) {
                        Tree variable = node.getVariable();
                        if (variable instanceof IdentifierTree id) {
                            names.add(id.getName().toString());
                        } else if (variable instanceof MemberSelectTree select
                                && select.getExpression() instanceof IdentifierTree self
                                && self.getName().contentEquals("this")) {
                            names.add(select.getIdentifier().toString());
                        }
                        return super.visitAssignment(node, unused);
                    }
                }.scan(constructor.getBody(), null);
                return names;
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
