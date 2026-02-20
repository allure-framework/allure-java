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
