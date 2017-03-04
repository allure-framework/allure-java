package io.qameta.allure.cucumberjvm;

import cucumber.runtime.CucumberException;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.I18n;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

public class AllureCucumberJvm implements Reporter, Formatter {

    private static final Logger LOG = LoggerFactory.getLogger(AllureCucumberJvm.class);
    private static final List<String> SCENARIO_OUTLINE_KEYWORDS = Collections.synchronizedList(new ArrayList<String>());
    private final LinkedList<Step> gherkinSteps = new LinkedList<>();
    private AllureLifecycle lifecycle;
    private Feature feature;
    private Scenario scenario;
    private StepDefinitionMatch match;


    private static final String FAILED = "failed";
    private static final String SKIPPED = "skipped";
    private static final String PASSED = "passed";

    public AllureCucumberJvm() {
        this.lifecycle = Allure.getLifecycle();
        List<I18n> i18nList = I18n.getAll();

        for (I18n i18n : i18nList) {
            SCENARIO_OUTLINE_KEYWORDS.addAll(i18n.keywords("scenario_outline"));
        }
    }

    @Override
    public void before(Match match, Result result) {
        //Nothing to do with Allure
    }

    @Override
    public void result(Result result) {
        if (match != null) {
            if (FAILED.equals(result.getStatus())) {
                lifecycle.stopStep();
                lifecycle.stopTestContainer(this.scenario.getId());
//                currentStatus = FAILED;
            } else if (SKIPPED.equals(result.getStatus())) {
                lifecycle.stopStep();
//                if (PASSED.equals(currentStatus)) {
                //not to change FAILED status to CANCELED in the report
//                    ALLURE_LIFECYCLE.fire(new TestCasePendingEvent());
//                    currentStatus = SKIPPED;
//                }
            }
            lifecycle.stopStep();
            match = null;
        }
    }

    @Override
    public void after(Match match, Result result) {
        //Nothing to do with Allure
    }

    @Override
    public void match(Match match) {
        if (match instanceof StepDefinitionMatch) {
            this.match = (StepDefinitionMatch) match;
            Step step = extractStep(this.match);
            synchronized (gherkinSteps) {
                while (gherkinSteps.peek() != null && !isEqualSteps(step, gherkinSteps.peek())) {
                    fireCanceledStep(gherkinSteps.remove());
                }
                if (isEqualSteps(step, gherkinSteps.peek())) {
                    gherkinSteps.remove();
                }
            }
            StepResult stepResult = new StepResult();
            stepResult.withName(step.getName())
                    .withStart(System.currentTimeMillis());
            lifecycle.startStep(this.scenario.getId(), getStepUUID(step), stepResult);
        }
    }

    @Override
    public void embedding(String string, byte[] bytes) {
        //Nothing to do with Allure
    }

    @Override
    public void write(String string) {
        //Nothing to do with Allure
    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
        //Nothing to do with Allure
    }

    @Override
    public void uri(String uri) {
        //Nothing to do with Allure
    }

    @Override
    public void feature(Feature feature) {
        this.feature = feature;
        TestResultContainer featureContainer = new TestResultContainer();
        featureContainer
                .withName(feature.getName())
                .withDescription(feature.getDescription())
                .withUuid(feature.getId())
                .withStart(System.currentTimeMillis());
        lifecycle.startTestContainer(featureContainer);
    }

    @Override
    public void scenarioOutline(ScenarioOutline so) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void examples(Examples exmpls) {
        //Nothing to do with Allure
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
        this.scenario = scenario;
        TestResult testCase = new TestResult();
        testCase
                .withName(scenario.getName())
                .withDescription(scenario.getDescription())
                .withUuid(scenario.getId())
                .withStart(System.currentTimeMillis());
        lifecycle.scheduleTestCase(this.feature.getId(), testCase);

    }

    @Override
    public void background(Background b) {
        //Nothing to do with Allure
    }

    @Override
    public void scenario(Scenario scnr) {
        //Nothing to do with Allure
        System.out.println(scnr);
    }

    @Override
    public void step(Step step) {
        synchronized (gherkinSteps) {
            gherkinSteps.add(step);
        }
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        lifecycle.stopTestContainer(scenario.getId());
        lifecycle.writeTestContainer(scenario.getId());
    }

    @Override
    public void done() {
        lifecycle.stopTestContainer(feature.getId());
        lifecycle.writeTestContainer(feature.getId());
    }

    @Override
    public void close() {
        //Nothing to do with Allure
    }

    @Override
    public void eof() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    private Step extractStep(StepDefinitionMatch match) {
        try {
            Field step = match.getClass().getDeclaredField("step");
            step.setAccessible(true);
            return (Step) step.get(match);
        } catch (ReflectiveOperationException e) {
            //shouldn't ever happen
            LOG.error(e.getMessage(), e);
            throw new CucumberException(e);
        }
    }

    private boolean isEqualSteps(Step step, Step gherkinStep) {
        return Objects.equals(step.getLine(), gherkinStep.getLine());
    }


    private void fireCanceledStep(Step unimplementedStep) {
        StepResult stepResult = new StepResult();
        stepResult.withName(unimplementedStep.getName())
                .withStart(System.currentTimeMillis())
                .withStop(System.currentTimeMillis())
                .withStatus(Status.SKIPPED);
        lifecycle.startStep(this.scenario.getId(), getStepUUID(unimplementedStep), stepResult);
        lifecycle.stopStep(getStepUUID(unimplementedStep));
        //not to change FAILED status to CANCELED in the report
        lifecycle.stopTestContainer(this.scenario.getId());
    }

    private String getStepUUID(Step step) {
        return feature.getId() + scenario.getId() + step.getName() + step.getLine();
    }
}
