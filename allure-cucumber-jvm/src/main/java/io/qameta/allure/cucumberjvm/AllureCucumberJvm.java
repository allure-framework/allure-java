package io.qameta.allure.cucumberjvm;

import gherkin.I18n;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.TestResultContainer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllureCucumberJvm implements Reporter, Formatter {

    private static final Logger LOG = LoggerFactory.getLogger(AllureCucumberJvm.class);
    private static final List<String> SCENARIO_OUTLINE_KEYWORDS = Collections.synchronizedList(new ArrayList<String>());

    private AllureLifecycle lifecycle;
    private Feature feature;

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
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void after(Match match, Result result) {
        //Nothing to do with Allure
    }

    @Override
    public void match(Match match) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        TestResultContainer container = new TestResultContainer();
        container
                .withName(scenario.getName())
                .withDescription(scenario.getDescription())
                .withUuid(scenario.getId())
                .withStart(System.currentTimeMillis());
        lifecycle.startTestContainer(this.feature.getId(), container);

    }

    @Override
    public void background(Background b) {
        //Nothing to do with Allure
    }

    @Override
    public void scenario(Scenario scnr) {
        //Nothing to do with Allure
    }

    @Override
    public void step(Step step) {
        step.
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

}
