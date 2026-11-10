package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;

/// Flags anonymous classes that declare exactly one method and no fields —
/// they can be replaced by a lambda or method reference. Course source:
/// 2-lambda.pdf and 3-classe internes & enumeration.pdf.
///
/// Without type attribution we cannot verify the supertype is a functional
/// interface, so this is a heuristic.
public final class NoAnonymousClassForLambdaRule implements Rule {

    @Override
    public String name() {
        return "no-anonymous-class-for-lambda";
    }

    @Override
    public String description() {
        return "An anonymous class with a single method and no fields can be a lambda or method reference.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitNewClass(NewClassTree node, Void unused) {
                ClassTree body = node.getClassBody();
                if (body != null) {
                    int methods = 0;
                    int fields = 0;
                    for (Tree member : body.getMembers()) {
                        if (member instanceof MethodTree) {
                            methods++;
                        } else if (member instanceof VariableTree) {
                            fields++;
                        }
                    }
                    if (methods == 1 && fields == 0) {
                        violations.add(violation(context, node, Severity.INFO,
                                "Single-method anonymous class; use a lambda or method reference"));
                    }
                }
                return super.visitNewClass(node, unused);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
