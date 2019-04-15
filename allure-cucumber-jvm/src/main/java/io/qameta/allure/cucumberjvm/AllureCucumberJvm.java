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

import cucumber.runtime.StepDefinitionMatch;
import gherkin.I18n;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.DataTableRow;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import gherkin.formatter.model.Tag;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allure plugin for Cucumber-JVM.
 */
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity",
        "ClassDataAbstractionCoupling",
        "unused"
})
public class AllureCucumberJvm implements Reporter, Formatter {

    private static final List<String> SCENARIO_OUTLINE_KEYWORDS = Collections.synchronizedList(new ArrayList<String>());

    private static final String FAILED = "failed";
    private static final String PASSED = "passed";
    private static final String SKIPPED = "skipped";
    private static final String PENDING = "pending";

    private static final String TXT_EXTENSION = ".txt";
    private static final String TEXT_PLAIN = "text/plain";

    private final Map<Scenario, String> scenarioUuids = new ConcurrentHashMap<>();
    private final Deque<Step> gherkinSteps = new LinkedList<>();
    private final AllureLifecycle lifecycle;
    private Feature currentFeature;
    private boolean isNullMatch = true;
    private Scenario currentScenario;


    @SuppressWarnings("unused")
    public AllureCucumberJvm() {
        this(Allure.getLifecycle());
    }

    public AllureCucumberJvm(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        final List<I18n> i18nList = I18n.getAll();
        i18nList.forEach(i18n -> SCENARIO_OUTLINE_KEYWORDS.addAll(i18n.keywords("scenario_outline")));
    }

    @Override
    public void feature(final Feature feature) {
        this.currentFeature = feature;
    }

    @Override
    public void before(final Match match, final Result result) {
        new StepUtils(currentFeature, currentScenario).fireFixtureStep(match, result, true);
    }

    @Override
    public void after(final Match match, final Result result) {
        new StepUtils(currentFeature, currentScenario).fireFixtureStep(match, result, false);
    }

    @Override
    public void startOfScenarioLifeCycle(final Scenario scenario) {
        this.currentScenario = scenario;

        final Deque<Tag> tags = new LinkedList<>(scenario.getTags());

        if (SCENARIO_OUTLINE_KEYWORDS.contains(scenario.getKeyword())) {
            synchronized (gherkinSteps) {
                gherkinSteps.clear();
            }
        } else {
            tags.addAll(currentFeature.getTags());
        }

        final LabelBuilder labelBuilder = new LabelBuilder(currentFeature, scenario, tags);

        final String uuid = UUID.randomUUID().toString();
        scenarioUuids.put(scenario, uuid);

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setHistoryId(StepUtils.getHistoryId(scenario.getId()))
                .setFullName(String.format("%s: %s", currentFeature.getName(), scenario.getName()))
                .setName(scenario.getName())
                .setLabels(labelBuilder.getScenarioLabels())
                .setLinks(labelBuilder.getScenarioLinks());

        if (!currentFeature.getDescription().isEmpty()) {
            result.setDescription(currentFeature.getDescription());
        }

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

    }

    @Override
    public void step(final Step step) {
        synchronized (gherkinSteps) {
            gherkinSteps.add(step);
        }
    }

    @Override
    public void match(final Match match) {
        final StepUtils stepUtils = new StepUtils(currentFeature, currentScenario);
        if (match instanceof StepDefinitionMatch) {
            isNullMatch = false;
            final Step step = stepUtils.extractStep((StepDefinitionMatch) match);
            synchronized (gherkinSteps) {
                while (gherkinSteps.peek() != null && !stepUtils.isEqualSteps(step, gherkinSteps.peek())) {
                    stepUtils.fireCanceledStep(gherkinSteps.remove());
                }
                if (stepUtils.isEqualSteps(step, gherkinSteps.peek())) {
                    gherkinSteps.remove();
                }
            }
            final StepResult stepResult = new StepResult();
            stepResult.setName(String.format("%s %s", step.getKeyword(), getStepName(step)))
                    .setStart(System.currentTimeMillis());

            final String scenarioUuid = scenarioUuids.get(currentScenario);
            lifecycle.startStep(scenarioUuid, stepUtils.getStepUuid(step), stepResult);
            createDataTableAttachment(step.getRows());
        }
    }

    @SuppressWarnings("PMD.NcssCount")
    @Override
    public void result(final Result result) {
        if (!isNullMatch) {
            final StatusDetails statusDetails =
                    ResultsUtils.getStatusDetails(result.getError()).orElse(new StatusDetails());
            final TagParser tagParser = new TagParser(currentFeature, currentScenario);
            statusDetails
                    .setFlaky(tagParser.isFlaky())
                    .setMuted(tagParser.isMuted())
                    .setKnown(tagParser.isKnown());

            final String scenarioUuid = scenarioUuids.get(currentScenario);
            switch (result.getStatus()) {
                case FAILED:
                    final Status status = ResultsUtils.getStatus(result.getError())
                            .orElse(Status.FAILED);
                    lifecycle.updateStep(stepResult -> stepResult.setStatus(Status.FAILED));
                    lifecycle.updateTestCase(scenarioUuid, scenarioResult ->
                            scenarioResult.setStatus(status)
                                    .setStatusDetails(statusDetails));
                    lifecycle.stopStep();
                    break;
                case PENDING:
                    lifecycle.updateStep(stepResult -> stepResult.setStatus(Status.SKIPPED));
                    lifecycle.updateTestCase(scenarioUuid, scenarioResult ->
                            scenarioResult.setStatus(Status.SKIPPED)
                                    .setStatusDetails(statusDetails));
                    lifecycle.stopStep();
                    break;
                case SKIPPED:
                    lifecycle.updateStep(stepResult -> stepResult.setStatus(Status.SKIPPED));
                    lifecycle.stopStep();
                    break;
                case PASSED:
                    lifecycle.updateStep(stepResult -> stepResult.setStatus(Status.PASSED));
                    lifecycle.stopStep();
                    lifecycle.updateTestCase(scenarioUuid, scenarioResult ->
                            scenarioResult.setStatus(Status.PASSED)
                                    .setStatusDetails(statusDetails));
                    break;
                default:
                    break;
            }
            isNullMatch = true;
        }
    }

    @Override
    public void endOfScenarioLifeCycle(final Scenario scenario) {
        final StepUtils stepUtils = new StepUtils(currentFeature, currentScenario);
        synchronized (gherkinSteps) {
            while (gherkinSteps.peek() != null) {
                stepUtils.fireCanceledStep(gherkinSteps.remove());
            }
        }
        final String scenarioUuid = scenarioUuids.remove(scenario);
        lifecycle.stopTestCase(scenarioUuid);
        lifecycle.writeTestCase(scenarioUuid);
    }

    public String getStepName(final Step step) {
        return step.getName();
    }

    private void createDataTableAttachment(final List<DataTableRow> dataTableRows) {
        final StringBuilder dataTableCsv = new StringBuilder();

        if (dataTableRows != null && !dataTableRows.isEmpty()) {
            dataTableRows.forEach(dataTableRow -> {
                dataTableCsv.append(String.join("\t", dataTableRow.getCells()));
                dataTableCsv.append('\n');
            });

            final String attachmentSource = lifecycle
                    .prepareAttachment("Data table", "text/tab-separated-values", "csv");
            lifecycle.writeAttachment(attachmentSource,
                    new ByteArrayInputStream(dataTableCsv.toString().getBytes(Charset.forName("UTF-8"))));
        }
    }

    @Override
    public void embedding(final String string, final byte[] bytes) {
        lifecycle.addAttachment("Screenshot", null, null, new ByteArrayInputStream(bytes));
    }

    @Override
    public void write(final String string) {
        lifecycle.addAttachment(
                "Text output",
                TEXT_PLAIN,
                TXT_EXTENSION,
                Objects.toString(string).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void syntaxError(final String state, final String event,
                            final List<String> legalEvents, final String uri, final Integer line) {
        //Nothing to do with Allure
    }

    @Override
    public void uri(final String uri) {
        //Nothing to do with Allure
    }

    @Override
    public void scenarioOutline(final ScenarioOutline so) {
        //Nothing to do with Allure
    }

    @Override
    public void examples(final Examples exmpls) {
        //Nothing to do with Allure
    }


    @Override
    public void background(final Background b) {
        //Nothing to do with Allure
    }

    @Override
    public void scenario(final Scenario scnr) {
        //Nothing to do with Allure
    }

    @Override
    public void done() {
        //Nothing to do with Allure
    }

    @Override
    public void close() {
        //Nothing to do with Allure
    }

    @Override
    public void eof() {
        //Nothing to do with Allure

    }

}
