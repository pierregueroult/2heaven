package engineering.pierre.linter.rule;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Flags boxed `Integer` and `Long` (including `java.lang.Integer` /
/// `java.lang.Long`) in every position where the primitive `int` / `long`
/// could be used instead: variable, parameter and field types, method
/// return types, casts, array element types and `new Integer(...)` /
/// `new Long(...)`.
///
/// Boxed types inside generic type arguments (e.g. `List<Integer>`) are
/// not flagged here — no primitive can appear there; `Stream<Integer>` is
/// covered by [NoStreamOfIntegerRule].
public final class NoBoxedLongOrIntRule implements Rule {

    private static final Map<String, String> BOXED_TO_PRIMITIVE = Map.of(
            "Integer", "int",
            "Long", "long");

    @Override
    public String name() {
        return "no-boxed-long-or-int";
    }

    @Override
    public String description() {
        return "Use the primitives int/long instead of the boxed Integer/Long "
                + "wherever a primitive is possible.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree node, Void unused) {
                checkType(node.getType(), violations, context);
                return super.visitVariable(node, unused);
            }

            @Override
            public Void visitMethod(MethodTree node, Void unused) {
                checkType(node.getReturnType(), violations, context);
                return super.visitMethod(node, unused);
            }

            @Override
            public Void visitTypeCast(TypeCastTree node, Void unused) {
                checkType(node.getType(), violations, context);
                return super.visitTypeCast(node, unused);
            }

            @Override
            public Void visitNewClass(NewClassTree node, Void unused) {
                String boxed = BOXED_TO_PRIMITIVE.get(TypeNames.simpleName(node.getIdentifier()));
                if (boxed != null) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Boxed '" + TypeNames.simpleName(node.getIdentifier())
                                    + "'; use primitive '" + boxed + "' instead"));
                }
                return super.visitNewClass(node, unused);
            }

            private void checkType(Tree type, List<LintViolation> violations, LintContext context) {
                if (type instanceof ArrayTypeTree array) {
                    checkType(array.getType(), violations, context);
                    return;
                }
                String boxed = BOXED_TO_PRIMITIVE.get(TypeNames.simpleName(type));
                if (boxed != null) {
                    violations.add(violation(context, type, Severity.WARNING,
                            "Boxed '" + TypeNames.simpleName(type)
                                    + "'; use primitive '" + boxed + "' instead"));
                }
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
