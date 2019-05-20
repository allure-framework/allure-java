/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.cucumber4jvm;

import cucumber.api.TestCase;
import gherkin.ast.Feature;
import gherkin.pickles.PickleTag;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.qameta.allure.util.ResultsUtils.createFeatureLabel;
import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createStoryLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createTestClassLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;

/**
 * Scenario labels and links builder.
 */
@SuppressWarnings({"CyclomaticComplexity", "PMD.CyclomaticComplexity", "PMD.NcssCount", "MultipleStringLiterals"})
class LabelBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(LabelBuilder.class);
    private static final String COMPOSITE_TAG_DELIMITER = "=";

    private static final String SEVERITY = "@SEVERITY";
    private static final String ISSUE_LINK = "@ISSUE";
    private static final String TMS_LINK = "@TMSLINK";
    private static final String PLAIN_LINK = "@LINK";

    private final List<Label> scenarioLabels = new ArrayList<>();
    private final List<Link> scenarioLinks = new ArrayList<>();

    LabelBuilder(final Feature feature, final TestCase scenario, final Deque<PickleTag> tags) {
        final TagParser tagParser = new TagParser(feature, scenario);

        while (tags.peek() != null) {
            final PickleTag tag = tags.remove();

            final String tagString = tag.getName();

            if (tagString.contains(COMPOSITE_TAG_DELIMITER)) {

                final String[] tagParts = tagString.split(COMPOSITE_TAG_DELIMITER, 2);
                if (tagParts.length < 2 || Objects.isNull(tagParts[1]) || tagParts[1].isEmpty()) {
                    // skip empty tags, e.g. '@tmsLink=', to avoid formatter errors
                    continue;
                }

                final String tagKey = tagParts[0].toUpperCase();
                final String tagValue = tagParts[1];

                // Handle composite named links
                if (tagKey.startsWith(PLAIN_LINK + ".")) {
                    tryHandleNamedLink(tagString);
                    continue;
                }

                switch (tagKey) {
                    case SEVERITY:
                        getScenarioLabels().add(ResultsUtils.createSeverityLabel(tagValue.toLowerCase()));
                        break;
                    case TMS_LINK:
                        getScenarioLinks().add(ResultsUtils.createTmsLink(tagValue));
                        break;
                    case ISSUE_LINK:
                        getScenarioLinks().add(ResultsUtils.createIssueLink(tagValue));
                        break;
                    case PLAIN_LINK:
                        getScenarioLinks().add(ResultsUtils.createLink(null, tagValue, tagValue, null));
                        break;
                    default:
                        LOGGER.warn("Composite tag {} is not supported. adding it as RAW", tagKey);
                        getScenarioLabels().add(getTagLabel(tag));
                        break;
                }
            } else if (tagParser.isPureSeverityTag(tag)) {
                getScenarioLabels().add(ResultsUtils.createSeverityLabel(tagString.substring(1)));
            } else if (!tagParser.isResultTag(tag)) {
                getScenarioLabels().add(getTagLabel(tag));
            }
        }

        final String featureName = feature.getName();
        final String uri = scenario.getUri();

        getScenarioLabels().addAll(Arrays.asList(
                createHostLabel(),
                createThreadLabel(),
                createFeatureLabel(featureName),
                createStoryLabel(scenario.getName()),
                createPackageLabel(featurePackage(uri, featureName)),
                createSuiteLabel(featureName),
                createTestClassLabel(scenario.getName()),
                createFrameworkLabel("cucumber4jvm"),
                createLanguageLabel("java"),
                createLabel("gherkin_uri", uri)
        ));
    }

    public List<Label> getScenarioLabels() {
        return scenarioLabels;
    }

    public List<Link> getScenarioLinks() {
        return scenarioLinks;
    }

    private Label getTagLabel(final PickleTag tag) {
        return ResultsUtils.createTagLabel(tag.getName().substring(1));
    }

    /**
     * Handle composite named links.
     *
     * @param tagString Full tag name and value
     */
    private void tryHandleNamedLink(final String tagString) {
        final String namedLinkPatternString = PLAIN_LINK + "\\.(\\w+-?)+=(\\w+(-|_)?)+";
        final Pattern namedLinkPattern = Pattern.compile(namedLinkPatternString, Pattern.CASE_INSENSITIVE);

        if (namedLinkPattern.matcher(tagString).matches()) {
            final String type = tagString.split(COMPOSITE_TAG_DELIMITER)[0].split("[.]")[1];
            final String name = tagString.split(COMPOSITE_TAG_DELIMITER)[1];
            getScenarioLinks().add(ResultsUtils.createLink(null, name, null, type));
        } else {
            LOGGER.warn("Composite named tag {} does not match regex {}. Skipping", tagString,
                    namedLinkPatternString);
        }
    }

    private String featurePackage(final String uri, final String featureName) {
        final Path parent = Paths.get(uri).getParent();
        if (Objects.nonNull(parent)) {
            final Stream<String> folders = StreamSupport.stream(parent.spliterator(), false)
                    .map(Path::toString);
            final Stream<String> name = Stream.of(featureName);
            return Stream.concat(folders, name).collect(Collectors.joining("."));
        }
        return featureName;
    }

}
