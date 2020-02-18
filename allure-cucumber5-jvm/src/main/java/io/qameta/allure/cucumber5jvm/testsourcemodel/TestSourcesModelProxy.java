/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.cucumber5jvm.testsourcemodel;

import gherkin.GherkinDialect;
import gherkin.GherkinDialectProvider;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import io.cucumber.plugin.event.TestSourceRead;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


public class TestSourcesModelProxy {

    private final Map<URI, TestSourceRead> pathToReadEventMap = new HashMap<>();
    private final TestSourcesModel testSources;

    public TestSourcesModelProxy() {
        this.testSources = new TestSourcesModel();
    }

    public void addTestSourceReadEvent(final URI path, final TestSourceRead event) {
        this.pathToReadEventMap.put(path, event);
        testSources.addTestSourceReadEvent(path, event);
    }

    public Feature getFeature(final URI path) {
        return testSources.getFeature(path);
    }

    public ScenarioDefinition getScenarioDefinition(final URI path, final int line) {
        return testSources.getScenarioDefinition(testSources.getAstNode(path, line));
    }

    public String getKeywordFromSource(final URI uri, final int stepLine) {
        return this.getKeywordFromSourceInternal(uri, stepLine);
    }

    private String getKeywordFromSourceInternal(final URI uri, final int stepLine) {
        final Feature feature = getFeature(uri);
        if (feature != null) {
            final TestSourceRead event = this.getTestSourceReadEvent(uri);
            final String trimmedSourceLine = event.getSource().split("\n")[stepLine - 1].trim();
            final GherkinDialect dialect = new GherkinDialectProvider(feature.getLanguage()).getDefaultDialect();
            for (String keyword : dialect.getStepKeywords()) {
                if (trimmedSourceLine.startsWith(keyword)) {
                    return keyword;
                }
            }
        }
        return "";
    }

    private TestSourceRead getTestSourceReadEvent(final URI uri) {
        if (this.pathToReadEventMap.containsKey(uri)) {
            return pathToReadEventMap.get(uri);
        }
        return null;
    }
}
