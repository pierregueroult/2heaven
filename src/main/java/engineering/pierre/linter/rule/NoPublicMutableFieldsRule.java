package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/// Flags `public` non-`final` fields: a public field exposes the internal
/// representation and lets any caller mutate it ("pas d'encapsulation ==
/// vue"). Course source: 1-concept.pdf (encapsulation) and
/// 7-Collections.pdf ("la meilleure façon de résoudre ce problème est de ne
/// pas avoir d'accesseur !").
public final class NoPublicMutableFieldsRule implements Rule {

    @Override
    public String name() {
        return "no-public-mutable-fields";
    }

    @Override
    public String description() {
        return "Public fields must be final (constants); use getters/setters for mutable state.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class NoPublicMutableFieldsRuleScanner extends TreeScanner<Void, Void> {
            NoPublicMutableFieldsRuleScanner() {
                super();
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                for (Tree member : node.getMembers()) {
                    if (member instanceof VariableTree field) {
                        var flags = field.getModifiers().getFlags();
                        if (flags.contains(Modifier.PUBLIC) && !flags.contains(Modifier.FINAL)) {
                            violations.add(violation(context, field, Severity.WARNING,
                                    "Public mutable field '" + field.getName()
                                            + "' breaks encapsulation; make it private with an accessor"
                                            + " or declare it final"));
                        }
                    }
                }
                return super.visitClass(node, unused);
            }
        }
        new NoPublicMutableFieldsRuleScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
