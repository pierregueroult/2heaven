package engineering.pierre.linter.rule;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import javax.lang.model.element.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Flags a `switch` over an enum that has no `default` and does not cover all
/// constants: the compiler silently adds a default that does nothing, so a
/// newly added enum constant is swallowed ("le compilateur ajoute un
/// “default” ici, ahhhh !"). Course source: 1-concept.pdf. Exhaustive switches
/// (all constants covered, or with an explicit default) are accepted.
public final class EnumSwitchMissingDefaultRule implements Rule {

    @Override
    public String name() {
        return "enum-switch-missing-default";
    }

    @Override
    public String description() {
        return "A switch over an enum must have a default or cover all constants; "
                + "otherwise the compiler silently adds an empty default.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        final class EnumSwitchScanner extends TreeScanner<Void, Void> {
            private final Map<String, Set<String>> enumConstants;
            private final Map<String, String> variableTypes;
            private final Deque<ClassTree> classes;

            EnumSwitchScanner() {
                this.enumConstants = new HashMap<>();
                this.variableTypes = new HashMap<>();
                this.classes = new ArrayDeque<>();
                super();
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                if (node.getKind() == Tree.Kind.ENUM) {
                    String enumName = node.getSimpleName().toString();
                    Set<String> constants = new HashSet<>();
                    for (Tree member : node.getMembers()) {
                        if (member instanceof VariableTree field) {
                            var flags = field.getModifiers().getFlags();
                            if (flags.contains(Modifier.PUBLIC)
                                    && flags.contains(Modifier.STATIC)
                                    && flags.contains(Modifier.FINAL)) {
                                constants.add(field.getName().toString());
                            }
                        }
                    }
                    enumConstants.put(enumName, constants);
                }
                classes.push(node);
                try {
                    return super.visitClass(node, unused);
                } finally {
                    classes.pop();
                }
            }

            @Override
            public Void visitVariable(VariableTree node, Void unused) {
                variableTypes.put(node.getName().toString(), TypeNames.simpleName(node.getType()));
                return super.visitVariable(node, unused);
            }

            @Override
            public Void visitSwitch(SwitchTree node, Void unused) {
                String selectorType = selectorTypeName(node.getExpression());
                Set<String> constants = selectorType == null ? null : enumConstants.get(selectorType);
                if (constants != null && !constants.isEmpty()) {
                    Set<String> covered = new HashSet<>();
                    boolean hasDefault = false;
                    for (CaseTree caseTree : node.getCases()) {
                        for (var label : caseTree.getLabels()) {
                            if (label instanceof com.sun.source.tree.DefaultCaseLabelTree) {
                                hasDefault = true;
                            } else if (label instanceof com.sun.source.tree.ConstantCaseLabelTree) {
                                covered.add(label.toString());
                            }
                        }
                    }
                    if (!hasDefault && !covered.containsAll(constants)) {
                        violations.add(violation(context, node, Severity.WARNING,
                                "switch over enum '" + selectorType + "' has no default and misses "
                                        + missing(constants, covered)
                                        + "; the compiler silently adds an empty default"));
                    }
                }
                return super.visitSwitch(node, unused);
            }

            private String selectorTypeName(Tree expression) {
                while (expression instanceof com.sun.source.tree.ParenthesizedTree parens) {
                    expression = parens.getExpression();
                }
                if (expression instanceof IdentifierTree id) {
                    return variableTypes.get(id.getName().toString());
                }
                if (expression instanceof MemberSelectTree memberSelect) {
                    return variableTypes.get(memberSelect.getIdentifier().toString());
                }
                return null;
            }

            private String missing(Set<String> constants, Set<String> covered) {
                List<String> missing = constants.stream().filter(c -> !covered.contains(c)).toList();
                return String.join(", ", missing);
            }
        }
        new EnumSwitchScanner().scan(context.compilationUnit().get(), null);
        return violations;
    }
}
