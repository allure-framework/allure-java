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
package io.qameta.allure.cucumber2jvm;

import cucumber.api.HookType;
import cucumber.api.PendingException;
import cucumber.api.Result;
import cucumber.api.TestCase;
import cucumber.api.TestStep;
import cucumber.api.event.EventHandler;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseFinished;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestSourceRead;
import cucumber.api.event.TestStepFinished;
import cucumber.api.event.TestStepStarted;
import cucumber.api.formatter.Formatter;
import cucumber.runner.UnskipableStep;
import gherkin.ast.Examples;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.pickles.PickleCell;
import gherkin.pickles.PickleRow;
import gherkin.pickles.PickleTable;
import gherkin.pickles.PickleTag;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Allure plugin for Cucumber JVM 2.0.
 */
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity", "ClassDataAbstractionCoupling"
})
public class AllureCucumber2Jvm implements Formatter {

    private final AllureLifecycle lifecycle;

    private final Map<String, String> scenarioUuids = new HashMap<>();

    private final CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();
    private Feature currentFeature;
    private String currentFeatureFile;
    private TestCase currentTestCase;
    private String currentContainer;
    private boolean forbidTestCaseStatusChange;

    private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
    private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
    private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
    private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
    private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;

    @SuppressWarnings("unused")
    public AllureCucumber2Jvm() {
        this(Allure.getLifecycle());
    }

    public AllureCucumber2Jvm(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void setEventPublisher(final EventPublisher publisher) {
        publisher.registerHandlerFor(TestSourceRead.class, featureStartedHandler);

        publisher.registerHandlerFor(TestCaseStarted.class, caseStartedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class, caseFinishedHandler);

        publisher.registerHandlerFor(TestStepStarted.class, stepStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);
    }

    /*
    Event Handlers
     */

    private void handleFeatureStartedHandler(final TestSourceRead event) {
        cucumberSourceUtils.addTestSourceReadEvent(event.uri, event);
    }

    private void handleTestCaseStarted(final TestCaseStarted event) {
        currentTestCase = event.testCase;
        currentFeatureFile = currentTestCase.getUri();
        currentFeature = cucumberSourceUtils.getFeature(currentFeatureFile);
        currentContainer = UUID.randomUUID().toString();
        forbidTestCaseStatusChange = false;


        final Deque<PickleTag> tags = new LinkedList<>(currentTestCase.getTags());

        final LabelBuilder labelBuilder = new LabelBuilder(currentFeature, currentTestCase, tags);

        final String name = currentTestCase.getName();
        final String featureName = currentFeature.getName();

        final TestResult result = new TestResult()
                .setUuid(getTestCaseUuid(currentTestCase))
                .setHistoryId(getHistoryId(currentTestCase))
                .setFullName(featureName + ": " + name)
                .setName(name)
                .setLabels(labelBuilder.getScenarioLabels())
                .setLinks(labelBuilder.getScenarioLinks());

        final ScenarioDefinition scenarioDefinition =
                cucumberSourceUtils.getScenarioDefinition(currentFeatureFile, currentTestCase.getLine());
        if (scenarioDefinition instanceof ScenarioOutline) {
            result.setParameters(
                    getExamplesAsParameters((ScenarioOutline) scenarioDefinition)
            );
        }

        if (currentFeature.getDescription() != null && !currentFeature.getDescription().isEmpty()) {
            result.setDescription(currentFeature.getDescription());
        }

        final TestResultContainer resultContainer = new TestResultContainer()
                .setName(String.format("%s: %s", scenarioDefinition.getKeyword(), scenarioDefinition.getName()))
                .setUuid(getTestContainerUuid())
                .setChildren(Collections.singletonList(getTestCaseUuid(currentTestCase)));

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestContainer(getTestContainerUuid(), resultContainer);
        lifecycle.startTestCase(getTestCaseUuid(currentTestCase));
    }

    private void handleTestCaseFinished(final TestCaseFinished event) {

        final String uuid = getTestCaseUuid(event.testCase);
        final Optional<StatusDetails> details = getStatusDetails(event.result.getError());
        details.ifPresent(statusDetails -> lifecycle.updateTestCase(
                uuid,
                testResult -> testResult.setStatusDetails(statusDetails)
        ));
        lifecycle.stopTestCase(uuid);
        lifecycle.stopTestContainer(getTestContainerUuid());
        lifecycle.writeTestCase(uuid);
        lifecycle.writeTestContainer(getTestContainerUuid());
    }

    private void handleTestStepStarted(final TestStepStarted event) {
        if (!event.testStep.isHook()) {
            final String stepKeyword = Optional.ofNullable(
                    cucumberSourceUtils.getKeywordFromSource(currentFeatureFile, event.testStep.getStepLine())
            ).orElse("UNDEFINED");

            final StepResult stepResult = new StepResult()
                    .setName(String.format("%s %s", stepKeyword, event.testStep.getPickleStep().getText()))
                    .setStart(System.currentTimeMillis());

            lifecycle.startStep(getTestCaseUuid(currentTestCase), getStepUuid(event.testStep), stepResult);

            event.testStep.getStepArgument().stream()
                    .filter(PickleTable.class::isInstance)
                    .findFirst()
                    .ifPresent(table -> createDataTableAttachment((PickleTable) table));
        } else if (event.testStep instanceof UnskipableStep) {
            initHook((UnskipableStep) event.testStep);
        }
    }

    private void initHook(final UnskipableStep hook) {

        final FixtureResult hookResult = new FixtureResult()
                .setName(hook.getCodeLocation())
                .setStart(System.currentTimeMillis());

        if (hook.getHookType() == HookType.Before) {
            lifecycle.startPrepareFixture(getTestContainerUuid(), getHookStepUuid(hook), hookResult);
        } else {
            lifecycle.startTearDownFixture(getTestContainerUuid(), getHookStepUuid(hook), hookResult);
        }

    }

    private void handleTestStepFinished(final TestStepFinished event) {
        if (event.testStep.isHook() && event.testStep instanceof UnskipableStep) {
            handleHookStep(event);
        } else {
            handlePickleStep(event);
        }
    }

    /*
    Utility Methods
     */

    private String getTestContainerUuid() {
        return currentContainer;
    }

    private String getTestCaseUuid(final TestCase testCase) {
        return scenarioUuids.computeIfAbsent(getHistoryId(testCase), it -> UUID.randomUUID().toString());
    }

    private String getStepUuid(final TestStep step) {
        return currentFeature.getName() + getTestCaseUuid(currentTestCase)
                + step.getPickleStep().getText() + step.getStepLine();
    }

    private String getHookStepUuid(final TestStep step) {
        return currentFeature.getName() + getTestCaseUuid(currentTestCase)
                + step.getHookType().toString() + step.getCodeLocation();
    }

    private String getHistoryId(final TestCase testCase) {
        final String testCaseLocation = testCase.getUri() + ":" + testCase.getLine();
        return md5(testCaseLocation);
    }

    private Status translateTestCaseStatus(final Result testCaseResult) {
        switch (testCaseResult.getStatus()) {
            case FAILED:
                return getStatus(testCaseResult.getError())
                        .orElse(Status.FAILED);
            case PASSED:
                return Status.PASSED;
            case SKIPPED:
            case PENDING:
                return Status.SKIPPED;
            case AMBIGUOUS:
            case UNDEFINED:
            default:
                return null;
        }
    }

    private List<Parameter> getExamplesAsParameters(final ScenarioOutline scenarioOutline) {
        final List<Parameter> parameterList = new ArrayList<>();
        final List<Examples> scenarioOutlineList = scenarioOutline.getExamples().stream()
                .filter(examples -> examples.getLocation().getLine() + 2 == currentTestCase.getLine())
                .collect(Collectors.toList());

        scenarioOutlineList.forEach(examples -> examples.getTableBody()
                .forEach(tableRow -> {
                    IntStream.range(0, examples.getTableHeader().getCells().size())
                            .forEach(consumer -> {
                                final String name = examples.getTableHeader().getCells().get(consumer).getValue();
                                final String value = tableRow.getCells().get(consumer).getValue();
                                parameterList.add(new Parameter().setName(name).setValue(value));
                            });
                }));
        return parameterList;
    }

    private void createDataTableAttachment(final PickleTable pickleTable) {
        final List<PickleRow> rows = pickleTable.getRows();

        final StringBuilder dataTableCsv = new StringBuilder();
        if (!rows.isEmpty()) {
            rows.forEach(dataTableRow -> {
                dataTableCsv.append(
                        dataTableRow.getCells().stream()
                                .map(PickleCell::getValue)
                                .collect(Collectors.joining("\t"))
                );
                dataTableCsv.append('\n');
            });

            final String attachmentSource = lifecycle
                    .prepareAttachment("Data table", "text/tab-separated-values", "csv");
            lifecycle.writeAttachment(attachmentSource,
                    new ByteArrayInputStream(dataTableCsv.toString().getBytes(Charset.forName("UTF-8"))));
        }
    }

    private void handleHookStep(final TestStepFinished event) {
        final String uuid = getHookStepUuid(event.testStep);
        final FixtureResult fixtureResult = new FixtureResult().setStatus(translateTestCaseStatus(event.result));

        if (!Status.PASSED.equals(fixtureResult.getStatus())) {
            final TestResult testResult = new TestResult().setStatus(translateTestCaseStatus(event.result));
            final StatusDetails statusDetails = getStatusDetails(event.result.getError()).get();

            statusDetails.setMessage(event.testStep.getHookType()
                    .name() + " is failed: " + event.result.getError().getLocalizedMessage());

            if (event.testStep.getHookType() == HookType.Before) {
                final TagParser tagParser = new TagParser(currentFeature, currentTestCase);
                statusDetails
                        .setFlaky(tagParser.isFlaky())
                        .setMuted(tagParser.isMuted())
                        .setKnown(tagParser.isKnown());
                testResult.setStatus(Status.SKIPPED);
                updateTestCaseStatus(testResult.getStatus());
                forbidTestCaseStatusChange = true;
            } else {
                testResult.setStatus(Status.BROKEN);
                updateTestCaseStatus(testResult.getStatus());
            }
            fixtureResult.setStatusDetails(statusDetails);
        }

        lifecycle.updateFixture(uuid, result -> result.setStatus(fixtureResult.getStatus())
                .setStatusDetails(fixtureResult.getStatusDetails()));
        lifecycle.stopFixture(uuid);
    }

    private void handlePickleStep(final TestStepFinished event) {

        final Status stepStatus = translateTestCaseStatus(event.result);
        final StatusDetails statusDetails;
        if (event.result.getStatus() == Result.Type.UNDEFINED) {
            updateTestCaseStatus(Status.PASSED);

            statusDetails =
                    getStatusDetails(new PendingException("TODO: implement me"))
                            .orElse(new StatusDetails());
            lifecycle.updateTestCase(getTestCaseUuid(currentTestCase), scenarioResult ->
                    scenarioResult
                            .setStatusDetails(statusDetails));
        } else {
            statusDetails =
                    getStatusDetails(event.result.getError())
                            .orElse(new StatusDetails());
            updateTestCaseStatus(stepStatus);
        }


        if (!Status.PASSED.equals(stepStatus) && stepStatus != null) {
            forbidTestCaseStatusChange = true;
        }

        final TagParser tagParser = new TagParser(currentFeature, currentTestCase);
        statusDetails
                .setFlaky(tagParser.isFlaky())
                .setMuted(tagParser.isMuted())
                .setKnown(tagParser.isKnown());

        lifecycle.updateStep(getStepUuid(event.testStep),
                stepResult -> stepResult.setStatus(stepStatus).setStatusDetails(statusDetails));
        lifecycle.stopStep(getStepUuid(event.testStep));
    }

    private void updateTestCaseStatus(final Status status) {
        if (!forbidTestCaseStatusChange) {
            lifecycle.updateTestCase(getTestCaseUuid(currentTestCase),
                    result -> result.setStatus(status));
        }
    }
}
