package io.qameta.allure.cucumber2jvm;

import cucumber.api.*;
import cucumber.api.event.EventHandler;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestSourceRead;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestCaseFinished;
import cucumber.api.event.TestStepStarted;
import cucumber.api.event.TestStepFinished;
import cucumber.api.formatter.Formatter;

import cucumber.runner.UnskipableStep;
import cucumber.runtime.formatter.TestSourcesModel;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Examples;
import gherkin.ast.TableRow;
import gherkin.pickles.PickleCell;
import gherkin.pickles.PickleRow;
import gherkin.pickles.PickleTable;
import gherkin.pickles.PickleTag;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.util.ResultsUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private Feature currentFeature;
    private String currentFeatureFile;
    private TestCase currentTestCase;
    private final TestSourcesModel testSources = new TestSourcesModel();

    private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
    private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
    private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
    private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
    private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;

    public AllureCucumber2Jvm() {
        this.lifecycle = Allure.getLifecycle();
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
        testSources.addTestSourceReadEvent(event.uri, event);
    }

    private void handleTestCaseStarted(final TestCaseStarted event) {
        currentFeatureFile = event.testCase.getUri();
        currentFeature = testSources.getFeature(currentFeatureFile);

        currentTestCase = event.testCase;

        final Deque<PickleTag> tags = new LinkedList<>();
        tags.addAll(event.testCase.getTags());

        final LabelBuilder labelBuilder = new LabelBuilder(currentFeature, event.testCase, tags);

        final TestResult result = new TestResult()
                .withUuid(getTestCaseUuid(event.testCase))
                .withHistoryId(getHistoryId(event.testCase))
                .withName(event.testCase.getName())
                .withLabels(labelBuilder.getScenarioLabels())
                .withLinks(labelBuilder.getScenarioLinks());

        final ScenarioDefinition scenarioDefinition =
                testSources.getScenarioDefinition(currentFeatureFile, currentTestCase.getLine());
        if (scenarioDefinition instanceof ScenarioOutline) {
            result.withParameters(
                    getExamplesAsParameters((ScenarioOutline) scenarioDefinition)
            );
        }

        if (currentFeature.getDescription() != null && !currentFeature.getDescription().isEmpty()) {
            result.withDescription(currentFeature.getDescription());
        }

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(getTestCaseUuid(event.testCase));
    }

    private void handleTestCaseFinished(final TestCaseFinished event) {
        final StatusDetails statusDetails =
                ResultsUtils.getStatusDetails(event.result.getError()).orElse(new StatusDetails());

        if (statusDetails.getMessage() != null && statusDetails.getTrace() != null) {
            lifecycle.updateTestCase(getTestCaseUuid(event.testCase), scenarioResult ->
                    scenarioResult
                            .withStatus(translateTestCaseStatus(event.result))
                            .withStatusDetails(statusDetails));
        } else {
            lifecycle.updateTestCase(getTestCaseUuid(event.testCase), scenarioResult ->
                    scenarioResult
                            .withStatus(translateTestCaseStatus(event.result)));
        }

        lifecycle.stopTestCase(getTestCaseUuid(event.testCase));
        lifecycle.writeTestCase(getTestCaseUuid(event.testCase));
    }

    private void handleTestStepStarted(final TestStepStarted event) {
        if (!event.testStep.isHook()) {
            final String stepKeyword = Optional.ofNullable(
                    testSources.getKeywordFromSource(currentFeatureFile, event.testStep.getStepLine())
            ).orElse("UNDEFINED");

            final StepResult stepResult = new StepResult();
            stepResult
                    .withName(String.format("%s %s", stepKeyword, event.testStep.getPickleStep().getText()))
                    .withStart(System.currentTimeMillis());

            lifecycle.startStep(getTestCaseUuid(currentTestCase), getStepUuid(event.testStep), stepResult);

            event.testStep.getStepArgument().stream()
                    .filter(argument -> argument instanceof PickleTable)
                    .findFirst()
                    .ifPresent(table -> createDataTableAttachment((PickleTable) table));
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

    private String getTestCaseUuid(final TestCase testCase) {
        return scenarioUuids.computeIfAbsent(getHistoryId(testCase), it -> UUID.randomUUID().toString());
    }

    private String getStepUuid(final TestStep step) {
        return currentFeature.getName() + getTestCaseUuid(currentTestCase)
                + step.getPickleStep().getText() + step.getStepLine();
    }

    private String getHistoryId(final TestCase testCase) {
        final String testCaseLocation = testCase.getUri() + ":" + testCase.getLine();
        return Utils.md5(testCaseLocation);
    }

    private Status translateTestCaseStatus(final Result testCaseResult) {
        Status allureStatus;
        if (testCaseResult.getStatus() == Result.Type.UNDEFINED || testCaseResult.getStatus() == Result.Type.PENDING) {
            allureStatus = Status.SKIPPED;
        } else {
            try {
                allureStatus = Status.fromValue(testCaseResult.getStatus().lowerCaseName());
            } catch (IllegalArgumentException e) {
                allureStatus = Status.BROKEN;
            }
        }
        return allureStatus;
    }

    private List<Parameter> getExamplesAsParameters(final ScenarioOutline scenarioOutline) {
        final Examples examples = scenarioOutline.getExamples().get(0);
        final TableRow row = examples.getTableBody().stream()
                .filter(example -> example.getLocation().getLine() == currentTestCase.getLine())
                .findFirst().get();
        return IntStream.range(0, examples.getTableHeader().getCells().size()).mapToObj(index -> {
            final String name = examples.getTableHeader().getCells().get(index).getValue();
            final String value = row.getCells().get(index).getValue();
            return new Parameter().withName(name).withValue(value);
        }).collect(Collectors.toList());
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
        final String uuid = UUID.randomUUID().toString();
        final StepResult stepResult = new StepResult()
                .withName(event.testStep.getHookType().toString())
                .withStatus(translateTestCaseStatus(event.result))
                .withStart(System.currentTimeMillis() - event.result.getDuration())
                .withStop(System.currentTimeMillis());

        if (!Status.PASSED.equals(stepResult.getStatus())) {
            final StatusDetails statusDetails = ResultsUtils.getStatusDetails(event.result.getError()).get();
            stepResult.withStatusDetails(statusDetails);
            if (event.testStep.getHookType() == HookType.Before) {
                final TagParser tagParser = new TagParser(currentFeature, currentTestCase);
                statusDetails
                        .withMessage("Before is failed: " + event.result.getError().getLocalizedMessage())
                        .withFlaky(tagParser.isFlaky())
                        .withMuted(tagParser.isMuted())
                        .withKnown(tagParser.isKnown());
                lifecycle.updateTestCase(getTestCaseUuid(currentTestCase), scenarioResult ->
                        scenarioResult.withStatus(Status.SKIPPED)
                                .withStatusDetails(statusDetails));
            }
        }

        lifecycle.startStep(getTestCaseUuid(currentTestCase), uuid, stepResult);
        lifecycle.stopStep(uuid);
    }

    private void handlePickleStep(final TestStepFinished event) {
        final StatusDetails statusDetails;
        if (event.result.getStatus() == Result.Type.UNDEFINED) {
            statusDetails =
                    ResultsUtils.getStatusDetails(new PendingException("TODO: implement me"))
                            .orElse(new StatusDetails());
            lifecycle.updateTestCase(getTestCaseUuid(currentTestCase), scenarioResult ->
                    scenarioResult
                            .withStatus(translateTestCaseStatus(event.result))
                            .withStatusDetails(statusDetails));
        } else {
            statusDetails =
                    ResultsUtils.getStatusDetails(event.result.getError())
                            .orElse(new StatusDetails());
        }

        final TagParser tagParser = new TagParser(currentFeature, currentTestCase);
        statusDetails
                .withFlaky(tagParser.isFlaky())
                .withMuted(tagParser.isMuted())
                .withKnown(tagParser.isKnown());

        lifecycle.updateStep(getStepUuid(event.testStep), stepResult ->
                stepResult.withStatus(translateTestCaseStatus(event.result)));
        lifecycle.stopStep(getStepUuid(event.testStep));
    }
}
