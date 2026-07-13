/*
 *  Copyright 2016-2026 Qameta Software Inc
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
package io.qameta.allure.karate;

import io.karatelabs.core.RunEvent;
import io.karatelabs.core.RunListener;
import io.karatelabs.core.ScenarioResult;
import io.karatelabs.core.ScenarioRunEvent;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.core.StepResult;
import io.karatelabs.core.StepRunEvent;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Tag;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.createLabel;
import static io.qameta.allure.util.ResultsUtils.createLink;
import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.createTitlePath;
import static io.qameta.allure.util.ResultsUtils.createTitlePathFromSourcePath;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Reports Karate runtime events to Allure.
 *
 * <p>Register this listener with Karate so features, scenarios, steps, and attachments are converted into Allure
 * results. The listener uses the Allure lifecycle to write standard result files.</p>
 */
@SuppressWarnings({"MultipleStringLiterals", "PMD.GodClass"})
public class AllureKarate implements RunListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureKarate.class);

    private static final String BUILD_RESOURCES = "build/resources/";

    private final AllureLifecycle lifecycle;

    private final Map<ScenarioRuntime, String> testCaseUuids = new ConcurrentHashMap<>();

    /**
     * Creates an Allure karate with default configuration.
     */
    public AllureKarate() {
        this(Allure.getLifecycle());
    }

    /**
     * Creates an Allure karate with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public AllureKarate(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onEvent(final RunEvent event) {
        switch (event.getType()) {
            case SCENARIO_ENTER:
                return beforeScenario(((ScenarioRunEvent) event).source());
            case SCENARIO_EXIT:
                afterScenario((ScenarioRunEvent) event);
                return true;
            case STEP_ENTER:
                return beforeStep((StepRunEvent) event);
            case STEP_EXIT:
                afterStep((StepRunEvent) event);
                return true;
            default:
                return true;
        }
    }

    private boolean beforeScenario(final ScenarioRuntime sr) {
        final Scenario scenario = sr.getScenario();
        final Feature feature = scenario.getFeature();
        final String featureName = feature.getName();
        final String featureNameQualified = getFeatureNameQualified(feature);

        final String uuid = UUID.randomUUID().toString();
        testCaseUuids.put(sr, uuid);

        final String nameOrLine = getName(scenario, String.valueOf(scenario.getLine()));
        final String testCaseId = md5(String.format("%s:%s", featureNameQualified, nameOrLine));
        final String fullName = String.format("%s:%d", featureNameQualified, scenario.getLine());
        final List<String> titlePath = createTitlePathFromSourcePath(featureNameQualified);
        titlePath.addAll(createTitlePath(featureName));
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setFullName(fullName)
                .setName(getName(scenario, fullName))
                .setDescription(getDescription(scenario))
                .setTestCaseId(testCaseId)
                .setTitlePath(titlePath);

        final List<String> labels = getTagTexts(scenario);
        final List<Label> allLabels = getLabels(labels);
        allLabels.add(ResultsUtils.createFeatureLabel(featureName));
        result.setLabels(allLabels);

        final List<Link> links = getLinks(labels);
        if (!links.isEmpty()) {
            result.setLinks(links);
        }

        final AllureExternalKey testKey = testKey(uuid);
        lifecycle.scheduleTest(testKey, result);
        lifecycle.startTest(testKey);
        return true;
    }

    private static AllureExternalKey testKey(final String scenarioUuid) {
        return AllureExternalKey.of(AllureKarate.class, "test", scenarioUuid);
    }

    private static AllureExternalKey stepKey(final String scenarioUuid, final int stepIndex) {
        return AllureExternalKey.of(AllureKarate.class, "step", scenarioUuid, stepIndex);
    }

    private static String getName(final Scenario scenario, final String defaultValue) {
        if (Objects.isNull(scenario.getName()) || scenario.getName().trim().startsWith("#")) {
            return defaultValue;
        }
        final boolean blank = scenario.getName().chars()
                .allMatch(Character::isWhitespace);
        return blank ? defaultValue : scenario.getName().trim();
    }

    private static String getDescription(final Scenario scenario) {
        final String description = scenario.getDescription();
        if (Objects.isNull(description)) {
            return "";
        }
        return description.lines()
                .filter(line -> !line.trim().startsWith("#"))
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private static String getFeatureNameQualified(final Feature feature) {
        final String path = feature.getResource().getRelativePath().replace('\\', '/');
        final int resourcesIndex = path.indexOf(BUILD_RESOURCES);
        if (resourcesIndex < 0) {
            return path;
        }
        final int sourceSetIndex = path.indexOf('/', resourcesIndex + BUILD_RESOURCES.length());
        return sourceSetIndex < 0 ? path : path.substring(sourceSetIndex + 1);
    }

    private void afterScenario(final ScenarioRunEvent event) {
        final ScenarioRuntime sr = event.source();
        final String uuid = testCaseUuids.remove(sr);
        if (Objects.isNull(uuid)) {
            return;
        }
        final Optional<ScenarioResult> maybeResult = Optional.ofNullable(event.result());

        final Status status = maybeResult
                .filter(result -> !result.isFailed())
                .isPresent()
                        ? Status.PASSED
                        : maybeResult
                                .map(ScenarioResult::getError)
                                .flatMap(ResultsUtils::getStatus)
                                .orElse(null);

        final StatusDetails statusDetails = maybeResult
                .map(ScenarioResult::getError)
                .flatMap(ResultsUtils::getStatusDetails)
                .orElse(null);

        final List<Parameter> list = new ArrayList<>();
        if (event.result() != null && event.result().getScenario().getExampleIndex() > -1) {
            final Map<String, Object> data = event.result().getScenario().getExampleData();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                list.add(createParameter(entry.getKey(), entry.getValue()));
            }
        }

        final AllureExternalKey testKey = testKey(uuid);
        lifecycle.updateTest(testKey, tr -> {
            tr.setStatus(status);
            tr.setStatusDetails(statusDetails);
            tr.getParameters().addAll(list);
        });

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);
    }

    private boolean beforeStep(final StepRunEvent event) {
        final Step step = event.step();
        final String parentUuid = testCaseUuids.get(event.scenarioRuntime());
        if (Objects.isNull(parentUuid)) {
            return true;
        }

        if (isCallStep(step)) {
            return true;
        }

        final io.qameta.allure.model.StepResult stepResult = new io.qameta.allure.model.StepResult()
                .setName(getStepName(step));

        lifecycle.startStep(testKey(parentUuid), stepKey(parentUuid, step.getIndex()), stepResult);

        return true;
    }

    private void afterStep(final StepRunEvent event) {
        final StepResult result = event.result();
        final String parentUuid = testCaseUuids.get(event.scenarioRuntime());
        if (Objects.isNull(parentUuid)) {
            return;
        }

        final Step step = result.getStep();
        if (isCallStep(step)) {
            return;
        }

        final AllureExternalKey stepKey = stepKey(parentUuid, step.getIndex());

        final Status status = !result.isFailed()
                ? Status.PASSED
                : Optional.of(result)
                        .map(StepResult::getError)
                        .flatMap(ResultsUtils::getStatus)
                        .orElse(null);

        final StatusDetails statusDetails = Optional.of(result)
                .map(StepResult::getError)
                .flatMap(ResultsUtils::getStatusDetails)
                .orElse(null);

        lifecycle.updateStep(stepKey, s -> {
            s.setStatus(status);
            s.setStatusDetails(statusDetails);
        });

        if (Objects.nonNull(result.getEmbeds())) {
            result.getEmbeds().forEach(embed -> {
                final byte[] data = embed.getData();
                if (data == null) {
                    return;
                }
                try {
                    lifecycle.addAttachment(
                            stepKey,
                            embed.getName(),
                            embed.getMimeType(),
                            new ByteArrayInputStream(data),
                            AttachmentOptions.empty()
                    );
                } catch (RuntimeException e) {
                    LOGGER.warn("could not save embedding", e);
                }
            });
        }

        lifecycle.stopStep(stepKey);

    }

    private static boolean isCallStep(final Step step) {
        return "call".equals(step.getKeyword()) || "callonce".equals(step.getKeyword());
    }

    private static String getStepName(final Step step) {
        if (Objects.isNull(step.getKeyword())) {
            return step.getText();
        }
        if (Objects.isNull(step.getText()) || step.getText().isBlank()) {
            return step.getKeyword();
        }
        return step.getKeyword() + " " + step.getText();
    }

    private static List<String> getTagTexts(final Scenario scenario) {
        return scenario.getTagsEffective().stream()
                .map(Tag::getText)
                .toList();
    }

    private List<Label> getLabels(final List<String> labels) {
        final Map<String, String> allureLabels = new HashMap<>();
        final List<Label> allLabels = new ArrayList<>();
        for (String tag : labels.stream()
                .filter(l -> l.contains("allure")).collect(Collectors.toList())) {
            final String tagName = tag.substring(0, tag.indexOf(':'));
            final String tagValue = tag.substring(tag.indexOf(':') + 1);
            if (tagName.contains("allure.label")) {
                allureLabels.put(
                        tagName.substring("allure.label.".length()),
                        tagValue
                );
            }
            if (tagName.contains("allure.id")) {
                allureLabels.put("AS_ID", tagValue);
            }
            if (tagName.contains("allure.severity")) {
                allureLabels.put("severity", tagValue);
            }
        }
        allureLabels.keySet().forEach(key -> allLabels.add(createLabel(key, allureLabels.get(key))));
        return allLabels;
    }

    private List<Link> getLinks(final List<String> labels) {
        final List<Link> allureLinks = new ArrayList<>();
        for (String tag : labels.stream()
                .filter(l -> l.contains("allure.link")).collect(Collectors.toList())) {
            final String tagName = tag.substring(0, tag.indexOf(':'));
            final String tagValue = tag.substring(tag.indexOf(':') + 1);
            switch (tagName.substring("allure.link".length())) {
                case "":
                    allureLinks.add(createLink(tagValue, "", "", "custom"));
                    break;
                case ".tms":
                    allureLinks.add(createLink(tagValue, "", "", "tms"));
                    break;
                case ".issue":
                    allureLinks.add(createLink(tagValue, "", "", "issue"));
                    break;
                default:
                    break;
            }
        }
        return allureLinks;
    }
}
