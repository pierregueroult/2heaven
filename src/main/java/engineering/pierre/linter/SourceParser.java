package engineering.pierre.linter;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/// Parses Java source files into [LintContext]s using the JDK's own compiler
/// API (`jdk.compiler` module) — no third-party parser needed.
public final class SourceParser {

    private SourceParser() {
        super();
    }

    /// Parses [file], failing explicitly when Java syntax cannot be analyzed.
    public static LintContext parse(Path file) {
        String source = read(file);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new Linter.LintException("A full JDK with jdk.compiler is required", null);
        }
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjects(file);
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            JavacTask task = (JavacTask) compiler.getTask(
                    null, fileManager, diagnostics, List.of("-proc:none"), null, fileObjects);
            CompilationUnitTree compilationUnit = task.parse().iterator().next();
            var errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .map(d -> file + ":" + d.getLineNumber() + ":" + d.getColumnNumber()
                            + ": " + d.getMessage(java.util.Locale.ROOT))
                    .toList();
            if (!errors.isEmpty()) {
                throw new Linter.LintException(String.join("\n", errors), null);
            }
            return new LintContext(file, source, source.lines().toList(),
                    Optional.of(compilationUnit), Optional.of(Trees.instance(task)));
        } catch (IOException e) {
            throw new Linter.LintException("Cannot parse " + file, e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }
}
