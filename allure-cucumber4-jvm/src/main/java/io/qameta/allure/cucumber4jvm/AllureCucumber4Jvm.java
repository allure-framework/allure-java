package io.qameta.allure.cucumber4jvm;

import cucumber.api.HookTestStep;
import cucumber.api.HookType;
import cucumber.api.PendingException;
import cucumber.api.PickleStepTestStep;
import cucumber.api.Result;
import cucumber.api.TestCase;
import cucumber.api.TestStep;

import cucumber.api.event.ConcurrentEventListener;
import cucumber.api.event.EventHandler;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseFinished;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestSourceRead;
import cucumber.api.event.TestStepFinished;
import cucumber.api.event.TestStepStarted;

import cucumber.runtime.formatter.TestSourcesModelProxy;
import gherkin.ast.Examples;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.TableRow;
import gherkin.pickles.PickleCell;
import gherkin.pickles.PickleRow;
import gherkin.pickles.PickleTable;
import gherkin.pickles.PickleTag;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * Allure plugin for Cucumber JVM 4.0.
 */
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity",
        "ClassDataAbstractionCoupling",
        "unused"})
public class AllureCucumber4Jvm implements ConcurrentEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureCucumber4Jvm.class);

    private final AllureLifecycle lifecycle;

    private final ConcurrentHashMap<String, String> scenarioUuids = new ConcurrentHashMap<>();
    private final TestSourcesModelProxy testSources = new TestSourcesModelProxy();

    private final ThreadLocal<Feature> currentFeature = new InheritableThreadLocal<>();
    private final ThreadLocal<String> currentFeatureFile = new InheritableThreadLocal<>();
    private final ThreadLocal<TestCase> currentTestCase = new InheritableThreadLocal<>();

    private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
    private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
    private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
    private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
    private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;

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
    }

    /*
    Event Handlers
     */

    private void handleFeatureStartedHandler(final TestSourceRead event) {
        testSources.addTestSourceReadEvent(event.uri, event);
    }

    private void handleTestCaseStarted(final TestCaseStarted event) {
        final String localCurrentFeatureFile = event.testCase.getUri();
        final Feature localCurrentFeature = testSources.getFeature(localCurrentFeatureFile);
        final TestCase localCurrentTestCase = event.testCase;

        currentFeatureFile.set(localCurrentFeatureFile);
        currentFeature.set(localCurrentFeature);
        currentTestCase.set(localCurrentTestCase);

        final Deque<PickleTag> tags = new LinkedList<>(localCurrentTestCase.getTags());

        final LabelBuilder labelBuilder = new LabelBuilder(localCurrentFeature, localCurrentTestCase, tags);

        final String name = localCurrentTestCase.getName();
        final String featureName = localCurrentFeature.getName();

        final TestResult result = new TestResult()
                .setUuid(getTestCaseUuid(localCurrentTestCase))
                .setHistoryId(getHistoryId(localCurrentTestCase))
                .setFullName(String.format("%s: %s", featureName, name))
                .setName(name)
                .setLabels(labelBuilder.getScenarioLabels())
                .setLinks(labelBuilder.getScenarioLinks());

        final ScenarioDefinition scenarioDefinition =
                testSources.getScenarioDefinition(localCurrentFeatureFile, localCurrentTestCase.getLine());
        if (scenarioDefinition instanceof ScenarioOutline) {
            result.setParameters(
                    getExamplesAsParameters((ScenarioOutline) scenarioDefinition, localCurrentTestCase)
            );
        }

        if (localCurrentFeature.getDescription() != null && !localCurrentFeature.getDescription().isEmpty()) {
            result.setDescription(localCurrentFeature.getDescription());
        }

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(getTestCaseUuid(localCurrentTestCase));
    }

    private void handleTestCaseFinished(final TestCaseFinished event) {
        final String uuid = getTestCaseUuid(event.testCase);
        lifecycle.updateTestCase(
                uuid,
                testResult -> testResult.setStatus(translateTestCaseStatus(event.result))
        );
        final Optional<StatusDetails> details = getStatusDetails(event.result.getError());
        details.ifPresent(statusDetails -> lifecycle.updateTestCase(
                uuid,
                testResult -> testResult.setStatusDetails(statusDetails)
        ));
        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);
    }

    private void handleTestStepStarted(final TestStepStarted event) {
        final String localCurrentFeatureFile = currentFeatureFile.get();
        final Feature localCurrentFeature = currentFeature.get();
        final TestCase localCurrentTestCase = currentTestCase.get();

        if (Objects.nonNull(localCurrentFeatureFile)
                && Objects.nonNull(localCurrentFeature)
                && Objects.nonNull(localCurrentTestCase)) {
            if (event.testStep instanceof PickleStepTestStep) {
                final PickleStepTestStep pickleStepTestStep = (PickleStepTestStep) event.testStep;

                final String stepKeyword = Optional.ofNullable(
                        testSources.getKeywordFromSource(localCurrentFeatureFile, pickleStepTestStep.getStepLine())
                ).orElse("UNDEFINED");

                final StepResult stepResult = new StepResult()
                        .setName(String.format("%s %s", stepKeyword, pickleStepTestStep.getPickleStep().getText()))
                        .setStart(System.currentTimeMillis());

                lifecycle.startStep(
                        getTestCaseUuid(localCurrentTestCase),
                        getStepUuid(event.testStep, localCurrentFeature, localCurrentTestCase),
                        stepResult
                );

                pickleStepTestStep.getStepArgument().stream()
                        .filter(PickleTable.class::isInstance)
                        .findFirst()
                        .ifPresent(table -> createDataTableAttachment((PickleTable) table));
            } else if (event.testStep instanceof HookTestStep) {
                final HookTestStep hookTestStep = (HookTestStep) event.testStep;
                final StepResult stepResult = new StepResult()
                        .setName(getHookStepName(hookTestStep))
                        .setStart(System.currentTimeMillis());
                lifecycle.startStep(
                        getTestCaseUuid(localCurrentTestCase),
                        getHookStepUuid(event.testStep, localCurrentFeature, localCurrentTestCase),
                        stepResult
                );
            } else {
                throw new IllegalStateException();
            }
        } else {
            LOGGER.error("Current Feature File, Feature and/or Test Case were not defined. "
                    + "Skipping TestStepStarted event");
        }
    }

    private void handleTestStepFinished(final TestStepFinished event) {
        final Feature localCurrentFeature = currentFeature.get();
        final TestCase localCurrentTestCase = currentTestCase.get();

        if (Objects.nonNull(localCurrentFeature) && Objects.nonNull(localCurrentTestCase)) {
            if (event.testStep instanceof PickleStepTestStep) {
                handlePickleStep(event, localCurrentFeature, localCurrentTestCase);
            } else if (event.testStep instanceof HookTestStep) {
                handleHookStep(event, localCurrentFeature, localCurrentTestCase);
            } else {
                throw new IllegalStateException();
            }
        } else {
            LOGGER.error("Current Feature and/or Test Case were not defined. Skipping TestStepFinished event");
        }
    }

    /*
    Utility Methods
     */

    private String getTestCaseUuid(final TestCase testCase) {
        return scenarioUuids.computeIfAbsent(getHistoryId(testCase), it -> UUID.randomUUID().toString());
    }

    private String getStepUuid(
            final TestStep step, final Feature localCurrentFeature, final TestCase localCurrentTestCase
    ) {
        final PickleStepTestStep pickleStep = (PickleStepTestStep) step;
        return localCurrentFeature.getName() + getTestCaseUuid(localCurrentTestCase)
                + pickleStep.getStepText() + pickleStep.getStepLine();
    }

    private String getHookStepUuid(
            final TestStep step, final Feature localCurrentFeature, final TestCase localCurrentTestCase
    ) {
        final HookTestStep hookTestStep = (HookTestStep) step;
        return localCurrentFeature.getName() + getTestCaseUuid(localCurrentTestCase)
                + hookTestStep.getHookType().toString() + step.getCodeLocation();
    }

    private String getHookStepName(final HookTestStep testStep) {
        return testStep.getHookType().toString() + ": " + testStep.getCodeLocation();
    }

    private String getHistoryId(final TestCase testCase) {
        final String testCaseLocation = testCase.getUri() + ":" + testCase.getLine();
        return ResultsUtils.md5(testCaseLocation);
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
            case UNDEFINED:
                return Status.SKIPPED;
            case AMBIGUOUS:
            default:
                return Status.BROKEN;
        }
    }

    private List<Parameter> getExamplesAsParameters(
            final ScenarioOutline scenarioOutline, final TestCase localCurrentTestCase
    ) {
        final Optional<Examples> examplesBlock =
                scenarioOutline.getExamples().stream()
                        .filter(example -> example.getTableBody().stream()
                                .anyMatch(row -> row.getLocation().getLine() == localCurrentTestCase.getLine())
                        ).findFirst();

        if(examplesBlock.isPresent()){
            final TableRow row = examplesBlock.get().getTableBody().stream()
                    .filter(example -> example.getLocation().getLine() == localCurrentTestCase.getLine())
                    .findFirst().get();
            return IntStream.range(0, examplesBlock.get().getTableHeader().getCells().size()).mapToObj(index -> {
                final String name = examplesBlock.get().getTableHeader().getCells().get(index).getValue();
                final String value = row.getCells().get(index).getValue();
                return new Parameter().setName(name).setValue(value);
            }).collect(Collectors.toList());
        } else {
            LOGGER.error("Could not find matching Examples block. Returning empty parameters list");
            return Collections.emptyList();
        }
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

    private void handleHookStep(
            final TestStepFinished event, final Feature localCurrentFeature, final TestCase localCurrentTestCase
    ) {
        final String uuid = getHookStepUuid(event.testStep, localCurrentFeature, localCurrentTestCase);
        final Status stepStatus = translateTestCaseStatus(event.result);

        Consumer<StepResult> stepResult = result -> result.setStatus(stepStatus);
        switch (stepStatus) {
            case FAILED:
            case BROKEN:
                final StatusDetails statusDetails
                        = getStatusDetails(event.result.getError()).orElse(new StatusDetails());
                final HookTestStep hookTestStep = (HookTestStep) event.testStep;
                if (hookTestStep.getHookType() == HookType.Before) {
                    final TagParser tagParser = new TagParser(localCurrentFeature, localCurrentTestCase);
                    statusDetails
                            .setMessage("Before is failed: " + statusDetails.getMessage())
                            .setFlaky(tagParser.isFlaky())
                            .setMuted(tagParser.isMuted())
                            .setKnown(tagParser.isKnown());
                    lifecycle.updateTestCase(getTestCaseUuid(localCurrentTestCase), scenarioResult ->
                            scenarioResult.setStatus(Status.SKIPPED)
                                    .setStatusDetails(statusDetails));
                }
                stepResult = result -> result
                        .setStatus(translateTestCaseStatus(event.result))
                        .setStatusDetails(statusDetails);
                break;
            case PASSED:
            case SKIPPED:
            default:
                break;
        }

        lifecycle.updateStep(uuid, stepResult);
        lifecycle.stopStep(uuid);
    }

    private void handlePickleStep(
            final TestStepFinished event, final Feature localCurrentFeature, final TestCase localCurrentTestCase
    ) {
        final String uuid = getStepUuid(event.testStep, localCurrentFeature, localCurrentTestCase);

        final StatusDetails statusDetails;
        if (event.result.getStatus() == Result.Type.UNDEFINED) {
            statusDetails =
                    getStatusDetails(new PendingException("TODO: implement me"))
                            .orElse(new StatusDetails());
            lifecycle.updateTestCase(getTestCaseUuid(localCurrentTestCase), scenarioResult ->
                    scenarioResult
                            .setStatus(translateTestCaseStatus(event.result))
                            .setStatusDetails(statusDetails));
        } else {
            statusDetails =
                    getStatusDetails(event.result.getError())
                            .orElse(new StatusDetails());
        }

        final TagParser tagParser = new TagParser(localCurrentFeature, localCurrentTestCase);
        statusDetails
                .setFlaky(tagParser.isFlaky())
                .setMuted(tagParser.isMuted())
                .setKnown(tagParser.isKnown());

        lifecycle.updateStep(uuid, stepResult ->
                stepResult.setStatus(translateTestCaseStatus(event.result)));
        lifecycle.stopStep(uuid);
    }
}
