package engineering.pierre.linter.rule;

import com.sun.source.tree.CatchTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Flags catching overly broad exception types. Course source:
/// 5-Exceptions.pdf — catch the most specific exception possible.
public final class NoCatchGenericExceptionRule implements Rule {

    private static final Set<String> GENERIC_TYPES =
            Set.of("Throwable", "Exception", "RuntimeException");

    @Override
    public String name() {
        return "no-catch-generic-exception";
    }

    @Override
    public String description() {
        return "Do not catch Throwable, Exception or RuntimeException; catch a more specific type.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitCatch(CatchTree node, Void unused) {
                String typeName = TypeNames.simpleName(node.getParameter().getType());
                if (GENERIC_TYPES.contains(typeName)) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Catching '" + typeName + "' is too broad; catch a more specific exception"));
                }
                return super.visitCatch(node, unused);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
