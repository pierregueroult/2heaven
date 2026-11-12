package engineering.pierre.linter.rule;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Flags `==` / `!=` between two reference-typed operands: it compares object
/// identity, not contents. Course source: 4-objets prédéfinis.pdf —
/// `s == "toto"` is true only for the interned literal while `t == "toto"`
/// is false for an equal runtime-built String; use `equals()`.
///
/// Exempt: comparisons against `null`, enum constants (identity is the
/// correct semantics for singletons), java.lang.Class objects, primitive/unboxing comparisons, and
/// operands whose type could not be attributed.
///
/// Type attribution is done on a private parse+analyze pass: the shared
/// [engineering.pierre.linter.SourceParser] stays parse-only because
/// `analyze()` rewrites the AST (e.g. it injects a synthetic `super()` into
/// empty constructors), which would corrupt the other rules.
public final class NoReferenceIdentityComparisonRule implements Rule {

    private static final Set<TypeKind> PRIMITIVE_KINDS = Set.of(
            TypeKind.BOOLEAN, TypeKind.BYTE, TypeKind.SHORT,
            TypeKind.INT, TypeKind.LONG, TypeKind.CHAR,
            TypeKind.FLOAT, TypeKind.DOUBLE);

    @Override
    public String name() {
        return "no-reference-identity-comparison";
    }

    @Override
    public String description() {
        return "Do not use == or != on objects (String, collections, ...); "
                + "it compares references — use equals().";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return violations;
        }
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(
                    null, fileManager, new DiagnosticCollector<>(), List.of("-proc:none"), null,
                    fileManager.getJavaFileObjects(context.path().toFile()));
            CompilationUnitTree unit = task.parse().iterator().next();
            try {
                task.analyze();
            } catch (IllegalStateException e) {
                throw new IllegalStateException("Type attribution failed for " + context.path(), e);
            }
            Trees trees = Trees.instance(task);
            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitBinary(BinaryTree node, Void unused) {
                    var kind = node.getKind();
                    if (kind == Tree.Kind.EQUAL_TO || kind == Tree.Kind.NOT_EQUAL_TO) {
                        TypeMirror left = typeOf(node.getLeftOperand());
                        TypeMirror right = typeOf(node.getRightOperand());
                        if (isReferenceType(left) && isReferenceType(right)) {
                            long position = trees.getSourcePositions().getStartPosition(node);
                            var lineColumn = context.lineColumn(position);
                            violations.add(new LintViolation(context.path(), name(), Severity.WARNING,
                                    lineColumn.line(), lineColumn.column(),
                                    (kind == Tree.Kind.EQUAL_TO ? "==" : "!=")
                                            + " compares object references, not contents; use equals()"));
                        }
                    }
                    return super.visitBinary(node, unused);
                }

                private TypeMirror typeOf(Tree operand) {
                    if (operand instanceof LiteralTree literal && literal.getValue() == null) {
                        return null; // null check is idiomatic and safe
                    }
                    return trees.getTypeMirror(new TreePath(getCurrentPath(), operand));
                }

                private boolean isReferenceType(TypeMirror type) {
                    if (type == null) {
                        return false;
                    }
                    var kind = type.getKind();
                    if (PRIMITIVE_KINDS.contains(kind) || kind == TypeKind.VOID
                            || kind == TypeKind.NONE || kind == TypeKind.ERROR
                            || kind == TypeKind.TYPEVAR) {
                        return false;
                    }
                    if (kind == TypeKind.DECLARED && type instanceof DeclaredType declared) {
                        var element = declared.asElement();
                        if (element.getKind() == ElementKind.ENUM
                                || element instanceof TypeElement typeElement
                                && typeElement.getQualifiedName().contentEquals("java.lang.Class")) {
                            return false; // enums and Class objects have identity semantics
                        }
                    }
                    return kind == TypeKind.DECLARED || kind == TypeKind.ARRAY;
                }
            }.scan(unit, null);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + context.path(), e);
        }
        return violations;
    }
}
