package engineering.pierre.linter;

import engineering.pierre.linter.rule.ConstructorFieldInitRule;
import engineering.pierre.linter.rule.EnumSwitchMissingDefaultRule;
import engineering.pierre.linter.rule.EqualsOverloadNotOverrideRule;
import engineering.pierre.linter.rule.EqualsWithoutHashCodeRule;
import engineering.pierre.linter.rule.NoAnonymousClassForLambdaRule;
import engineering.pierre.linter.rule.NoBoxedLongOrIntRule;
import engineering.pierre.linter.rule.NoCatchAndRethrowSameRule;
import engineering.pierre.linter.rule.NoCatchGenericExceptionRule;
import engineering.pierre.linter.rule.NoDeprecatedThreadControlRule;
import engineering.pierre.linter.rule.NoEmptyCatchRule;
import engineering.pierre.linter.rule.NoEnumValuesInLoopRule;
import engineering.pierre.linter.rule.NoExtendsCollectionRule;
import engineering.pierre.linter.rule.NoMapGetNullCheckRule;
import engineering.pierre.linter.rule.NoMutableHashcodeFieldsRule;
import engineering.pierre.linter.rule.NoMutableStaticFieldRule;
import engineering.pierre.linter.rule.NoNullCollectionElementRule;
import engineering.pierre.linter.rule.NoNullCollectionReturnRule;
import engineering.pierre.linter.rule.NoPrintStackTraceRule;
import engineering.pierre.linter.rule.NoPublicMutableFieldsRule;
import engineering.pierre.linter.rule.NoRawTypesRule;
import engineering.pierre.linter.rule.NoReferenceIdentityComparisonRule;
import engineering.pierre.linter.rule.NoStreamJustForForEachRule;
import engineering.pierre.linter.rule.NoStreamOfIntegerRule;
import engineering.pierre.linter.rule.NoStreamSideEffectsRule;
import engineering.pierre.linter.rule.NoStreamWithoutTerminalOpRule;
import engineering.pierre.linter.rule.NoStringConcatInLoopRule;
import engineering.pierre.linter.rule.NoSwallowInterruptedExceptionRule;
import engineering.pierre.linter.rule.NoUncheckedCastRule;
import engineering.pierre.linter.rule.NoWaitNotifyOnThisRule;
import engineering.pierre.linter.rule.NoWaitOutsideWhileLoopRule;
import engineering.pierre.linter.rule.NonDelegatingConstructorSuperLastRule;
import engineering.pierre.linter.rule.PreferDefensiveCopyRule;
import engineering.pierre.linter.rule.PreferFinalClassRule;
import engineering.pierre.linter.rule.PreferInstanceofPatternRule;
import engineering.pierre.linter.rule.PreferTryWithResourcesRule;
import engineering.pierre.linter.rule.RequireFinalLockObjectRule;
import engineering.pierre.linter.rule.RequireNonNullObjectParamRule;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/// Runs a set of [Rule]s over Java source files.
///
/// Files are linted concurrently on virtual threads; results are sorted by
/// path, line and column so output is deterministic.
public final class Linter {

    private final List<Rule> rules;

    public Linter(Collection<? extends Rule> rules) {
        this.rules = List.copyOf(rules);
        super();
    }

    /// A linter pre-registered with all built-in rules.
    public static Linter withBuiltInRules() {
        return new Linter(List.of(
                new RequireNonNullObjectParamRule(),
                new NonDelegatingConstructorSuperLastRule(),
                new NoStreamSideEffectsRule(),
                new NoEmptyCatchRule(),
                new NoCatchGenericExceptionRule(),
                new NoPrintStackTraceRule(),
                new NoSwallowInterruptedExceptionRule(),
                new NoDeprecatedThreadControlRule(),
                new NoRawTypesRule(),
                new NoUncheckedCastRule(),
                new EqualsWithoutHashCodeRule(),
                new NoStringConcatInLoopRule(),
                new PreferTryWithResourcesRule(),
                new NoAnonymousClassForLambdaRule(),
                new NoMutableStaticFieldRule(),
                new ConstructorFieldInitRule(),
                new NoWaitNotifyOnThisRule(),
                new NoBoxedLongOrIntRule(),
                new NoStreamOfIntegerRule(),
                new PreferFinalClassRule(),
                new NoWaitOutsideWhileLoopRule(),
                new RequireFinalLockObjectRule(),
                new NoReferenceIdentityComparisonRule(),
                new NoPublicMutableFieldsRule(),
                new NoExtendsCollectionRule(),
                new EqualsOverloadNotOverrideRule(),
                new NoStreamJustForForEachRule(),
                new NoStreamWithoutTerminalOpRule(),
                new NoMapGetNullCheckRule(),
                new NoNullCollectionElementRule(),
                new NoNullCollectionReturnRule(),
                new PreferDefensiveCopyRule(),
                new NoEnumValuesInLoopRule(),
                new PreferInstanceofPatternRule(),
                new NoCatchAndRethrowSameRule(),
                new EnumSwitchMissingDefaultRule(),
                new NoMutableHashcodeFieldsRule()
        ));
    }

    public List<Rule> rules() {
        return List.copyOf(rules);
    }

    public List<LintViolation> lint(Path file) {
        return lint(List.of(file));
    }

    public List<LintViolation> lint(Collection<Path> files) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = files.stream()
                    .map(file -> executor.submit(() -> lintFile(file)))
                    .toList();
            List<LintViolation> violations = new ArrayList<>();
            for (var future : futures) {
                violations.addAll(await(future));
            }
            return violations.stream()
                    .sorted(Comparator.comparing(LintViolation::path)
                            .thenComparingInt(LintViolation::line)
                            .thenComparingInt(LintViolation::column))
                    .toList();
        }
    }

    /// Lints a single file: parses it, then applies every rule in order.
    public List<LintViolation> lintFile(Path file) {
        LintContext context = SourceParser.parse(file);
        List<LintViolation> violations = new ArrayList<>();
        for (Rule rule : rules) {
            violations.addAll(rule.check(context));
        }
        return Suppressions.filter(context, violations);
    }

    private static List<LintViolation> await(
            java.util.concurrent.Future<List<LintViolation>> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LintException("Linting interrupted", e);
        } catch (ExecutionException e) {
            throw new LintException("Linting failed: " + e.getCause().getMessage(), e.getCause());
        }
    }

    /// Signals an unrecoverable failure while linting.
    public static final class LintException extends RuntimeException {
        public LintException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
