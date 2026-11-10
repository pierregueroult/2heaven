package engineering.pierre.linter.rule;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Flags `collection.stream().forEach(...)`: creating a stream just to call
/// `forEach` is unnecessary — collections already have a `forEach` method.
/// Course source: 2-lambda.pdf ("Attention, ne pas créer un stream juste pour
/// appeler forEach").
public final class NoStreamJustForForEachRule implements Rule {

    private static final Set<String> STREAM_SOURCES = Set.of("stream", "parallelStream");
    private static final Set<String> FOR_EACH = Set.of("forEach", "forEachOrdered");

    @Override
    public String name() {
        return "no-stream-just-for-foreach";
    }

    @Override
    public String description() {
        return "Use collection.forEach(...) directly instead of collection.stream().forEach(...).";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class NoStreamJustForForEachRuleScanner extends TreeScanner<Void, Void> {
            NoStreamJustForForEachRuleScanner() {
                super();
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                if (node.getMethodSelect() instanceof MemberSelectTree memberSelect
                        && FOR_EACH.contains(memberSelect.getIdentifier().toString())
                        && memberSelect.getExpression() instanceof MethodInvocationTree source
                        && source.getMethodSelect() instanceof MemberSelectTree sourceSelect
                        && STREAM_SOURCES.contains(sourceSelect.getIdentifier().toString())) {
                    violations.add(violation(context, node, Severity.WARNING,
                            memberSelect.getIdentifier() + "() on a fresh stream is useless; "
                                    + "call forEach directly on the collection"));
                } else if (node.getMethodSelect() instanceof IdentifierTree) {
                    // bare forEach(...) is a direct collection call: fine
                }
                return super.visitMethodInvocation(node, unused);
            }
        }
        new NoStreamJustForForEachRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
