package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/// Flags non-delegating constructors — those that do not delegate to another
/// constructor via `this(...)` — whose last statement is not an explicit
/// `super(...)` call.
///
/// Since Java 25 (flexible constructor bodies) `super(...)` may appear
/// anywhere in a constructor; this rule requires it to be the last
/// instruction. Enum and record constructors are exempt because the
/// language forbids an explicit `super(...)` there.
public final class NonDelegatingConstructorSuperLastRule implements Rule {

    @Override
    public String name() {
        return "non-delegating-constructor-super-last";
    }

    @Override
    public String description() {
        return "Every non-delegating constructor must call super() as its last statement.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            private final Deque<Tree.Kind> enclosingClassKinds = new ArrayDeque<>();

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                enclosingClassKinds.push(node.getKind());
                try {
                    return super.visitClass(node, unused);
                } finally {
                    enclosingClassKinds.pop();
                }
            }

            @Override
            public Void visitMethod(MethodTree node, Void unused) {
                if (node.getName().contentEquals("<init>")) {
                    checkConstructor(context, node, violations);
                }
                return super.visitMethod(node, unused);
            }

            private void checkConstructor(LintContext context, MethodTree constructor,
                                          List<LintViolation> violations) {
                if (constructor.getBody() == null || isEnumOrRecord()) {
                    return;
                }
                var statements = constructor.getBody().getStatements();
                if (statements.isEmpty()) {
                    violations.add(violation(context, constructor, Severity.WARNING,
                            "Non-delegating constructor must call super() as its last statement"));
                    return;
                }
                if (isConstructorInvocation(statements.getFirst(), "this")) {
                    return; // delegates to another constructor: not delegating
                }
                if (!isConstructorInvocation(statements.getLast(), "super")) {
                    violations.add(violation(context, constructor, Severity.WARNING,
                            "Non-delegating constructor must call super() as its last statement"));
                }
            }

            private boolean isEnumOrRecord() {
                Tree.Kind kind = enclosingClassKinds.peek();
                return kind == Tree.Kind.ENUM || kind == Tree.Kind.RECORD;
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }

    /// Matches an explicit `this(...)` / `super(...)` constructor invocation.
    private static boolean isConstructorInvocation(StatementTree statement, String name) {
        if (!(statement instanceof ExpressionStatementTree expression)
                || !(expression.getExpression() instanceof MethodInvocationTree invocation)) {
            return false;
        }
        return invocation.getMethodSelect() instanceof IdentifierTree identifier
                && identifier.getName().contentEquals(name);
    }
}
