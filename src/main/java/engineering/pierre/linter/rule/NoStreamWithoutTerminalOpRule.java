package engineering.pierre.linter.rule;

import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Flags a stream pipeline used as a statement whose last call is an
/// intermediate operation: streams are lazy, so without a terminal operation
/// nothing is computed at all. Course source: 2-lambda.pdf ("si on n'appelle
/// pas d'opération finale, aucun calcul n'est fait").
public final class NoStreamWithoutTerminalOpRule implements Rule {

    private static final Set<String> STREAM_SOURCES = Set.of("stream", "parallelStream");
    private static final Set<String> INTERMEDIATE_OPS = Set.of(
            "map", "flatMap", "filter", "peek", "distinct", "sorted", "limit", "skip",
            "mapToInt", "mapToLong", "mapToDouble",
            "flatMapToInt", "flatMapToLong", "flatMapToDouble",
            "takeWhile", "dropWhile", "onClose");

    @Override
    public String name() {
        return "no-stream-without-terminal-op";
    }

    @Override
    public String description() {
        return "A stream pipeline without a terminal operation computes nothing "
                + "(streams are lazy); add collect/forEach/reduce/... or drop the pipeline.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class NoStreamWithoutTerminalOpRuleScanner extends TreeScanner<Void, Void> {
            NoStreamWithoutTerminalOpRuleScanner() {
                super();
            }

            @Override
            public Void visitExpressionStatement(ExpressionStatementTree node, Void unused) {
                Tree expression = node.getExpression();
                if (expression instanceof MethodInvocationTree invocation
                        && invocation.getMethodSelect() instanceof MemberSelectTree lastSelect
                        && INTERMEDIATE_OPS.contains(lastSelect.getIdentifier().toString())
                        && chainContainsStreamSource(lastSelect.getExpression())) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "stream pipeline ends with '" + lastSelect.getIdentifier()
                                    + "' (intermediate op) and no terminal operation: "
                                    + "the pipeline is lazy and computes nothing"));
                }
                return super.visitExpressionStatement(node, unused);
            }

            private boolean chainContainsStreamSource(Tree expression) {
                while (expression instanceof MethodInvocationTree invocation
                        && invocation.getMethodSelect() instanceof MemberSelectTree select) {
                    if (STREAM_SOURCES.contains(select.getIdentifier().toString())) {
                        return true;
                    }
                    expression = select.getExpression();
                }
                return false;
            }
        }
        new NoStreamWithoutTerminalOpRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
