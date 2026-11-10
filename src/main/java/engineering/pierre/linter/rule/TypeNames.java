package engineering.pierre.linter.rule;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;

/// Helpers to extract simple names from type trees without type attribution.
final class TypeNames {

    private TypeNames() {
    }

    /// Simple name of a type tree: `String`, `java.lang.String` and
    /// `List<String>` all yield their bare name; arrays yield the element's.
    static String simpleName(Tree type) {
        if (type instanceof IdentifierTree identifier) {
            return identifier.getName().toString();
        }
        if (type instanceof MemberSelectTree memberSelect) {
            return memberSelect.getIdentifier().toString();
        }
        if (type instanceof ParameterizedTypeTree parameterized) {
            return simpleName(parameterized.getType());
        }
        if (type instanceof ArrayTypeTree array) {
            return simpleName(array.getType());
        }
        if (type instanceof PrimitiveTypeTree primitive) {
            return primitive.getPrimitiveTypeKind().toString().toLowerCase();
        }
        return "";
    }
}
