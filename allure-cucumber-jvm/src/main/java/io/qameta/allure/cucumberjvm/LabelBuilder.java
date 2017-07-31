package io.qameta.allure.cucumberjvm;

import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Tag;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static io.qameta.allure.util.ResultsUtils.createFeatureLabel;
import static io.qameta.allure.util.ResultsUtils.createSeverityLabel;
import static io.qameta.allure.util.ResultsUtils.createStoryLabel;
import static io.qameta.allure.util.ResultsUtils.createTagLabel;
import static io.qameta.allure.util.ResultsUtils.getHostName;
import static io.qameta.allure.util.ResultsUtils.getThreadName;

/**
 * Scenario labels and links builder.
 */
class LabelBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(LabelBuilder.class);
    private static final String COMPOSITE_TAG_DELIMITER = "=";

    private static final String SEVERITY = "@SEVERITY";
    private static final String ISSUE_LINK = "@ISSUE";
    private static final String TMS_LINK = "@TMSLINK";

    private final List<Label> scenarioLabels = new ArrayList<>();
    private final List<Link> scenarioLinks = new ArrayList<>();

    LabelBuilder(final Feature feature, final Scenario scenario, final Deque<Tag> tags) {
        final TagParser tagParser = new TagParser(feature, scenario);

        getScenarioLabels().add(createFeatureLabel(feature.getName()));
        getScenarioLabels().add(createStoryLabel(scenario.getName()));

        while (tags.peek() != null) {
            final Tag tag = tags.remove();

            final String tagString = tag.getName();

            if (tagString.contains(COMPOSITE_TAG_DELIMITER)) {

                final String tagKey = tagString.split(COMPOSITE_TAG_DELIMITER)[0].toUpperCase();
                final String tagValue = tagString.split(COMPOSITE_TAG_DELIMITER)[1];

                switch (tagKey) {
                    case SEVERITY:
                        getScenarioLabels().add(createSeverityLabel(tagValue.toLowerCase()));
                        break;
                    case TMS_LINK:
                        getScenarioLinks().add(ResultsUtils.createTmsLink(tagValue));
                        break;
                    case ISSUE_LINK:
                        getScenarioLinks().add(ResultsUtils.createIssueLink(tagValue));
                        break;
                    default:
                        LOGGER.warn("Composite tag {} is not supported. adding it as RAW", tagKey);
                        getScenarioLabels().add(getTagLabel(tag));
                        break;
                }
            } else if (tagParser.isPureSeverityTag(tag)) {
                getScenarioLabels().add(createSeverityLabel(tagString.substring(1)));
            } else if (!tagParser.isResultTag(tag)) {
                getScenarioLabels().add(getTagLabel(tag));
            }
        }

        getScenarioLabels().add(new Label().withName("host").withValue(getHostName()));
        getScenarioLabels().add(new Label().withName("package").withValue(feature.getName()));
        getScenarioLabels().add(new Label().withName("suite").withValue(feature.getName()));
        getScenarioLabels().add(new Label().withName("testClass").withValue(scenario.getName()));
        getScenarioLabels().add(new Label().withName("thread").withValue(getThreadName()));

    }

    private Label getTagLabel(final Tag tag) {
        return createTagLabel(tag.getName().substring(1));
    }

    public List<Label> getScenarioLabels() {
        return scenarioLabels;
    }

    public List<Link> getScenarioLinks() {
        return scenarioLinks;
    }
}
