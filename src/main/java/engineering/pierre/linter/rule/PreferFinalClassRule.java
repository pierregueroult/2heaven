package engineering.pierre.linter.rule;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Flags classes that are open for inheritance without a reason: inheritance
/// is no longer the default in modern Java, so a class should be `final`
/// unless it is explicitly designed to be extended. Exempt:
///
/// - `abstract`, `sealed` and `non-sealed` classes (explicit inheritance design),
/// - interfaces, enums, records and annotation types (not extensible classes;
///   enums and records are implicitly final),
/// - anonymous classes (no name, cannot be extended),
/// - classes extended by another class in the same file (making them `final`
///   would not compile).
public final class PreferFinalClassRule implements Rule {

    @Override
    public String name() {
        return "prefer-final-class";
    }

    @Override
    public String description() {
        return "Classes should be final unless designed for inheritance; "
                + "abstract, sealed and in-file-extended classes are exempt.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        Set<String> extendedInFile = extendedTypeNames(context);
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void unused) {
                String name = node.getSimpleName().toString();
                if (node.getKind() == Tree.Kind.CLASS
                        && !name.isEmpty()
                        && !extendedInFile.contains(name)
                        && isOpenForInheritance(node)) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Class '" + name + "' should be final unless designed for inheritance"));
                }
                return super.visitClass(node, unused);
            }

            private boolean isOpenForInheritance(ClassTree node) {
                var flags = node.getModifiers().getFlags();
                return !flags.contains(Modifier.FINAL)
                        && !flags.contains(Modifier.ABSTRACT)
                        && !flags.contains(Modifier.SEALED)
                        && !flags.contains(Modifier.NON_SEALED);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }

    /// Simple names of every type extended by a class declared in this file.
    private Set<String> extendedTypeNames(LintContext context) {
        Set<String> names = new HashSet<>();
        final class ExtendsScanner extends TreeScanner<Void, Void> {
            @Override
            public Void visitClass(ClassTree node, Void unused) {
                Tree extendsClause = node.getExtendsClause();
                if (extendsClause instanceof IdentifierTree identifier) {
                    names.add(identifier.getName().toString());
                } else if (extendsClause instanceof MemberSelectTree select) {
                    names.add(select.getIdentifier().toString());
                }
                return super.visitClass(node, unused);
            }
        }
        new ExtendsScanner().scan(context.compilationUnit().get(), null);
        return names;
    }
}
