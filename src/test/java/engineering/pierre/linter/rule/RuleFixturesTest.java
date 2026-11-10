package engineering.pierre.linter.rule;

import engineering.pierre.linter.Linter;
import engineering.pierre.linter.LintViolation;
import engineering.pierre.linter.Rule;
import engineering.pierre.linter.SourceParser;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every built-in rule must ship a fixture pair under
/// `src/test/resources/fixtures/<rule-name>/`: `bad.java` is flagged by the
/// rule, `good.java` is not. The same files are meant to be reused by the
/// documentation to show what is good and what is not.
final class RuleFixturesTest {

    @Test
    void everyRuleHasFixturesAndTheyBehaveAsExpected() {
        for (Rule rule : Linter.withBuiltInRules().rules()) {
            Path bad = fixture(rule, "bad.java");
            Path good = fixture(rule, "good.java");

            List<LintViolation> badViolations = rule.check(SourceParser.parse(bad));
            assertFalse(badViolations.isEmpty(),
                    "fixtures/" + rule.name() + "/bad.java should be flagged by " + rule.name());
            assertTrue(badViolations.stream().allMatch(v -> v.ruleName().equals(rule.name())),
                    "bad.java should only contain violations of " + rule.name());

            List<LintViolation> goodViolations = rule.check(SourceParser.parse(good));
            assertTrue(goodViolations.isEmpty(),
                    "fixtures/" + rule.name() + "/good.java should not be flagged by "
                            + rule.name() + " but got: " + goodViolations);
        }
    }

    private static Path fixture(Rule rule, String fileName) {
        String resource = "/fixtures/" + rule.name() + "/" + fileName;
        URL url = RuleFixturesTest.class.getResource(resource);
        assertNotNull(url, "Missing fixture " + resource + " for rule " + rule.name());
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot resolve fixture " + resource, e);
        }
    }
}
