package io.qameta.allure.cucumberjvm;

import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Tag;
import io.qameta.allure.ResultsUtils;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static io.qameta.allure.ResultsUtils.getHostName;
import static io.qameta.allure.ResultsUtils.getThreadName;

/**
 * Scenario labels and links builder.
 */
class LabelBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(LabelBuilder.class);
    private static final String COMPOSITE_TAG_DELIMITER = "=";

    private static final String SEVERITY = "@SEVERITY";
    private static final String ISSUE_LINK = "@ISSUE";
    private static final String TMS_LINK = "@TMSLINK";

    private final List<Label> scenarioLabels = new ArrayList<>();
    private final List<Link> scenarioLinks = new ArrayList<>();

    LabelBuilder(final Feature feature, final Scenario scenario, final Deque<Tag> tags) {
        final TagParser tagParser = new TagParser(feature, scenario);

        getScenarioLabels().add(getFeatureLabel(feature.getName()));
        getScenarioLabels().add(getStoryLabel(scenario.getName()));

        while (tags.peek() != null) {
            final Tag tag = tags.remove();

            final String tagString = tag.getName().toUpperCase();

            if (tagString.contains(COMPOSITE_TAG_DELIMITER)) {

                final String tagKey = tagString.split(COMPOSITE_TAG_DELIMITER)[0];
                final String tagValue = tagString.split(COMPOSITE_TAG_DELIMITER)[1];

                switch (tagKey) {
                    case SEVERITY:
                        getScenarioLabels().add(getSeverityLabel(tagValue));
                        break;
                    case TMS_LINK:
                        getScenarioLinks().add(ResultsUtils.createTmsLink(tagValue));
                        break;
                    case ISSUE_LINK:
                        getScenarioLinks().add(ResultsUtils.createIssueLink(tagValue));
                        break;
                    default:
                        LOG.warn("Composite tag {} is not supported. adding it as RAW", tagKey);
                        getScenarioLabels().add(getTagLabel(tag));
                        break;
                }
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

    private Label getFeatureLabel(final String featureName) {

        return ResultsUtils.createLabel(new io.qameta.allure.Feature() {
            @Override
            public String value() {
                return featureName;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return io.qameta.allure.Feature.class;
            }
        });
    }

    private Label getSeverityLabel(final String severity) {
        return ResultsUtils.createLabel(new Severity() {
            @Override
            public SeverityLevel value() {
                try {
                    return SeverityLevel.valueOf(severity);
                } catch (IllegalArgumentException e) {
                    LOG.warn("There is no severity level {} failing back to 'normal'", e);
                    return SeverityLevel.NORMAL;

                }
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Severity.class;
            }
        });
    }

    private Label getStoryLabel(final String storyName) {
        return ResultsUtils.createLabel(new Story() {
            @Override
            public String value() {
                return storyName;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Story.class;
            }
        });
    }

    private Label getTagLabel(final Tag tag) {
        return new Label().withName("tag").withValue(tag.getName().substring(1));
    }

    public List<Label> getScenarioLabels() {
        return scenarioLabels;
    }

    public List<Link> getScenarioLinks() {
        return scenarioLinks;
    }
}
