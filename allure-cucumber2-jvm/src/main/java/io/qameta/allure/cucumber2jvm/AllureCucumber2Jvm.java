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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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
    private final Map<HookType, ParentHook> parentHooks = new HashMap<>();

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
        parentHooks.clear();
        currentFeatureFile = event.testCase.getUri();
        currentFeature = cucumberSourceUtils.getFeature(currentFeatureFile);

        currentTestCase = event.testCase;

        final Deque<PickleTag> tags = new LinkedList<>(event.testCase.getTags());

        final LabelBuilder labelBuilder = new LabelBuilder(currentFeature, event.testCase, tags);

        final String name = event.testCase.getName();
        final String featureName = currentFeature.getName();

        final TestResult result = new TestResult()
                .setUuid(getTestCaseUuid(event.testCase))
                .setHistoryId(getHistoryId(event.testCase))
                .setFullName(String.format("%s: %s", featureName, name))
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

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(getTestCaseUuid(event.testCase));
    }

    private void handleTestCaseFinished(final TestCaseFinished event) {

        if (parentHooks.get(HookType.After) != null) {
            lifecycle.stopStep(parentHooks.get(HookType.After).uuid);
        }

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
        if (parentHooks.get(hook.getHookType()) == null) {
            final StepResult parentHookResult = new StepResult()
                    .setName(hook.getHookType().name())
                    .setStart(System.currentTimeMillis());

            parentHooks.put(hook.getHookType(), new ParentHook(getParentHookUuid(hook)));
            lifecycle.startStep(getTestCaseUuid(currentTestCase), this.parentHooks.get(hook.getHookType()).uuid,
                    parentHookResult);
        }

        final StepResult hookResult = new StepResult()
                .setName(hook.getCodeLocation())
                .setStart(System.currentTimeMillis());

        lifecycle.startStep(getHookStepUuid(hook), hookResult);
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

    private String getParentHookUuid(final TestStep step) {
        return currentFeature.getName() + getTestCaseUuid(currentTestCase)
                + step.getHookType().toString();
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
        final Examples examples = scenarioOutline.getExamples().get(0);
        final TableRow row = examples.getTableBody().stream()
                .filter(example -> example.getLocation().getLine() == currentTestCase.getLine())
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

        final ParentHook parentHook = parentHooks.get(event.testStep.getHookType());
        parentHook.updateStatus(translateTestCaseStatus(event.result));
        final Consumer<StepResult> parentHookResult = result -> result.setStatus(parentHook.getStatus());

        if (!Status.PASSED.equals(translateTestCaseStatus(event.result))) {
            final StatusDetails statusDetails = getStatusDetails(event.result.getError()).get();
            if (event.testStep.getHookType() == HookType.Before) {
                final TagParser tagParser = new TagParser(currentFeature, currentTestCase);
                statusDetails
                        .setMessage("Before is failed: " + event.result.getError().getLocalizedMessage())
                        .setFlaky(tagParser.isFlaky())
                        .setMuted(tagParser.isMuted())
                        .setKnown(tagParser.isKnown());
                lifecycle.updateTestCase(getTestCaseUuid(currentTestCase), scenarioResult ->
                        scenarioResult.setStatus(Status.SKIPPED)
                                .setStatusDetails(statusDetails));
            }
            stepResult = result -> result
                    .setStatus(translateTestCaseStatus(event.result))
                    .setStatusDetails(statusDetails);
        }

        lifecycle.updateStep(uuid, stepResult);
        lifecycle.stopStep(uuid);
        lifecycle.updateStep(parentHook.uuid, parentHookResult);
    }

    private void handlePickleStep(final TestStepFinished event) {

        if (parentHooks.get(HookType.Before) != null) {
            lifecycle.stopStep(parentHooks.get(HookType.Before).uuid);
            parentHooks.clear();
        }

        final StatusDetails statusDetails;
        if (event.result.getStatus() == Result.Type.UNDEFINED) {
            statusDetails =
                    getStatusDetails(new PendingException("TODO: implement me"))
                            .orElse(new StatusDetails());
            lifecycle.updateTestCase(getTestCaseUuid(currentTestCase), scenarioResult ->
                    scenarioResult
                            .setStatus(translateTestCaseStatus(event.result))
                            .setStatusDetails(statusDetails));
        } else {
            statusDetails =
                    getStatusDetails(event.result.getError())
                            .orElse(new StatusDetails());
        }

        final TagParser tagParser = new TagParser(currentFeature, currentTestCase);
        statusDetails
                .setFlaky(tagParser.isFlaky())
                .setMuted(tagParser.isMuted())
                .setKnown(tagParser.isKnown());

        lifecycle.updateStep(getStepUuid(event.testStep), stepResult ->
                stepResult.setStatus(translateTestCaseStatus(event.result)));
        lifecycle.stopStep(getStepUuid(event.testStep));
    }
}
