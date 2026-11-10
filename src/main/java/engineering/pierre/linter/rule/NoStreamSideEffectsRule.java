package engineering.pierre.linter.rule;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Flags side effects in stream pipelines (including `parallelStream()`):
///
/// - `forEach`, `forEachOrdered` and `peek` calls on a stream — they exist
///   precisely to perform side effects;
/// - mutations of captured state (fields, array elements, `++`/`--`) inside
///   lambdas passed to stream operations.
///
/// A pipeline is detected syntactically: the receiver chain of the call
/// contains `stream()`, `parallelStream()`, `Arrays.stream(...)`, or a
/// static factory on a `*Stream` type (`Stream.of`, `IntStream.range`, ...).
/// Calls on variables that merely hold a stream cannot be detected without
/// type attribution.
public final class NoStreamSideEffectsRule implements Rule {

    private static final Set<String> STREAM_SOURCES = Set.of("stream", "parallelStream");
    private static final Set<String> SIDE_EFFECT_OPS = Set.of("forEach", "forEachOrdered", "peek");

    @Override
    public String name() {
        return "no-stream-side-effects";
    }

    @Override
    public String description() {
        return "Stream pipelines (including parallel streams) must be side-effect free: "
                + "no forEach/forEachOrdered/peek and no mutation of captured state in lambdas.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class PipelineScanner extends TreeScanner<Void, Void> {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                if (node.getMethodSelect() instanceof MemberSelectTree select
                        && receiverChainHasStreamSource(select.getExpression())) {
                    String operation = select.getIdentifier().toString();
                    if (SIDE_EFFECT_OPS.contains(operation)) {
                        violations.add(violation(context, node, Severity.WARNING,
                                "'" + operation + "' is a side effect in a stream pipeline; "
                                        + "use a side-effect-free operation such as collect"));
                    }
                    for (Tree argument : node.getArguments()) {
                        if (argument instanceof LambdaExpressionTree lambda) {
                            checkLambda(context, lambda, violations);
                        }
                    }
                }
                return super.visitMethodInvocation(node, unused);
            }
        }
        new PipelineScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }

    /// Walks the receiver chain of a method call looking for a stream source.
    private static boolean receiverChainHasStreamSource(Tree receiver) {
        while (receiver instanceof MethodInvocationTree invocation) {
            Tree methodSelect = invocation.getMethodSelect();
            if (!(methodSelect instanceof MemberSelectTree select)) {
                if (methodSelect instanceof IdentifierTree id
                        && STREAM_SOURCES.contains(id.getName().toString())) {
                    return true;
                }
                return false;
            }
            String name = select.getIdentifier().toString();
            if (STREAM_SOURCES.contains(name)) {
                return true;
            }
            if (select.getExpression() instanceof IdentifierTree id) {
                String receiverName = id.getName().toString();
                return receiverName.endsWith("Stream")
                        || (receiverName.equals("Arrays") && name.equals("stream"));
            }
            receiver = select.getExpression();
        }
        return false;
    }

    /// Flags mutations inside a stream lambda of anything not declared within
    /// the lambda itself — i.e. captured fields or array elements.
    private void checkLambda(LintContext context, LambdaExpressionTree lambda,
                             List<LintViolation> violations) {
        Set<String> declaredInLambda = declaredNames(lambda);
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitAssignment(AssignmentTree node, Void unused) {
                checkTarget(node.getVariable());
                return super.visitAssignment(node, unused);
            }

            @Override
            public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
                checkTarget(node.getVariable());
                return super.visitCompoundAssignment(node, unused);
            }

            @Override
            public Void visitUnary(UnaryTree node, Void unused) {
                if (node.getKind() == Tree.Kind.PREFIX_INCREMENT
                        || node.getKind() == Tree.Kind.PREFIX_DECREMENT
                        || node.getKind() == Tree.Kind.POSTFIX_INCREMENT
                        || node.getKind() == Tree.Kind.POSTFIX_DECREMENT) {
                    checkTarget(node.getExpression());
                }
                return super.visitUnary(node, unused);
            }

            private void checkTarget(Tree target) {
                if (target instanceof IdentifierTree id
                        && !declaredInLambda.contains(id.getName().toString())) {
                    violations.add(violation(context, target, Severity.WARNING,
                            "Stream lambda must not mutate captured variable '"
                                    + id.getName() + "'"));
                } else if (target instanceof ArrayAccessTree) {
                    violations.add(violation(context, target, Severity.WARNING,
                            "Stream lambda must not mutate array elements"));
                }
            }
        }.scan(lambda.getBody(), null);
    }

    /// Names declared inside the lambda: its parameters and every local
    /// variable declared in its body.
    private static Set<String> declaredNames(LambdaExpressionTree lambda) {
        Set<String> names = new HashSet<>();
        for (VariableTree param : lambda.getParameters()) {
            names.add(param.getName().toString());
        }
        final class BodyScanner extends TreeScanner<Void, Void> {
            @Override
            public Void visitVariable(VariableTree node, Void unused) {
                names.add(node.getName().toString());
                return super.visitVariable(node, unused);
            }
        }
        new BodyScanner().scan(lambda.getBody(), null);
        return names;
    }
}
