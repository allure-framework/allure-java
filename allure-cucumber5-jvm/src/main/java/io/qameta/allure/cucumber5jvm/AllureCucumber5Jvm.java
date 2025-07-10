/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.cucumber5jvm;

import gherkin.ast.Examples;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.TableRow;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.DataTableArgument;
import io.cucumber.plugin.event.EmbedEvent;
import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.HookType;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Step;
import io.cucumber.plugin.event.StepArgument;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestSourceRead;
import io.cucumber.plugin.event.TestStep;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;
import io.cucumber.plugin.event.WriteEvent;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.cucumber5jvm.testsourcemodel.TestSourcesModelProxy;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Allure plugin for Cucumber JVM 5.0.
 */
@SuppressWarnings({
        "ClassDataAbstractionCoupling",
        "ClassFanOutComplexity",
        "MultipleStringLiterals",
})
public class AllureCucumber5Jvm implements ConcurrentEventListener {

    private static final String COLON = ":";

    private final AllureLifecycle lifecycle;

    private final TestSourcesModelProxy testSources = new TestSourcesModelProxy();

    private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
    private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
    private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
    private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
    private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;
    private final EventHandler<WriteEvent> writeEventHandler = this::handleWriteEvent;
    private final EventHandler<EmbedEvent> embedEventHandler = this::handleEmbedEvent;

    private final Map<TestStep, String> hookStepContainerUuid = new ConcurrentHashMap<>();
    private final Map<TestStep, String> stepUuids = new ConcurrentHashMap<>();
    private final Map<TestStep, String> fixtureUuids = new ConcurrentHashMap<>();

    private static final String TXT_EXTENSION = ".txt";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String CUCUMBER_WORKING_DIR = Paths.get("").toUri().getSchemeSpecificPart();

    @SuppressWarnings("unused")
    public AllureCucumber5Jvm() {
        this(Allure.getLifecycle());
    }

    public AllureCucumber5Jvm(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void setEventPublisher(final EventPublisher publisher) {
        publisher.registerHandlerFor(TestSourceRead.class, featureStartedHandler);

        publisher.registerHandlerFor(TestCaseStarted.class, caseStartedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class, caseFinishedHandler);

        publisher.registerHandlerFor(TestStepStarted.class, stepStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);

        publisher.registerHandlerFor(WriteEvent.class, writeEventHandler);
        publisher.registerHandlerFor(EmbedEvent.class, embedEventHandler);
    }

    private void handleFeatureStartedHandler(final TestSourceRead event) {
        testSources.addTestSourceReadEvent(event.getUri(), event);
    }

    private void handleTestCaseStarted(final TestCaseStarted event) {
        final TestCase testCase = event.getTestCase();
        final Feature feature = testSources.getFeature(testCase.getUri());

        final Deque<String> tags = new LinkedList<>(testCase.getTags());
        final LabelBuilder labelBuilder = new LabelBuilder(feature, testCase, tags);

        final String name = testCase.getName();


        // the same way full name is generated for
        // org.junit.platform.engine.support.descriptor.ClasspathResourceSource
        // to support io.qameta.allure.junitplatform.AllurePostDiscoveryFilter
        final String fullName = String.format("%s:%d",
                getTestCaseUri(testCase),
                testCase.getLine()
        );

        final String testCaseUuid = testCase.getId().toString();

        final TestResult result = new TestResult()
                .setUuid(testCaseUuid)
                .setTestCaseId(getTestCaseId(testCase))
                .setHistoryId(getHistoryId(testCase))
                .setFullName(fullName)
                .setName(name)
                .setLabels(labelBuilder.getScenarioLabels())
                .setLinks(labelBuilder.getScenarioLinks());

        final ScenarioDefinition scenarioDefinition =
                testSources.getScenarioDefinition(
                        testCase.getUri(),
                        testCase.getLine()
                );

        if (scenarioDefinition instanceof ScenarioOutline) {
            result.setParameters(
                    getExamplesAsParameters((ScenarioOutline) scenarioDefinition, testCase)
            );
        }

        final String description = Stream.of(feature.getDescription(), scenarioDefinition.getDescription())
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));

        if (!description.isEmpty()) {
            result.setDescription(description);
        }

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(testCaseUuid);
    }

    private void handleTestCaseFinished(final TestCaseFinished event) {
        final TestCase testCase = event.getTestCase();
        final Feature feature = testSources.getFeature(testCase.getUri());
        final String uuid = testCase.getId().toString();
        final Result result = event.getResult();
        final Status status = translateTestCaseStatus(result);
        final StatusDetails statusDetails = getStatusDetails(result.getError())
                .orElseGet(StatusDetails::new);

        final TagParser tagParser = new TagParser(feature, testCase);
        statusDetails
                .setFlaky(tagParser.isFlaky())
                .setMuted(tagParser.isMuted())
                .setKnown(tagParser.isKnown());

        lifecycle.updateTestCase(uuid, testResult -> testResult
                .setStatus(status)
                .setStatusDetails(statusDetails)
        );

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);
    }

    private void handleTestStepStarted(final TestStepStarted event) {
        final TestCase testCase = event.getTestCase();
        if (event.getTestStep() instanceof HookTestStep) {
            final HookTestStep hook = (HookTestStep) event.getTestStep();

            if (isFixtureHook(hook)) {
                handleStartFixtureHook(testCase, hook);
            } else {
                handleStartStepHook(testCase, hook);
            }
        } else if (event.getTestStep() instanceof PickleStepTestStep) {
            handleStartPickleStep(testCase, (PickleStepTestStep) event.getTestStep());
        }
    }

    private void handleStartPickleStep(final TestCase testCase,
                                       final PickleStepTestStep pickleStep) {
        final String uuid = testCase.getId().toString();
        final Step step = pickleStep.getStep();

        final StepResult stepResult = new StepResult()
                .setName(step.getKeyWord() + step.getText())
                .setStart(System.currentTimeMillis());

        final String stepUuid = stepUuids.computeIfAbsent(
                pickleStep,
                cl -> UUID.randomUUID().toString()
        );

        lifecycle.setCurrentTestCase(uuid);
        lifecycle.startStep(uuid, stepUuid, stepResult);

        final StepArgument stepArgument = step.getArgument();
        if (stepArgument instanceof DataTableArgument) {
            final DataTableArgument dataTableArgument = (DataTableArgument) stepArgument;
            createDataTableAttachment(dataTableArgument);
        }
    }

    private void handleStartStepHook(final TestCase testCase,
                                     final HookTestStep hook) {
        final String uuid = testCase.getId().toString();
        final StepResult stepResult = new StepResult()
                .setName(hook.getCodeLocation())
                .setStart(System.currentTimeMillis());

        final String stepUuid = stepUuids.computeIfAbsent(
                hook, unused -> UUID.randomUUID().toString()
        );

        lifecycle.setCurrentTestCase(uuid);
        lifecycle.startStep(uuid, stepUuid, stepResult);
    }

    private void handleStartFixtureHook(final TestCase testCase,
                                        final HookTestStep hook) {
        final String uuid = testCase.getId().toString();

        final String containerUuid = hookStepContainerUuid
                .computeIfAbsent(hook, unused -> UUID.randomUUID().toString());

        lifecycle.startTestContainer(new TestResultContainer()
                .setUuid(containerUuid)
                .setChildren(Collections.singletonList(uuid))
        );

        final FixtureResult hookResult = new FixtureResult()
                .setName(hook.getCodeLocation());

        final String fixtureUuid = fixtureUuids.computeIfAbsent(
                hook, unused -> UUID.randomUUID().toString()
        );
        if (hook.getHookType() == HookType.BEFORE) {
            lifecycle.startPrepareFixture(containerUuid, fixtureUuid, hookResult);
        } else {
            lifecycle.startTearDownFixture(containerUuid, fixtureUuid, hookResult);
        }
    }

    private void handleTestStepFinished(final TestStepFinished event) {
        if (event.getTestStep() instanceof HookTestStep) {
            final HookTestStep hook = (HookTestStep) event.getTestStep();
            if (isFixtureHook(hook)) {
                handleStopHookStep(event.getResult(), hook);
            } else {
                handleStopStep(event.getTestCase(), event.getResult(), hook);
            }
        } else if (event.getTestStep() instanceof PickleStepTestStep) {
            final PickleStepTestStep pickleStep = (PickleStepTestStep) event.getTestStep();
            handleStopStep(event.getTestCase(), event.getResult(), pickleStep);
        }
    }

    private static boolean isFixtureHook(final HookTestStep hook) {
        return hook.getHookType() == HookType.BEFORE || hook.getHookType() == HookType.AFTER;
    }

    private void handleWriteEvent(final WriteEvent event) {
        lifecycle.addAttachment(
                "Text output",
                TEXT_PLAIN,
                TXT_EXTENSION,
                Objects.toString(event.getText()).getBytes(StandardCharsets.UTF_8)
        );
    }

    private void handleEmbedEvent(final EmbedEvent event) {
        lifecycle.addAttachment(event.name, event.getMediaType(), null, new ByteArrayInputStream(event.getData()));
    }

    private String getHistoryId(final TestCase testCase) {
        final String testCaseLocation = getTestCaseUri(testCase) + COLON + testCase.getLine();
        return md5(testCaseLocation);
    }

    private String getTestCaseId(final TestCase testCase) {
        final String testCaseId = getTestCaseUri(testCase) + COLON + testCase.getName();
        return md5(testCaseId);
    }

    private String getTestCaseUri(final TestCase testCase) {
        final String testCaseUri = testCase.getUri().getSchemeSpecificPart();
        if (testCaseUri.startsWith(CUCUMBER_WORKING_DIR)) {
            return testCaseUri.substring(CUCUMBER_WORKING_DIR.length());
        }
        return testCaseUri;
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

    private List<Parameter> getExamplesAsParameters(
            final ScenarioOutline scenario,
            final TestCase localCurrentTestCase) {
        final Optional<Examples> maybeExample =
                scenario.getExamples().stream()
                        .filter(example -> example.getTableBody().stream()
                                .anyMatch(row -> row.getLocation().getLine()
                                                 == localCurrentTestCase.getLine())
                        )
                        .findFirst();

        if (!maybeExample.isPresent()) {
            return Collections.emptyList();
        }

        final Examples examples = maybeExample.get();

        final Optional<TableRow> maybeRow = examples.getTableBody().stream()
                .filter(example -> example.getLocation().getLine() == localCurrentTestCase.getLine())
                .findFirst();

        if (!maybeRow.isPresent()) {
            return Collections.emptyList();
        }

        final TableRow row = maybeRow.get();

        return IntStream.range(0, examples.getTableHeader().getCells().size())
                .mapToObj(index -> {
                    final String name = examples.getTableHeader().getCells().get(index).getValue();
                    final String value = row.getCells().get(index).getValue();
                    return createParameter(name, value);
                })
                .collect(Collectors.toList());
    }

    private void createDataTableAttachment(final DataTableArgument dataTableArgument) {
        final List<List<String>> rowsInTable = dataTableArgument.cells();
        final StringBuilder dataTableCsv = new StringBuilder();
        for (List<String> columns : rowsInTable) {
            if (!columns.isEmpty()) {
                final String rowValue = columns.stream().collect(Collectors.joining("\t", "", "\n"));
                dataTableCsv.append(rowValue);
            }
        }
        final String attachmentSource = lifecycle
                .prepareAttachment("Data table", "text/tab-separated-values", "csv");
        lifecycle.writeAttachment(attachmentSource,
                new ByteArrayInputStream(dataTableCsv.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private void handleStopHookStep(final Result eventResult,
                                    final HookTestStep hook) {
        final String containerUuid = hookStepContainerUuid.get(hook);
        if (Objects.isNull(containerUuid)) {
            // maybe throw an exception?
            return;
        }

        final String uuid = fixtureUuids.get(hook);
        if (Objects.isNull(uuid)) {
            // maybe throw an exception?
            return;
        }

        final Status status = translateTestCaseStatus(eventResult);
        final StatusDetails statusDetails = getStatusDetails(eventResult.getError())
                .orElseGet(StatusDetails::new);

        lifecycle.updateFixture(uuid, result -> result
                .setStatus(status)
                .setStatusDetails(statusDetails)
        );
        lifecycle.stopFixture(uuid);

        lifecycle.stopTestContainer(containerUuid);
        lifecycle.writeTestContainer(containerUuid);
    }

    private void handleStopStep(final TestCase testCase,
                                final Result eventResult,
                                final TestStep step) {
        final String stepUuid = stepUuids.get(step);
        if (Objects.isNull(stepUuid)) {
            // maybe exception?
            return;
        }

        final Feature feature = testSources.getFeature(testCase.getUri());

        final Status stepStatus = translateTestCaseStatus(eventResult);

        final StatusDetails statusDetails
                = eventResult.getStatus() == io.cucumber.plugin.event.Status.UNDEFINED
                ? new StatusDetails().setMessage("Undefined Step. Please add step definition")
                : getStatusDetails(eventResult.getError())
                        .orElse(new StatusDetails());

        final TagParser tagParser = new TagParser(feature, testCase);
        statusDetails
                .setFlaky(tagParser.isFlaky())
                .setMuted(tagParser.isMuted())
                .setKnown(tagParser.isKnown());

        lifecycle.updateStep(
                stepUuid,
                stepResult -> stepResult
                        .setStatus(stepStatus)
                        .setStatusDetails(statusDetails)
        );
        lifecycle.stopStep(stepUuid);
    }
}
