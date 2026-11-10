package engineering.pierre.linter.rule;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

import javax.lang.model.element.Modifier;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags public methods that declare a parameter of type `Object` without
/// guarding it with `Objects.requireNonNull(...)` anywhere in the body.
public final class RequireNonNullObjectParamRule implements Rule {

    @Override
    public String name() {
        return "require-non-null-object-param";
    }

    @Override
    public String description() {
        return "Public methods must call Objects.requireNonNull on every Object-typed parameter.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        class MethodScanner extends TreeScanner<Void, Void> {
            @Override
            public Void visitMethod(MethodTree node, Void unused) {
                checkMethod(context, node, violations);
                return super.visitMethod(node, unused);
            }
        }
        new MethodScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }

    private void checkMethod(LintContext context, MethodTree method,
                             List<LintViolation> violations) {
        if (method.getBody() == null || !isPublic(method)) {
            return;
        }
        for (VariableTree param : method.getParameters()) {
            if (isObjectType(param.getType())
                    && !hasRequireNonNull(method.getBody(), param.getName().toString())) {
                violations.add(violation(context, param, Severity.WARNING,
                        "Parameter '" + param.getName() + "' of public method '"
                                + method.getName() + "' is of type Object and must be "
                                + "guarded with Objects.requireNonNull"));
            }
        }
    }

    private static boolean isPublic(MethodTree method) {
        return method.getModifiers().getFlags().contains(Modifier.PUBLIC);
    }

    /// Matches a plain `Object` or fully-qualified `java.lang.Object` type.
    private static boolean isObjectType(Tree type) {
        if (type instanceof IdentifierTree identifier) {
            return identifier.getName().contentEquals("Object");
        }
        if (type instanceof MemberSelectTree memberSelect) {
            return memberSelect.getIdentifier().contentEquals("Object");
        }
        return false;
    }

    private static boolean hasRequireNonNull(BlockTree body, String paramName) {
        boolean[] found = {false};
        class RequireNonNullScanner extends TreeScanner<Void, Void> {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                if (isRequireNonNullOn(node, paramName)) {
                    found[0] = true;
                }
                return super.visitMethodInvocation(node, unused);
            }
        }
        new RequireNonNullScanner().scan(body, null);
        return found[0];
    }

    private static boolean isRequireNonNullOn(MethodInvocationTree node, String paramName) {
        if (!(node.getMethodSelect() instanceof MemberSelectTree select)
                || !(select.getExpression() instanceof IdentifierTree objects)
                || !objects.getName().contentEquals("Objects")
                || !select.getIdentifier().contentEquals("requireNonNull")) {
            return false;
        }
        return !node.getArguments().isEmpty()
                && node.getArguments().getFirst() instanceof IdentifierTree arg
                && arg.getName().contentEquals(paramName);
    }
}
