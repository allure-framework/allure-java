package io.qameta.allure.cucumber2jvm;

import cucumber.api.HookType;
import cucumber.api.Result;
import cucumber.api.TestCase;
import cucumber.api.TestStep;
import cucumber.api.event.*;
import cucumber.api.formatter.Formatter;

import cucumber.runner.UnskipableStep;
import cucumber.runtime.formatter.TestSourcesModel;
import gherkin.ast.Feature;
import gherkin.ast.Step;
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
import java.util.stream.Collectors;

public class AllureCucumber2Jvm implements Formatter {

    private final AllureLifecycle lifecycle;

    private final Map<String, String> scenarioUuids = new HashMap<>();

    private Feature currentFeature;
    private String currentFeatureFile;
    private TestCase currentTestCase;
    private final TestSourcesModel testSources = new TestSourcesModel();

    public AllureCucumber2Jvm(){
        this.lifecycle = Allure.getLifecycle();
    }

    private EventHandler<TestSourceRead> featureStartedHandler = this::handleFeatureStartedHandler;

    private EventHandler<TestCaseStarted> caseStartedHandler = this::handleTestCaseStarted;
    private EventHandler<TestCaseFinished> caseFinishedHandler = this::handleTestCaseFinished;

    private EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
    private EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;

    private void handleFeatureStartedHandler(TestSourceRead event) {
        testSources.addTestSourceReadEvent(event.uri, event);
        currentFeature = testSources.getFeature(event.uri);
        currentFeatureFile = event.uri;
    }

    private void handleTestCaseStarted(TestCaseStarted event) {
        currentTestCase = event.testCase;

        final Deque<PickleTag> tags = new LinkedList<>();
        tags.addAll(event.testCase.getTags());
        //tags.addAll(currentFeature.getTags());

        final LabelBuilder labelBuilder = new LabelBuilder(currentFeature, event.testCase, tags);

        final TestResult result = new TestResult()
                .withUuid(getTestCaseUuid(event.testCase))
                .withHistoryId(getHistoryId(event.testCase))
                .withName(event.testCase.getName())
                .withLabels(labelBuilder.getScenarioLabels())
                .withLinks(labelBuilder.getScenarioLinks());

        if (currentFeature.getDescription() != null && !currentFeature.getDescription().isEmpty()) {
            result.withDescription(currentFeature.getDescription());
        }

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(getTestCaseUuid(event.testCase));
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        final StatusDetails statusDetails =
                ResultsUtils.getStatusDetails(event.result.getError()).orElse(new StatusDetails());

        lifecycle.updateTestCase(getTestCaseUuid(event.testCase), scenarioResult ->
                scenarioResult
                        .withStatus(translateTestCaseStatus(event.result))
                        .withStatusDetails(statusDetails));

        lifecycle.stopTestCase(getTestCaseUuid(event.testCase));
        lifecycle.writeTestCase(getTestCaseUuid(event.testCase));
    }

    private void handleTestStepStarted(TestStepStarted event) {
        if(!event.testStep.isHook()){
            String stepKeyword = Optional.ofNullable(
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

    private void createDataTableAttachment(final PickleTable pickleTable) {
        List<PickleRow> rows = pickleTable.getRows();

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

    private void handleTestStepFinished(TestStepFinished event) {
        if(event.testStep.isHook() && event.testStep instanceof UnskipableStep)
            handleHookStep(event);
        else
            handlePickleStep(event);
    }

    private void handleHookStep(TestStepFinished event){
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

    private void handlePickleStep(TestStepFinished event){
        final StatusDetails statusDetails =
                ResultsUtils.getStatusDetails(event.result.getError()).orElse(new StatusDetails());
        final TagParser tagParser = new TagParser(currentFeature, currentTestCase);
        statusDetails
                .withFlaky(tagParser.isFlaky())
                .withMuted(tagParser.isMuted())
                .withKnown(tagParser.isKnown());

        lifecycle.updateStep(stepResult -> stepResult.withStatus(translateTestCaseStatus(event.result)));
        lifecycle.stopStep();
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestSourceRead.class, featureStartedHandler);

        publisher.registerHandlerFor(TestCaseStarted.class, caseStartedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class, caseFinishedHandler);

        publisher.registerHandlerFor(TestStepStarted.class, stepStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);
    }

    /*
    Utility Methods
     */

    private String getTestCaseUuid(TestCase testCase){
        return scenarioUuids.computeIfAbsent(getHistoryId(testCase), it -> UUID.randomUUID().toString());
    }

    protected String getStepUuid(final TestStep step) {
        return currentFeature.getName() + currentTestCase.getName() + step.getPickleStep().getText() + step.getStepLine();
    }

    private String getHistoryId(TestCase testCase){
        return Utils.md5(testCase.getUri() + ":" + testCase.getLine());
    }

    private Status translateTestCaseStatus(Result testCaseResult){
        try{
            return Status.fromValue(testCaseResult.getStatus().lowerCaseName());
        }catch (IllegalArgumentException e){
            // if status is Unknown then return BROKEN
            return Status.BROKEN;
        }
    }
}
