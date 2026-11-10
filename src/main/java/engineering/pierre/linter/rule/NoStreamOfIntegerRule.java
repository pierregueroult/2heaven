package engineering.pierre.linter.rule;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags every `Stream<Integer>` — including `java.util.stream.Stream<Integer>`
/// and occurrences that only appear as an intermediate step of a pipeline —
/// wherever the type is written: variable, parameter and return types,
/// casts and explicit type witnesses (`Stream.<Integer>of(...)`).
///
/// A stream of integers should always be an [java.util.stream.IntStream]
/// (`mapToInt`, `IntStream.range`, ...), which avoids boxing entirely.
public final class NoStreamOfIntegerRule implements Rule {

    @Override
    public String name() {
        return "no-stream-of-integer";
    }

    @Override
    public String description() {
        return "Use IntStream instead of Stream<Integer>, including as an "
                + "intermediate step of a stream pipeline.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitParameterizedType(ParameterizedTypeTree node, Void unused) {
                if ("Stream".equals(TypeNames.simpleName(node.getType()))
                        && node.getTypeArguments().size() == 1
                        && "Integer".equals(TypeNames.simpleName(node.getTypeArguments().getFirst()))) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Stream<Integer> boxes every element; use IntStream instead "
                                    + "(e.g. mapToInt, IntStream.range)"));
                }
                return super.visitParameterizedType(node, unused);
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                // Explicit type witness on a Stream factory: `Stream.<Integer>of(...)`.
                if (node.getMethodSelect() instanceof MemberSelectTree select
                        && "Stream".equals(TypeNames.simpleName(select.getExpression()))
                        && node.getTypeArguments().stream()
                                .anyMatch(arg -> "Integer".equals(TypeNames.simpleName(arg)))) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Stream.<Integer> boxes every element; use IntStream instead "
                                    + "(e.g. IntStream.of)"));
                }
                return super.visitMethodInvocation(node, unused);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
