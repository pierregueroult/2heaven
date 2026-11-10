# Agent instructions

## Adding a new lint rule

When you add a new rule, all four of these are required — a rule without them is incomplete:

1. **README**: add a row for the rule to the "Built-in rules" table in `README.md`.
2. **Test**: add a dedicated test class for the rule under `src/test/java/engineering/pierre/linter/rule/`.
3. **Fixture**: add a `bad.java` / `good.java` pair under `src/test/resources/fixtures/<rule-name>/`
   (`bad.java` must be flagged, `good.java` must be clean). `RuleFixturesTest` picks the pair up
   automatically, and the fixtures double as the examples used in the documentation.
4. **Self-lint**: the project lints its own sources in CI (`mvn package -DskipTests`, then
   `java -jar target/2heaven-*.jar src/main/java src/test/java`). All production and test code must
   pass every rule — **except the fixtures**, which intentionally violate rules to demonstrate them.

Run `mvn test` and the self-lint before considering the rule done.
