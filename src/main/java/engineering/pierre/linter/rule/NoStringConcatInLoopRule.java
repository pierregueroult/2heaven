package engineering.pierre.linter.rule;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Flags String concatenation (`s += x` or `s = s + x`) inside loops — each
/// iteration allocates a new String; use a `StringBuilder` instead.
/// Course source: 4-objets predefinis.pdf (String is immutable, StringBuilder
/// is the mutable buffer).
///
/// Without type attribution, only variables explicitly declared as `String`
/// (locals or fields) are considered.
public final class NoStringConcatInLoopRule implements Rule {

    @Override
    public String name() {
        return "no-string-concat-in-loop";
    }

    @Override
    public String description() {
        return "Do not concatenate Strings with + or += inside a loop; use a StringBuilder.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            private final Set<String> stringVariables = new HashSet<>();
            private int loopDepth = 0;

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                for (Tree member : node.getMembers()) {
                    if (member instanceof VariableTree field
                            && TypeNames.simpleName(field.getType()).equals("String")) {
                        stringVariables.add(field.getName().toString());
                    }
                }
                try {
                    return super.visitClass(node, unused);
                } finally {
                    for (Tree member : node.getMembers()) {
                        if (member instanceof VariableTree field) {
                            stringVariables.remove(field.getName().toString());
                        }
                    }
                }
            }

            @Override
            public Void visitMethod(MethodTree node, Void unused) {
                Set<String> locals = new HashSet<>();
                if (node.getBody() != null) {
                    new TreeScanner<Void, Void>() {
                        @Override
                        public Void visitVariable(VariableTree var, Void unused) {
                            if (TypeNames.simpleName(var.getType()).equals("String")) {
                                locals.add(var.getName().toString());
                            }
                            return super.visitVariable(var, unused);
                        }
                    }.scan(node.getBody(), null);
                }
                stringVariables.addAll(locals);
                try {
                    return super.visitMethod(node, unused);
                } finally {
                    stringVariables.removeAll(locals);
                }
            }

            @Override
            public Void visitWhileLoop(WhileLoopTree node, Void unused) {
                loopDepth++;
                try {
                    return super.visitWhileLoop(node, unused);
                } finally {
                    loopDepth--;
                }
            }

            @Override
            public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
                loopDepth++;
                try {
                    return super.visitDoWhileLoop(node, unused);
                } finally {
                    loopDepth--;
                }
            }

            @Override
            public Void visitForLoop(ForLoopTree node, Void unused) {
                loopDepth++;
                try {
                    return super.visitForLoop(node, unused);
                } finally {
                    loopDepth--;
                }
            }

            @Override
            public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
                loopDepth++;
                try {
                    return super.visitEnhancedForLoop(node, unused);
                } finally {
                    loopDepth--;
                }
            }

            @Override
            public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
                if (loopDepth > 0
                        && node.getKind() == Tree.Kind.PLUS_ASSIGNMENT
                        && isStringVariable(node.getVariable())) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "String concatenation with += in a loop; use a StringBuilder"));
                }
                return super.visitCompoundAssignment(node, unused);
            }

            @Override
            public Void visitAssignment(AssignmentTree node, Void unused) {
                if (loopDepth > 0
                        && isStringVariable(node.getVariable())
                        && node.getExpression() instanceof BinaryTree binary
                        && binary.getKind() == Tree.Kind.PLUS) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "String concatenation with + in a loop; use a StringBuilder"));
                }
                return super.visitAssignment(node, unused);
            }

            private boolean isStringVariable(Tree variable) {
                return variable instanceof IdentifierTree id
                        && stringVariables.contains(id.getName().toString());
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
