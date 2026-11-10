# 2heaven

A Java linter library (Maven artifact `engineering.pierre:2heaven`) with 19 built-in rules, usable both as a **library** and as a
**standalone command-line jar** — no build-tool plugin or code in the consuming
project required.

## Built-in rules

| Rule | Severity | What it flags |
|---|---|---|
| `require-non-null-object-param` | WARNING | Object parameters that should be `@NonNull` / null-checked |
| `non-delegating-constructor-super-last` | WARNING | Non-delegating constructors not calling `super()` last |
| `no-stream-side-effects` | WARNING | Side effects inside stream pipelines |
| `no-empty-catch` | WARNING | Empty `catch` blocks |
| `no-catch-generic-exception` | ERROR | Catching `Exception`/`Throwable`/`RuntimeException` |
| `no-print-stack-trace` | WARNING | `printStackTrace()` calls |
| `no-swallow-interrupted-exception` | WARNING | Swallowed `InterruptedException` |
| `no-deprecated-thread-control` | WARNING | Deprecated `Thread.stop/suspend/resume` |
| `no-raw-types` | WARNING | Raw types |
| `no-unchecked-cast` | WARNING | Unchecked casts |
| `equals-without-hashcode` | WARNING | `equals()` without `hashCode()` |
| `no-string-concat-in-loop` | WARNING | String concatenation in loops |
| `prefer-try-with-resources` | WARNING | Manual `close()` instead of try-with-resources |
| `no-anonymous-class-for-lambda` | INFO | Single-method anonymous classes usable as lambdas |
| `no-mutable-static-field` | WARNING | Mutable `static` fields |
| `constructor-field-init` | WARNING | Field initializers that belong in a constructor |
| `no-wait-notify-on-this` | WARNING | `wait()`/`notify()` on `this` |
| `no-boxed-long-or-int` | WARNING | Boxed `Integer`/`Long` where `int`/`long` can be used |
| `no-stream-of-integer` | WARNING | `Stream<Integer>` (even as an intermediate step) instead of `IntStream` |
| `prefer-final-class` | WARNING | Non-`final` classes not designed for inheritance (abstract, sealed and in-file-extended classes are exempt) |

Each rule ships a fixture pair under `src/test/resources/fixtures/<rule-name>/`
(`bad.java` is flagged, `good.java` is not). `RuleFixturesTest` verifies every
rule against its pair, and the same files are the source for rule examples in
the documentation.

## Use as a command-line tool (recommended for CI)

Download the jar from the [latest release](../../releases/latest) and run it —
no installation, no code in the consuming project:

```bash
curl -sL -o 2heaven.jar \
  https://github.com/pierregueroult/2heaven/releases/latest/download/2heaven-0.2.0.jar
java -jar 2heaven.jar src/main/java
```

- Accepts files and/or directories (directories are searched recursively for `.java`).
- Prints one `path:line:col: SEVERITY [rule] message` line per violation.
- Exit codes: `0` = no ERROR-severity violations, `1` = at least one ERROR
  (warnings/info are reported but don't fail), `2` = usage or IO error.

In GitHub Actions:

```yaml
- uses: actions/setup-java@v4
  with: { distribution: temurin, java-version: 27 }
- run: curl -sL -o 2heaven.jar https://github.com/pierregueroult/2heaven/releases/latest/download/2heaven-0.2.0.jar
- run: java -jar 2heaven.jar src/main/java
```

Pin the URL to a specific tag (e.g. `releases/download/v0.2.0/...`) for
reproducible builds. Requires Java 27+ on the runner.

## Use as a library

```xml
<dependency>
    <groupId>engineering.pierre</groupId>
    <artifactId>2heaven</artifactId>
    <version>0.2.0</version>
</dependency>
```

```java
import engineering.pierre.linter.Linter;

var violations = Linter.withBuiltInRules().lint(java.nio.file.Path.of("src/main/java/Foo.java"));
```

`lint(...)` accepts single files or collections and returns `LintViolation`s
sorted by path, line and column.

## Building and releasing

```bash
mvn package        # build + test (JDK 27)
```

Pushing a `v*` tag triggers the release workflow, which builds the executable
jar and attaches it to a GitHub release.

## Self-linting

This repository runs its own linter on its sources in CI (`.github/workflows/lint.yml`).
