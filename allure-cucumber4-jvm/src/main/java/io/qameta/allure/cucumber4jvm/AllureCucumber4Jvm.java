package io.qameta.allure.cucumber4jvm;

import cucumber.api.*;
import cucumber.api.event.*;
import gherkin.ast.*;
import gherkin.pickles.PickleCell;
import gherkin.pickles.PickleRow;
import gherkin.pickles.PickleTable;
import gherkin.pickles.PickleTag;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.*;
import io.qameta.allure.util.ResultsUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Allure plugin for Cucumber JVM 4.0.
 */
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity", "ClassDataAbstractionCoupling"
})
public class AllureCucumber4Jvm implements ConcurrentEventListener, Plugin {

    private final AllureLifecycle lifecycle;

    private final Map<String, String> scenarioUuids = new HashMap<>();

    private final CucumberSourceUtils cucumberSourceUtils = new CucumberSourceUtils();
    private final ThreadLocal<Feature> currentFeature = new ThreadLocal<>();
    private final ThreadLocal<String> currentFeatureFile = new ThreadLocal<>();
    private final ThreadLocal<TestCase> currentTestCase = new ThreadLocal<>();

    private final EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;
    private final EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
    private final EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;
    private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
    private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;

    public AllureCucumber4Jvm() {
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
        cucumberSourceUtils.addTestSourceReadEvent(event.uri, event);
    }

    private void handleTestCaseStarted(final TestCaseStarted event) {
        currentFeatureFile.set(event.testCase.getUri());
        currentFeature.set(cucumberSourceUtils.getFeature(currentFeatureFile.get()));

        currentTestCase.set(event.testCase);

        final Deque<PickleTag> tags = new LinkedList<>();
        tags.addAll(event.testCase.getTags());

        final LabelBuilder labelBuilder = new LabelBuilder(currentFeature.get(), event.testCase, tags);

        final TestResult result = new TestResult()
                .setUuid(getTestCaseUuid(event.testCase))
                .setHistoryId(getHistoryId(event.testCase))
                .setName(event.testCase.getName())
                .setLabels(labelBuilder.getScenarioLabels())
                .setLinks(labelBuilder.getScenarioLinks());

        final ScenarioDefinition scenarioDefinition =
                cucumberSourceUtils.getScenarioDefinition(currentFeatureFile.get(), currentTestCase.get().getLine());
        if (scenarioDefinition instanceof ScenarioOutline) {
            result.setParameters(
                    getExamplesAsParameters((ScenarioOutline) scenarioDefinition)
            );
        }

        if (currentFeature.get().getDescription() != null && !currentFeature.get().getDescription().isEmpty()) {
            result.setDescription(currentFeature.get().getDescription());
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
                            .setStatus(translateTestCaseStatus(event.result))
                            .setStatusDetails(statusDetails));
        } else {
            lifecycle.updateTestCase(getTestCaseUuid(event.testCase), scenarioResult ->
                    scenarioResult
                            .setStatus(translateTestCaseStatus(event.result)));
        }

        lifecycle.stopTestCase(getTestCaseUuid(event.testCase));
        lifecycle.writeTestCase(getTestCaseUuid(event.testCase));
    }

    private void handleTestStepStarted(final TestStepStarted event) {
        if (event.testStep instanceof PickleStepTestStep) {
            final PickleStepTestStep pickleStepTestStep = (PickleStepTestStep) event.testStep;

            final String stepKeyword = Optional.ofNullable(
                    cucumberSourceUtils.getKeywordFromSource(currentFeatureFile.get(), pickleStepTestStep.getStepLine())
            ).orElse("UNDEFINED");

            final StepResult stepResult = new StepResult()
                    .setName(String.format("%s %s", stepKeyword, pickleStepTestStep.getPickleStep().getText()))
                    .setStart(System.currentTimeMillis());

            lifecycle.startStep(getTestCaseUuid(currentTestCase.get()), getStepUuid(event.testStep), stepResult);

            pickleStepTestStep.getStepArgument().stream()
                    .filter(argument -> argument instanceof PickleTable)
                    .findFirst()
                    .ifPresent(table -> createDataTableAttachment((PickleTable) table));
        } else if (event.testStep instanceof HookTestStep) {
            final HookTestStep hookTestStep = (HookTestStep) event.testStep;
            final StepResult stepResult = new StepResult()
                    .setName(hookTestStep.getHookType().toString())
                    .setStart(System.currentTimeMillis());

            lifecycle.startStep(getTestCaseUuid(currentTestCase.get()), getHookStepUuid(event.testStep), stepResult);
        } else {
            throw new IllegalStateException();
        }
    }

    private void handleTestStepFinished(final TestStepFinished event) {
        if (event.testStep instanceof PickleStepTestStep) {
            handlePickleStep(event);
        } else if (event.testStep instanceof HookTestStep) {
            handleHookStep(event);
        } else {
            throw new IllegalStateException();
        }
    }

    /*
    Utility Methods
     */

    private String getTestCaseUuid(final TestCase testCase) {
        return scenarioUuids.computeIfAbsent(getHistoryId(testCase), it -> UUID.randomUUID().toString());
    }

    private String getStepUuid(final TestStep step) {
        final PickleStepTestStep pickleStep = (PickleStepTestStep) step;
        return currentFeature.get().getName() + getTestCaseUuid(currentTestCase.get())
                + pickleStep.getStepText() + pickleStep.getStepLine();
    }

    private String getHookStepUuid(final TestStep step) {
        final HookTestStep hookTestStep = (HookTestStep) step;
        return currentFeature.get().getName() + getTestCaseUuid(currentTestCase.get())
                + hookTestStep.getHookType().toString() + step.getCodeLocation();
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
                .filter(example -> example.getLocation().getLine() == currentTestCase.get().getLine())
                .findFirst().get();
        return IntStream.range(0, examples.getTableHeader().getCells().size()).mapToObj(index -> {
            final String name = examples.getTableHeader().getCells().get(index).getValue();
            final String value = row.getCells().get(index).getValue();
            return new Parameter().setName(name).setValue(value);
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
        final String uuid = getHookStepUuid(event.testStep);
        Consumer<StepResult> stepResult = result -> result.setStatus(translateTestCaseStatus(event.result));

        if (!Status.PASSED.equals(translateTestCaseStatus(event.result))) {
            final StatusDetails statusDetails = ResultsUtils.getStatusDetails(event.result.getError()).get();
            final HookTestStep hookTestStep = (HookTestStep) event.testStep;
            if (hookTestStep.getHookType() == HookType.Before) {
                final TagParser tagParser = new TagParser(currentFeature.get(), currentTestCase.get());
                statusDetails
                        .setMessage("Before is failed: " + event.result.getError().getLocalizedMessage())
                        .setFlaky(tagParser.isFlaky())
                        .setMuted(tagParser.isMuted())
                        .setKnown(tagParser.isKnown());
                lifecycle.updateTestCase(getTestCaseUuid(currentTestCase.get()), scenarioResult ->
                        scenarioResult.setStatus(Status.SKIPPED)
                                .setStatusDetails(statusDetails));
            }
            stepResult = result -> result
                    .setStatus(translateTestCaseStatus(event.result))
                    .setStatusDetails(statusDetails);
        }

        lifecycle.updateStep(uuid, stepResult);
        lifecycle.stopStep(uuid);
    }

    private void handlePickleStep(final TestStepFinished event) {
        final StatusDetails statusDetails;
        if (event.result.getStatus() == Result.Type.UNDEFINED) {
            statusDetails =
                    ResultsUtils.getStatusDetails(new PendingException("TODO: implement me"))
                            .orElse(new StatusDetails());
            lifecycle.updateTestCase(getTestCaseUuid(currentTestCase.get()), scenarioResult ->
                    scenarioResult
                            .setStatus(translateTestCaseStatus(event.result))
                            .setStatusDetails(statusDetails));
        } else {
            statusDetails =
                    ResultsUtils.getStatusDetails(event.result.getError())
                            .orElse(new StatusDetails());
        }

        final TagParser tagParser = new TagParser(currentFeature.get(), currentTestCase.get());
        statusDetails
                .setFlaky(tagParser.isFlaky())
                .setMuted(tagParser.isMuted())
                .setKnown(tagParser.isKnown());

        lifecycle.updateStep(getStepUuid(event.testStep), stepResult ->
                stepResult.setStatus(translateTestCaseStatus(event.result)));
        lifecycle.stopStep(getStepUuid(event.testStep));
    }
}
