/*
 *  Copyright 2016-2026 Qameta Software Inc
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
package cucumber.runtime.formatter;

import cucumber.api.event.TestSourceRead;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;

/**
 * Compatibility proxy around Cucumber feature source storage.
 *
 * <p>The proxy hides version-specific Cucumber source model APIs from the reporting plugin. Integrations use it to add source-read events and resolve feature, scenario, and step keyword metadata during execution.</p>
 */
public class TestSourcesModelProxy {

    private final TestSourcesModel testSources;

    /**
     * Creates a test sources model proxy with default configuration.
     */
    public TestSourcesModelProxy() {
        this.testSources = new TestSourcesModel();
    }

    /**
     * Adds the test source read event.
     *
     * @param path the path to read from or write to
     * @param event the framework event to process
     */
    public void addTestSourceReadEvent(final String path, final TestSourceRead event) {
        testSources.addTestSourceReadEvent(path, event);
    }

    /**
     * Returns the feature.
     *
     * @param path the path to read from or write to
     * @return the feature
     */
    public Feature getFeature(final String path) {
        return testSources.getFeature(path);
    }

    /**
     * Returns the scenario definition.
     *
     * @param path the path to read from or write to
     * @param line the source line number to resolve
     * @return the scenario definition
     */
    public ScenarioDefinition getScenarioDefinition(final String path, final int line) {
        return testSources.getScenarioDefinition(path, line);
    }

    /**
     * Returns the keyword from source.
     *
     * @param uri the feature file URI
     * @param stepLine the feature file line number of the step
     * @return the keyword from source
     */
    public String getKeywordFromSource(final String uri, final int stepLine) {
        return testSources.getKeywordFromSource(uri, stepLine);
    }
}
