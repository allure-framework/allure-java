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
package io.qameta.allure.cucumber4jvm;

import cucumber.api.HookTestStep;
import cucumber.api.HookType;
import cucumber.api.PickleStepTestStep;
import cucumber.api.Result;
import cucumber.api.TestCase;
import cucumber.api.TestStep;
import cucumber.api.event.ConcurrentEventListener;
import cucumber.api.event.EmbedEvent;
import cucumber.api.event.EventHandler;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseFinished;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestSourceRead;
import cucumber.api.event.TestStepFinished;
import cucumber.api.event.TestStepStarted;
import cucumber.api.event.WriteEvent;
import cucumber.runtime.formatter.TestSourcesModelProxy;
import gherkin.ast.Examples;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.TableRow;
import gherkin.pickles.PickleCell;
import gherkin.pickles.PickleRow;
import gherkin.pickles.PickleStep;
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
import java.net.URI;
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

import static cucumber.api.HookType.Before;
import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Allure plugin for Cucumber JVM 4.0.
 */
@SuppressWarnings({
        "ClassDataAbstractionCoupling",
        "ClassFanOutComplexity",
        "MultipleStringLiterals",
})
public class AllureCucumber4Jvm implements ConcurrentEventListener {

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
    private final Map<TestCase, String> testCaseUuids = new ConcurrentHashMap<>();
    private final Map<TestStep, String> stepUuids = new ConcurrentHashMap<>();
    private final Map<TestStep, String> fixtureUuids = new ConcurrentHashMap<>();

    private static final String TXT_EXTENSION = ".txt";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String CUCUMBER_WORKING_DIR = Paths.get("").toUri().getSchemeSpecificPart();

    @SuppressWarnings("unused")
    public AllureCucumber4Jvm() {
        this(Allure.getLifecycle());
    }

    public AllureCucumber4Jvm(final AllureLifecycle lifecycle) {
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
        testSources.addTestSourceReadEvent(event.uri, event);
    }

    private void handleTestCaseStarted(final TestCaseStarted event) {
        final TestCase testCase = event.getTestCase();
        final Feature feature = testSources.getFeature(testCase.getUri());

        final Deque<PickleTag> tags = new LinkedList<>(testCase.getTags());
        final LabelBuilder labelBuilder = new LabelBuilder(feature, testCase, tags);

        final String name = testCase.getName();


        // the same way full name is generated for
        // org.junit.platform.engine.support.descriptor.ClasspathResourceSource
        // to support io.qameta.allure.junitplatform.AllurePostDiscoveryFilter
        final String fullName = String.format("%s:%d",
                getTestCaseUri(testCase),
                testCase.getLine()
        );

        final String testCaseUuid = testCaseUuids
                .computeIfAbsent(testCase, tc -> UUID.randomUUID().toString());

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
        final String uuid = testCaseUuids.get(testCase);
        if (Objects.isNull(uuid)) {
            return;
        }

        final Feature feature = testSources.getFeature(testCase.getUri());
        final Result result = event.result;
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
        if (event.testStep instanceof HookTestStep) {
            final HookTestStep hook = (HookTestStep) event.testStep;

            if (isFixtureHook(hook)) {
                handleStartFixtureHook(testCase, hook);
            } else {
                handleStartStepHook(testCase, hook);
            }
        } else if (event.testStep instanceof PickleStepTestStep) {
            handleStartPickleStep(testCase, (PickleStepTestStep) event.testStep);
        }
    }

    private void handleStartPickleStep(final TestCase testCase,
                                       final PickleStepTestStep pickleStep) {
        final String uuid = testCaseUuids.get(testCase);
        if (Objects.isNull(uuid)) {
            return;
        }

        final PickleStep step = pickleStep.getPickleStep();
        final String stepKeyword = Optional
                .ofNullable(
                        testSources.getKeywordFromSource(
                                testCase.getUri(),
                                pickleStep.getStepLine()
                        )
                )
                .orElse("");

        final StepResult stepResult = new StepResult()
                .setName(stepKeyword + step.getText())
                .setStart(System.currentTimeMillis());

        final String stepUuid = stepUuids.computeIfAbsent(
                pickleStep,
                cl -> UUID.randomUUID().toString()
        );

        lifecycle.setCurrentTestCase(uuid);
        lifecycle.startStep(uuid, stepUuid, stepResult);

        pickleStep.getStepArgument()
                .stream()
                .filter(PickleTable.class::isInstance)
                .map(PickleTable.class::cast)
                .findFirst()
                .ifPresent(this::createDataTableAttachment);

    }

    private void handleStartStepHook(final TestCase testCase,
                                     final HookTestStep hook) {
        final String uuid = testCaseUuids.get(testCase);
        if (Objects.isNull(uuid)) {
            return;
        }

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
        final String uuid = testCaseUuids.get(testCase);
        if (Objects.isNull(uuid)) {
            return;
        }


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
        if (hook.getHookType() == Before) {
            lifecycle.startPrepareFixture(containerUuid, fixtureUuid, hookResult);
        } else {
            lifecycle.startTearDownFixture(containerUuid, fixtureUuid, hookResult);
        }
    }

    private void handleTestStepFinished(final TestStepFinished event) {
        if (event.testStep instanceof HookTestStep) {
            final HookTestStep hook = (HookTestStep) event.testStep;
            if (isFixtureHook(hook)) {
                handleStopHookStep(event.result, hook);
            } else {
                handleStopStep(event.getTestCase(), event.result, hook);
            }
        } else if (event.testStep instanceof PickleStepTestStep) {
            final PickleStepTestStep pickleStep = (PickleStepTestStep) event.testStep;
            handleStopStep(event.getTestCase(), event.result, pickleStep);
        }
    }

    private static boolean isFixtureHook(final HookTestStep hook) {
        return hook.getHookType() == Before || hook.getHookType() == HookType.After;
    }

    private void handleWriteEvent(final WriteEvent event) {
        lifecycle.addAttachment(
                "Text output",
                TEXT_PLAIN,
                TXT_EXTENSION,
                Objects.toString(event.text).getBytes(StandardCharsets.UTF_8)
        );
    }

    private void handleEmbedEvent(final EmbedEvent event) {
        lifecycle.addAttachment(
                Objects.isNull(event.name)
                        ? "Embedding"
                        : event.name,
                event.mimeType,
                null,
                new ByteArrayInputStream(event.data)
        );
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
        final String testCaseUri = getUriWithoutScheme(testCase);

        if (testCaseUri.startsWith(CUCUMBER_WORKING_DIR)) {
            return testCaseUri.substring(CUCUMBER_WORKING_DIR.length());
        }
        return testCaseUri;
    }

    private static String getUriWithoutScheme(final TestCase testCase) {
        try {
            return URI.create(testCase.getUri()).getSchemeSpecificPart();
        } catch (Exception ignored) {
            return testCase.getUri();
        }
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

    private void createDataTableAttachment(final PickleTable pickleTable) {
        final List<PickleRow> rows = pickleTable.getRows();

        final StringBuilder dataTableCsv = new StringBuilder();
        for (PickleRow row : rows) {
            final String rowString = row.getCells().stream()
                    .map(PickleCell::getValue)
                    .collect(Collectors.joining("\t", "", "\n"));
            dataTableCsv.append(rowString);
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
                = eventResult.getStatus() == Result.Type.UNDEFINED
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
