package cucumber.runtime.formatter;

import cucumber.api.event.TestSourceRead;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;

public class TestSourcesModelProxy {

    private final TestSourcesModel testSources;

    public TestSourcesModelProxy(){
        this.testSources = new TestSourcesModel();
    }

    public void addTestSourceReadEvent(String path, TestSourceRead event) {
        testSources.addTestSourceReadEvent(path, event);
    }

    public Feature getFeature(final String path) {
        return testSources.getFeature(path);
    }

    public ScenarioDefinition getScenarioDefinition(String path, int line) {
        return testSources.getScenarioDefinition(path, line);
    }

    public String getKeywordFromSource(String uri, int stepLine) {
        return testSources.getKeywordFromSource(uri, stepLine);
    }
}
