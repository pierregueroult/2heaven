package engineering.pierre.linter;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Declaration-scoped, explicitly namespaced suppressions.
final class Suppressions {
    private Suppressions() {
        super();
    }

    static List<LintViolation> filter(LintContext context, List<LintViolation> violations) {
        if (!context.hasAst() || context.trees().isEmpty() || violations.isEmpty()) {
            return List.copyOf(violations);
        }
        List<Scope> scopes = new ArrayList<>();
        final class DeclarationScanner extends TreeScanner<Void, Void> {
            @Override
            public Void scan(Tree node, Void unused) {
                ModifiersTree modifiers = switch (node) {
                    case ClassTree declaration -> declaration.getModifiers();
                    case MethodTree declaration -> declaration.getModifiers();
                    case VariableTree declaration -> declaration.getModifiers();
                    case null, default -> null;
                };
                if (modifiers != null) {
                    Set<String> rules = suppressedRules(modifiers);
                    if (!rules.isEmpty()) {
                        var positions = context.trees().get().getSourcePositions();
                        scopes.add(new Scope(positions.getStartPosition(node),
                                positions.getEndPosition(node), rules));
                    }
                }
                return super.scan(node, unused);
            }
        }
        new DeclarationScanner().scan(context.compilationUnit().get(), null);
        return violations.stream().filter(violation -> {
            if (violation.line() < 1 || violation.column() < 1) {
                return true;
            }
            long position = context.compilationUnit().get().getLineMap()
                    .getPosition(violation.line(), violation.column());
            return scopes.stream().noneMatch(scope -> position >= scope.start() && position < scope.end()
                    && (scope.rules().contains(violation.ruleName()) || scope.rules().contains("all")));
        }).toList();
    }

    private static Set<String> suppressedRules(ModifiersTree modifiers) {
        Set<String> rules = new HashSet<>();
        for (var annotation : modifiers.getAnnotations()) {
            String name = annotation.getAnnotationType().toString();
            if (name.equals("SuppressWarnings") || name.equals("java.lang.SuppressWarnings")) {
                final class ValueScanner extends TreeScanner<Void, Void> {
                    @Override
                    public Void visitLiteral(LiteralTree literal, Void unused) {
                        if (literal.getValue() instanceof String value
                                && value.startsWith("2heaven:")) {
                            rules.add(value.substring("2heaven:".length()));
                        }
                        return super.visitLiteral(literal, unused);
                    }
                }
                new ValueScanner().scan(annotation.getArguments(), null);
            }
        }
        return Set.copyOf(rules);
    }

    private record Scope(long start, long end, Set<String> rules) {
    }
}
