package engineering.pierre.linter;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Everything a [Rule] needs to inspect one Java source file: the raw text,
/// its lines, and — when the file parsed successfully — its AST.
public record LintContext(
        Path path,
        String source,
        List<String> lines,
        Optional<CompilationUnitTree> compilationUnit,
        Optional<Trees> trees
) {
    public LintContext {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(source, "source");
        lines = List.copyOf(lines);
        Objects.requireNonNull(compilationUnit, "compilationUnit");
        Objects.requireNonNull(trees, "trees");
    }

    /// A context for a file that could not be parsed: source-based rules can
    /// still run, AST-based rules should bail out via [hasAst].
    public static LintContext sourceOnly(Path path, String source) {
        return new LintContext(path, source, source.lines().toList(),
                Optional.empty(), Optional.empty());
    }

    public boolean hasAst() {
        return compilationUnit.isPresent();
    }

    /// Translates an absolute source position into a 1-based line/column pair,
    /// or `(-1, -1)` when no AST is available or the position is unknown.
    public LineColumn lineColumnOf(Tree tree) {
        if (compilationUnit.isEmpty() || trees.isEmpty()) {
            return new LineColumn(-1, -1);
        }
        long position = trees.get().getSourcePositions().getStartPosition(tree);
        return lineColumn(position);
    }

    /// Translates an absolute source position into a 1-based line/column pair.
    public LineColumn lineColumn(long position) {
        if (compilationUnit.isEmpty() || position < 0) {
            return new LineColumn(-1, -1);
        }
        var lineMap = compilationUnit.get().getLineMap();
        return new LineColumn(
                (int) lineMap.getLineNumber(position),
                (int) lineMap.getColumnNumber(position));
    }

    /// A 1-based line/column pair.
    public record LineColumn(int line, int column) {
    }
}
