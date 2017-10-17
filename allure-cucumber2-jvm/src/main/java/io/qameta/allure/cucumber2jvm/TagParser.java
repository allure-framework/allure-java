package io.qameta.allure.cucumber2jvm;

import cucumber.api.TestCase;
import gherkin.ast.Feature;
import gherkin.ast.Scenario;
import gherkin.pickles.PickleTag;
import io.qameta.allure.SeverityLevel;

import java.util.Arrays;

/**
 * Parser for tags.
 */
class TagParser {
    private static final String FLAKY = "@FLAKY";
    private static final String KNOWN = "@KNOWN";
    private static final String MUTED = "@MUTED";

    private final Feature feature;
    private final TestCase scenario;

    TagParser(final Feature feature, final TestCase scenario) {
        this.feature = feature;
        this.scenario = scenario;
    }

    protected boolean isFlaky() {
        return getStatusDetailByTag(FLAKY);
    }

    protected boolean isMuted() {
        return getStatusDetailByTag(MUTED);
    }

    protected boolean isKnown() {
        return getStatusDetailByTag(KNOWN);
    }

    protected boolean getStatusDetailByTag(final String tagName) {
        return scenario.getTags().stream()
                .anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName))
                || feature.getTags().stream()
                .anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName));
    }

    protected boolean isResultTag(final PickleTag tag) {
        return Arrays.asList(new String[]{FLAKY, KNOWN, MUTED})
                .contains(tag.getName().toUpperCase());
    }

    protected boolean isPureSeverityTag(final PickleTag tag) {
        return Arrays.stream(SeverityLevel.values())
                .map(SeverityLevel::value)
                .map(value -> "@" + value)
                .anyMatch(value -> value.equalsIgnoreCase(tag.getName()));
    }

}
