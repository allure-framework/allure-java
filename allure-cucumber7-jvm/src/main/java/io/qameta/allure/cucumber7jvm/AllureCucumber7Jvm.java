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
package io.qameta.allure.cucumber7jvm;

import io.cucumber.messages.types.Examples;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.TableCell;
import io.cucumber.messages.types.TableRow;
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
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;
import io.cucumber.plugin.event.WriteEvent;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.cucumber7jvm.testsourcemodel.TestSourcesModelProxy;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Allure plugin for Cucumber JVM 7.0.
 */
@SuppressWarnings({
        "ClassDataAbstractionCoupling",
        "ClassFanOutComplexity",
        "PMD.ExcessiveImports",
})
public class AllureCucumber7Jvm implements ConcurrentEventListener {

    private final AllureLifecycle lifecycle;

    private final TestSourcesModelProxy testSources = new TestSourcesModelProxy();

    private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
    private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
    private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
    private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
    private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;
    private final EventHandler<WriteEvent> writeEventHandler = this::handleWriteEvent;
    private final EventHandler<EmbedEvent> embedEventHandler = this::handleEmbedEvent;

    private static final String TXT_EXTENSION = ".txt";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String CUCUMBER_WORKING_DIR = Paths.get("").toUri().getSchemeSpecificPart();

    @SuppressWarnings("unused")
    public AllureCucumber7Jvm() {
        this(Allure.getLifecycle());
    }

    public AllureCucumber7Jvm(final AllureLifecycle lifecycle) {
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
                testCase.getLocation().getLine()
        );

        final TestResult result = new TestResult()
                .setUuid(testCase.getId().toString())
                .setHistoryId(getHistoryId(testCase))
                .setFullName(fullName)
                .setName(name)
                .setLabels(labelBuilder.getScenarioLabels())
                .setLinks(labelBuilder.getScenarioLinks());

        final Scenario scenarioDefinition =
                testSources.getScenarioDefinition(
                        testCase.getUri(),
                        testCase.getLocation().getLine()
                );


        if (scenarioDefinition.getExamples() != null) {
            result.setParameters(
                    getExamplesAsParameters(scenarioDefinition, testCase)
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
        lifecycle.startTestCase(testCase.getId().toString());
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
        final String uuid = testCase.getId().toString();

        if (event.getTestStep() instanceof PickleStepTestStep) {
            final PickleStepTestStep pickleStep = (PickleStepTestStep) event.getTestStep();
            final Step step = pickleStep.getStep();

            final StepResult stepResult = new StepResult()
                    .setName(step.getKeyword() + step.getText())
                    .setStart(System.currentTimeMillis());

            lifecycle.setCurrentTestCase(uuid);
            lifecycle.startStep(uuid, pickleStep.getId().toString(), stepResult);

            final StepArgument stepArgument = step.getArgument();
            if (stepArgument instanceof DataTableArgument) {
                final DataTableArgument dataTableArgument = (DataTableArgument) stepArgument;
                createDataTableAttachment(dataTableArgument);
            }
        } else if (event.getTestStep() instanceof HookTestStep) {
            final HookTestStep hook = (HookTestStep) event.getTestStep();

            final String containerUuid = UUID.randomUUID().toString();
            lifecycle.startTestContainer(new TestResultContainer()
                    .setUuid(containerUuid)
                    .setChildren(Collections.singletonList(uuid))
            );

            final FixtureResult hookResult = new FixtureResult()
                    .setName(hook.getCodeLocation());

            final String fixtureUuid = hook.getId().toString();
            switch (hook.getHookType()) {
                case BEFORE:
                case BEFORE_STEP:
                    lifecycle.startPrepareFixture(containerUuid, fixtureUuid, hookResult);
                    return;
                default:
                    lifecycle.startTearDownFixture(containerUuid, fixtureUuid, hookResult);
            }
        }
    }


    private void handleTestStepFinished(final TestStepFinished event) {
        if (event.getTestStep() instanceof HookTestStep) {
            handleHookStep(event);
        } else {
            handlePickleStep(event);
        }
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

    /*
    Utility Methods
     */

    private String getHistoryId(final TestCase testCase) {
        final String testCaseLocation = getTestCaseUri(testCase) + ":" + testCase.getLocation().getLine();
        return md5(testCaseLocation);
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
            final Scenario scenario,
            final TestCase localCurrentTestCase) {

        final Optional<Examples> maybeExample =
                scenario.getExamples().stream()
                        .filter(example -> example.getTableBody().stream()
                                .anyMatch(row -> row.getLocation().getLine()
                                                 == localCurrentTestCase.getLocation().getLine())
                        )
                        .findFirst();

        if (!maybeExample.isPresent()) {
            return Collections.emptyList();
        }

        final Examples examples = maybeExample.get();

        final Optional<TableRow> maybeRow = examples.getTableBody().stream()
                .filter(example -> example.getLocation().getLine() == localCurrentTestCase.getLocation().getLine())
                .findFirst();

        if (!maybeRow.isPresent()) {
            return Collections.emptyList();
        }

        final TableRow row = maybeRow.get();
        final int size = row.getCells().size();

        final List<String> headerNames = examples.getTableHeader()
                .map(TableRow::getCells)
                .map(rows -> rows.stream()
                        .map(TableCell::getValue)
                        .collect(Collectors.toList())
                )
                .orElse(null);

        return IntStream.range(0, size)
                .mapToObj(index -> {
                    final String name = Objects.nonNull(headerNames)
                            ? headerNames.get(index)
                            : "arg" + index;
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
                for (int i = 0; i < columns.size(); i++) {
                    if (i == columns.size() - 1) {
                        dataTableCsv.append(columns.get(i));
                    } else {
                        dataTableCsv.append(columns.get(i));
                        dataTableCsv.append('\t');
                    }
                }
                dataTableCsv.append('\n');
            }
        }
        final String attachmentSource = lifecycle
                .prepareAttachment("Data table", "text/tab-separated-values", "csv");
        lifecycle.writeAttachment(attachmentSource,
                new ByteArrayInputStream(dataTableCsv.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private void handleHookStep(final TestStepFinished event) {
        final TestCase testCase = event.getTestCase();
        final Feature feature = testSources.getFeature(testCase.getUri());

        final HookTestStep hookStep = (HookTestStep) event.getTestStep();
        final String uuid = hookStep.getId().toString();
        final FixtureResult fixtureResult = new FixtureResult().setStatus(translateTestCaseStatus(event.getResult()));

        if (!Status.PASSED.equals(fixtureResult.getStatus())) {
            final TestResult testResult = new TestResult().setStatus(translateTestCaseStatus(event.getResult()));
            final StatusDetails statusDetails = getStatusDetails(event.getResult().getError())
                    .orElseGet(StatusDetails::new);

            final String errorMessage = event.getResult().getError() == null
                    ? hookStep.getHookType().name() + " is failed."
                    : hookStep.getHookType().name()
                      + " is failed: "
                      + event.getResult().getError().getLocalizedMessage();
            statusDetails.setMessage(errorMessage);

            if (hookStep.getHookType() == HookType.BEFORE) {
                final TagParser tagParser = new TagParser(feature, testCase);
                statusDetails
                        .setFlaky(tagParser.isFlaky())
                        .setMuted(tagParser.isMuted())
                        .setKnown(tagParser.isKnown());
                testResult.setStatus(Status.SKIPPED);
//                updateTestCaseStatus(testResult.getStatus());
//                forbidTestCaseStatusChange.set(true);
            } else {
                testResult.setStatus(Status.BROKEN);
//                updateTestCaseStatus(testResult.getStatus());
            }
            fixtureResult.setStatusDetails(statusDetails);
        }

        lifecycle.updateFixture(uuid, result -> result.setStatus(fixtureResult.getStatus())
                .setStatusDetails(fixtureResult.getStatusDetails()));
        lifecycle.stopFixture(uuid);
    }

    private void handlePickleStep(final TestStepFinished event) {
        final TestCase testCase = event.getTestCase();
        final Feature feature = testSources.getFeature(testCase.getUri());

        final String uuid = testCase.getId().toString();
        final Status stepStatus = translateTestCaseStatus(event.getResult());
        final StatusDetails statusDetails;
        if (event.getResult().getStatus() == io.cucumber.plugin.event.Status.UNDEFINED) {
//            updateTestCaseStatus(Status.PASSED);

            statusDetails = getStatusDetails(new IllegalStateException("Undefined Step. Please add step definition"))
                    .orElse(new StatusDetails());

            lifecycle.updateTestCase(
                    uuid,
                    scenarioResult -> scenarioResult
                            .setStatusDetails(statusDetails)
            );
        } else {
            statusDetails = getStatusDetails(event.getResult().getError())
                    .orElse(new StatusDetails());
//            updateTestCaseStatus(stepStatus);
        }

        if (!Status.PASSED.equals(stepStatus) && stepStatus != null) {
//            forbidTestCaseStatusChange.set(true);
        }

        final TagParser tagParser = new TagParser(feature, testCase);
        statusDetails
                .setFlaky(tagParser.isFlaky())
                .setMuted(tagParser.isMuted())
                .setKnown(tagParser.isKnown());

        final String stepUuid = event.getTestStep().getId().toString();
        lifecycle.updateStep(
                stepUuid,
                stepResult -> stepResult.setStatus(stepStatus).setStatusDetails(statusDetails)
        );
        lifecycle.stopStep(stepUuid);
    }
}
