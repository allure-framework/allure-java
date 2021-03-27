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
import com.intuit.karate.core.Result;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioResult;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.core.Step;
import com.intuit.karate.core.StepResult;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureKarate implements RuntimeHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureKarate.class);

    private static final String ALLURE_UUID = "ALLURE_UUID";

    private AllureLifecycle lifecycle;

    public AllureKarate() {
        this(Allure.getLifecycle());
    }

    public AllureKarate(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public boolean beforeScenario(final ScenarioRuntime sr) {
        final Feature feature = sr.featureRuntime.feature;
        final String featureName = feature.getName();
        final String featureNameQualified = feature.getPackageQualifiedName();
        final Scenario scenario = sr.scenario;
        final String scenarioName = scenario.getName();
        LOGGER.info("tags: {}", sr.tags.getTagValues());

        final String uuid = UUID.randomUUID().toString();
        sr.magicVariables.put(ALLURE_UUID, uuid);

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setFullName(String.format("%s | %s", featureNameQualified, scenarioName))
                .setName(scenarioName)
                .setDescription(scenario.getDescription())
                .setTestCaseId(scenario.getUniqueId())
                .setStage(Stage.RUNNING)
                .setLabels(List.of(
                        ResultsUtils.createFeatureLabel(featureName)
                ));

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

        lifecycle.updateTestCase(uuid, tr -> {
            tr.setStage(Stage.FINISHED);
            tr.setStatus(status);
            tr.setStatusDetails(statusDetails);
        });

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);
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

        result.getEmbeds().forEach(embed -> {
            try (InputStream is = new BufferedInputStream(new FileInputStream(embed.getFile()))) {
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

        lifecycle.updateStep(uuid, s -> {
            s.setStatus(status);
            s.setStatusDetails(statusDetails);
        });
        lifecycle.stopStep(uuid);

    }
}
