package engineering.pierre.linter.rule;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import engineering.pierre.linter.LintContext;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Flags raw uses of well-known generic types (`List l`, `new ArrayList()`,
/// `(List) o`, `extends HashMap`). Course source: 6- type et methode
/// parametre.pdf — raw types bypass generic type checking (erasure).
public final class NoRawTypesRule implements Rule {

    private static final Set<String> GENERIC_TYPES = Set.of(
            "List", "ArrayList", "LinkedList", "Set", "HashSet", "LinkedHashSet", "TreeSet",
            "Map", "HashMap", "LinkedHashMap", "TreeMap", "Queue", "Deque", "ArrayDeque",
            "Stack", "Vector", "Hashtable", "Collection", "Iterator", "ListIterator",
            "Comparator", "Optional", "Stream", "IntStream", "LongStream", "DoubleStream",
            "Function", "BiFunction", "Supplier", "Consumer", "BiConsumer", "Predicate",
            "BiPredicate");

    @Override
    public String name() {
        return "no-raw-types";
    }

    @Override
    public String description() {
        return "Do not use raw generic types; always provide type arguments.";
    }

    @Override
    public List<LintViolation> check(LintContext context) {
        List<LintViolation> violations = new ArrayList<>();
        if (context.compilationUnit().isEmpty()) {
            return violations;
        }
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree node, Void unused) {
                checkType(node.getType(), violations, context);
                return super.visitVariable(node, unused);
            }

            @Override
            public Void visitMethod(MethodTree node, Void unused) {
                checkType(node.getReturnType(), violations, context);
                for (VariableTree param : node.getParameters()) {
                    checkType(param.getType(), violations, context);
                }
                return super.visitMethod(node, unused);
            }

            @Override
            public Void visitNewClass(NewClassTree node, Void unused) {
                if (isRawGeneric(node.getIdentifier())) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Raw type '" + TypeNames.simpleName(node.getIdentifier())
                                    + "'; provide type arguments"));
                }
                return super.visitNewClass(node, unused);
            }

            @Override
            public Void visitTypeCast(TypeCastTree node, Void unused) {
                if (isRawGeneric(node.getType())) {
                    violations.add(violation(context, node, Severity.WARNING,
                            "Raw type '" + TypeNames.simpleName(node.getType())
                                    + "' in cast; provide type arguments"));
                }
                return super.visitTypeCast(node, unused);
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                checkType(node.getExtendsClause(), violations, context);
                for (Tree implemented : node.getImplementsClause()) {
                    checkType(implemented, violations, context);
                }
                return super.visitClass(node, unused);
            }

            private void checkType(Tree type, List<LintViolation> violations, LintContext context) {
                if (type != null && isRawGeneric(type)) {
                    violations.add(violation(context, type, Severity.WARNING,
                            "Raw type '" + TypeNames.simpleName(type) + "'; provide type arguments"));
                }
            }

            private boolean isRawGeneric(Tree type) {
                if (type instanceof ParameterizedTypeTree) {
                    return false;
                }
                if (type instanceof ArrayTypeTree array) {
                    return isRawGeneric(array.getType());
                }
                String name;
                if (type instanceof IdentifierTree id) {
                    name = id.getName().toString();
                } else if (type instanceof MemberSelectTree ms) {
                    name = ms.getIdentifier().toString();
                } else {
                    return false;
                }
                return GENERIC_TYPES.contains(name);
            }
        }.scan(context.compilationUnit().get(), null);
        return violations;
    }
}
