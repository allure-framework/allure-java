/*
 *  Copyright 2021 Qameta Software OÜ
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

import com.intuit.karate.RuntimeHook;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureRuntime;
import com.intuit.karate.core.Result;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioResult;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.core.Step;
import com.intuit.karate.core.StepResult;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.createLabel;
import static io.qameta.allure.util.ResultsUtils.createLink;
import static io.qameta.allure.util.ResultsUtils.createParameter;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("MultipleStringLiterals")
public class AllureKarate implements RuntimeHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureKarate.class);

    private static final String ALLURE_UUID = "ALLURE_UUID";

    private final AllureLifecycle lifecycle;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, String> stepsAndTcUuids = new HashMap<>();
    private final Map<String, Step> stepAndUuids = new HashMap<>();

    private final List<String> tcUuids = new ArrayList<>();

    public AllureKarate() {
        this(Allure.getLifecycle());
    }

    public AllureKarate(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public boolean beforeScenario(final ScenarioRuntime sr) {
        final Feature feature = sr.featureRuntime.result.getFeature();
        final String featureName = feature.getName();
        final String featureNameQualified = feature.getPackageQualifiedName();
        final Scenario scenario = sr.scenario;
        final String scenarioName = scenario.getName();

        final String uuid = UUID.randomUUID().toString();
        sr.magicVariables.put(ALLURE_UUID, uuid);

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setFullName(String.format("%s | %s", featureNameQualified, scenarioName))
                .setName(scenarioName)
                .setDescription(scenario.getDescription())
                .setTestCaseId(scenario.getUniqueId())
                .setStage(Stage.RUNNING);

        final List<String> labels = sr.tags.getTags();
        final List<Label> allLabels = getLabels(labels);
        allLabels.add(ResultsUtils.createFeatureLabel(featureName));
        result.setLabels(allLabels);

        final List<Link> links = getLinks(labels);
        if (!links.isEmpty()) {
            result.setLinks(links);
        }

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);
        return true;
    }

    @Override
    public void afterScenario(final ScenarioRuntime sr) {
        final String uuid = (String) sr.magicVariables.get(ALLURE_UUID);
        if (Objects.isNull(uuid)) {
            return;
        }
        final Optional<ScenarioResult> maybeResult = Optional.of(sr)
                .map(s -> s.result);

        final Status status = !sr.isFailed()
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
        if (sr.result != null && sr.result.getScenario().getExampleIndex() > -1) {
            final Map<String, Object> data = sr.result.getScenario().getExampleData();
            final Set<String> keys = data.keySet();
            for (String key : keys) {
                list.add(createParameter(key, sr.result.getScenario().getExampleData().get(key)));
            }
        }

        lifecycle.updateTestCase(uuid, tr -> {
            tr.setStage(Stage.FINISHED);
            tr.setStatus(status);
            tr.setStatusDetails(statusDetails);
            tr.setParameters(list);
        });

        lifecycle.stopTestCase(uuid);
        tcUuids.add(uuid);
    }

    @Override
    public boolean beforeStep(final Step step,
                              final ScenarioRuntime sr) {
        final String parentUuid = (String) sr.magicVariables.get(ALLURE_UUID);
        if (Objects.isNull(parentUuid)) {
            return true;
        }

        final String uuid = parentUuid + "-" + step.getIndex();
        final io.qameta.allure.model.StepResult stepResult = new io.qameta.allure.model.StepResult()
                .setName(step.getText());

        lifecycle.startStep(parentUuid, uuid, stepResult);

        return true;
    }

    @Override
    public void afterStep(final StepResult result,
                          final ScenarioRuntime sr) {
        final String parentUuid = (String) sr.magicVariables.get(ALLURE_UUID);
        if (Objects.isNull(parentUuid)) {
            return;
        }

        final Step step = result.getStep();
        final String uuid = parentUuid + "-" + step.getIndex();

        final Result stepResult = result.getResult();

        final Status status = !stepResult.isFailed()
                ? Status.PASSED
                : Optional.of(stepResult)
                .map(Result::getError)
                .flatMap(ResultsUtils::getStatus)
                .orElse(null);

        final StatusDetails statusDetails = Optional.of(stepResult)
                .map(Result::getError)
                .flatMap(ResultsUtils::getStatusDetails)
                .orElse(null);

        lifecycle.updateStep(uuid, s -> {
            s.setStatus(status);
            s.setStatusDetails(statusDetails);
        });
        lifecycle.stopStep(uuid);

        if (stepResult.isFailed()
                && sr.engine.getConfig().getDriverOptions() != null
                && (Boolean) sr.engine.getConfig().getDriverOptions().get("screenshotOnFailure")
        ) {
            addToStepsAndTcUuids(uuid, lifecycle.getCurrentTestCase().get());
            addToStepAndUuids(uuid, step);
        }

        if (Objects.nonNull(result.getEmbeds())) {
            result.getEmbeds().forEach(embed -> {
                try (InputStream is = new BufferedInputStream(Files.newInputStream(embed.getFile().toPath()))) {
                    lifecycle.addAttachment(
                            embed.getFile().getName(),
                            embed.getResourceType().contentType,
                            embed.getResourceType().getExtension(),
                            is
                    );
                } catch (IOException e) {
                    LOGGER.warn("could not save embedding", e);
                }
            });
        }

    }

    @Override
    public void afterFeature(final FeatureRuntime fr) {

        if (!stepsAndTcUuids.isEmpty()) {
            fr.result.getScenarioResults()
                    .forEach(sc -> {
                        if (Objects.nonNull(sc.getFailedStep())) {
                            getKeySetFromStepAndUuids().forEach(uuid -> {
                                if (getValueFromStepAndUuids(uuid) == sc.getFailedStep().getStep()) {
                                    final List<Attachment> attachments = new ArrayList<>();
                                    sc.getFailedStep().getEmbeds().forEach(e -> attachments.add(
                                                    new Attachment()
                                                            .setSource(e.getFile().getPath())
                                                            .setType(e.getResourceType().contentType)
                                                            .setName(e.getFile().getName())
                                            )
                                    );
                                    lifecycle.updateTestCase(getValueFromStepsAndTcUuids(uuid), result ->
                                            result.setAttachments(attachments)
                                    );
                                }
                            });
                        }
                    });
        }

        tcUuids.forEach(lifecycle::writeTestCase);
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

    private void addToStepsAndTcUuids(String stepUuid, String tcUuid) {
        lock.writeLock().lock();
        try {
            stepsAndTcUuids.put(stepUuid, tcUuid);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void addToStepAndUuids(String stepUuid, Step step) {
        lock.writeLock().lock();
        try {
            stepAndUuids.put(stepUuid, step);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String getValueFromStepsAndTcUuids(String stepUuid) {
        lock.readLock().lock();
        try {
            return stepsAndTcUuids.get(stepUuid);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Step getValueFromStepAndUuids(String stepUuid) {
        lock.readLock().lock();
        try {
            return stepAndUuids.get(stepUuid);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Set<String> getKeySetFromStepAndUuids() {
        lock.readLock().lock();
        try {
            return stepAndUuids.keySet();
        } finally {
            lock.readLock().unlock();
        }
    }
}
