package cucumber.runtime.formatter;

import cucumber.api.event.TestSourceRead;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;

/**
 * Proxy class to internal Cucumber implementation of TestSourcesModel.
 */
public class TestSourcesModelProxy {

    private final TestSourcesModel testSources;

    public TestSourcesModelProxy() {
        this.testSources = new TestSourcesModel();
    }

    public void addTestSourceReadEvent(final String path, final TestSourceRead event) {
        testSources.addTestSourceReadEvent(path, event);
    }

    public Feature getFeature(final String path) {
        return testSources.getFeature(path);
    }

    public ScenarioDefinition getScenarioDefinition(final String path, final int line) {
        return testSources.getScenarioDefinition(path, line);
    }

    public String getKeywordFromSource(final String uri, final int stepLine) {
        return testSources.getKeywordFromSource(uri, stepLine);
    }
}
