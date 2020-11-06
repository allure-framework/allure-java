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
package io.qameta.allure.cucumberjvm;

import cucumber.runtime.CucumberException;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Step;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Objects;

import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Step utils.
 */
class StepUtils {
    private static final Logger LOG = LoggerFactory.getLogger(StepUtils.class);
    private static final String FAILED = "failed";

    private final AllureLifecycle lifecycle;
    private final Feature feature;
    private final Scenario scenario;
    private final String scenarioUuid;

    StepUtils(final Feature feature, final Scenario scenario, final String scenarioUuid) {
        this(feature, scenario, scenarioUuid, Allure.getLifecycle());
    }

    StepUtils(
            final Feature feature,
            final Scenario scenario,
            final String scenarioUuid,
            final AllureLifecycle lifecycle
    ) {
        this.lifecycle = lifecycle;
        this.feature = feature;
        this.scenario = scenario;
        this.scenarioUuid = scenarioUuid;
    }

    protected Step extractStep(final StepDefinitionMatch match) {
        try {
            final Field step = match.getClass().getDeclaredField("step");
            step.setAccessible(true);
            return (Step) step.get(match);
        } catch (ReflectiveOperationException e) {
            //shouldn't ever happen
            LOG.error(e.getMessage(), e);
            throw new CucumberException(e);
        }
    }

    protected boolean isEqualSteps(final Step step, final Step gherkinStep) {
        return Objects.equals(step.getLine(), gherkinStep.getLine());
    }

    protected void fireCanceledStep(final Step unimplementedStep) {
        final StepResult stepResult = new StepResult();
        stepResult.setName(unimplementedStep.getName())
                .setStart(System.currentTimeMillis())
                .setStop(System.currentTimeMillis())
                .setStatus(Status.SKIPPED)
                .setStatusDetails(new StatusDetails().setMessage("Unimplemented step"));
        lifecycle.startStep(scenarioUuid, getStepUuid(unimplementedStep), stepResult);
        lifecycle.stopStep(getStepUuid(unimplementedStep));

        final StatusDetails statusDetails = new StatusDetails();
        final TagParser tagParser = new TagParser(feature, scenario);
        statusDetails
                .setFlaky(tagParser.isFlaky())
                .setMuted(tagParser.isMuted())
                .setKnown(tagParser.isKnown());
        lifecycle.updateTestCase(scenarioUuid, scenarioResult ->
                scenarioResult.setStatus(Status.SKIPPED)
                        .setStatusDetails(statusDetails
                                .setMessage("Unimplemented steps were found")));
    }

    protected String getStepUuid(final Step step) {
        return feature.getId() + scenario.getId() + step.getName() + step.getLine();
    }

    protected static String getHistoryId(final String id) {
        return md5(id);
    }

    protected void fireFixtureStep(final Match match, final Result result, final boolean isBefore) {
        final String uuid = md5(match.getLocation());
        final StepResult stepResult = new StepResult()
                .setName(match.getLocation())
                .setStatus(Status.fromValue(result.getStatus()))
                .setStart(System.currentTimeMillis() - result.getDuration())
                .setStop(System.currentTimeMillis());
        if (FAILED.equals(result.getStatus())) {
            final StatusDetails statusDetails = ResultsUtils.getStatusDetails(result.getError()).get();
            stepResult.setStatusDetails(statusDetails);
            if (isBefore) {
                final TagParser tagParser = new TagParser(feature, scenario);
                statusDetails
                        .setMessage("Before is failed: " + result.getError().getLocalizedMessage())
                        .setFlaky(tagParser.isFlaky())
                        .setMuted(tagParser.isMuted())
                        .setKnown(tagParser.isKnown());
                lifecycle.updateTestCase(scenarioUuid, scenarioResult ->
                        scenarioResult.setStatus(Status.SKIPPED)
                                .setStatusDetails(statusDetails));
            }
        }
        lifecycle.startStep(scenarioUuid, uuid, stepResult);
        lifecycle.stopStep(uuid);
    }
}
