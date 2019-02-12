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
package io.qameta.allure.cucumber2jvm;

import cucumber.api.event.TestSourceRead;

import gherkin.Parser;
import gherkin.AstBuilder;
import gherkin.TokenMatcher;
import gherkin.ParserException;
import gherkin.GherkinDialect;
import gherkin.GherkinDialectProvider;

import gherkin.ast.GherkinDocument;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Node;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.TableRow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Parts of package-private cucumber.runtime.formatter.TestSourcesModel needed for Allure 2 adapter.
 */
public final class CucumberSourceUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CucumberSourceUtils.class);

    private final Map<String, TestSourceRead> pathToReadEventMap = new HashMap<>();
    private final Map<String, GherkinDocument> pathToAstMap = new HashMap<>();
    private final Map<String, Map<Integer, AstNode>> pathToNodeMap = new HashMap<>();

    public void addTestSourceReadEvent(final String path, final TestSourceRead event) {
        pathToReadEventMap.put(path, event);
    }

    public Feature getFeature(final String path) {
        if (!pathToAstMap.containsKey(path)) {
            parseGherkinSource(path);
        }
        if (pathToAstMap.containsKey(path)) {
            return pathToAstMap.get(path).getFeature();
        }
        return null;
    }

    private void parseGherkinSource(final String path) {
        if (!pathToReadEventMap.containsKey(path)) {
            return;
        }
        final Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        final TokenMatcher matcher = new TokenMatcher();
        try {
            final GherkinDocument gherkinDocument = parser.parse(pathToReadEventMap.get(path).source, matcher);
            pathToAstMap.put(path, gherkinDocument);
            final Map<Integer, AstNode> nodeMap = new HashMap<>();
            final AstNode currentParent = new AstNode(gherkinDocument.getFeature(), null);
            for (ScenarioDefinition child : gherkinDocument.getFeature().getChildren()) {
                processScenarioDefinition(nodeMap, child, currentParent);
            }
            pathToNodeMap.put(path, nodeMap);
        } catch (ParserException e) {
            LOGGER.trace(e.getMessage(), e);
        }
    }

    private void processScenarioDefinition(
            final Map<Integer, AstNode> nodeMap, final ScenarioDefinition child, final AstNode currentParent
    ) {
        final AstNode childNode = new AstNode(child, currentParent);
        nodeMap.put(child.getLocation().getLine(), childNode);

        child.getSteps().forEach(
            step -> nodeMap.put(step.getLocation().getLine(), new AstNode(step, childNode))
        );

        if (child instanceof ScenarioOutline) {
            processScenarioOutlineExamples(nodeMap, (ScenarioOutline) child, childNode);
        }
    }

    private void processScenarioOutlineExamples(
            final Map<Integer, AstNode> nodeMap, final ScenarioOutline scenarioOutline, final AstNode childNode
    ) {
        scenarioOutline.getExamples().forEach(examples -> {
            final AstNode examplesNode = new AstNode(examples, childNode);
            final TableRow headerRow = examples.getTableHeader();
            final AstNode headerNode = new AstNode(headerRow, examplesNode);
            nodeMap.put(headerRow.getLocation().getLine(), headerNode);
            IntStream.range(0, examples.getTableBody().size()).forEach(i -> {
                final TableRow examplesRow = examples.getTableBody().get(i);
                final Node rowNode = new CucumberSourceUtils.ExamplesRowWrapperNode(examplesRow, i);
                final AstNode expandedScenarioNode = new AstNode(rowNode, examplesNode);
                nodeMap.put(examplesRow.getLocation().getLine(), expandedScenarioNode);
            });
        });
    }

    private AstNode getAstNode(final String path, final int line) {
        if (!pathToNodeMap.containsKey(path)) {
            parseGherkinSource(path);
        }
        if (pathToNodeMap.containsKey(path)) {
            return pathToNodeMap.get(path).get(line);
        }
        return null;
    }

    public ScenarioDefinition getScenarioDefinition(final String path, final int line) {
        return getScenarioDefinition(getAstNode(path, line));
    }

    private ScenarioDefinition getScenarioDefinition(final AstNode astNode) {
        return astNode.getNode() instanceof ScenarioDefinition
                ? (ScenarioDefinition) astNode.getNode()
                : (ScenarioDefinition) astNode.getParent().getParent().getNode();
    }

    public String getKeywordFromSource(final String uri, final int stepLine) {
        final Feature feature = getFeature(uri);
        if (feature != null) {
            final TestSourceRead event = getTestSourceReadEvent(uri);
            final String trimmedSourceLine = event.source.split("\n")[stepLine - 1].trim();
            final GherkinDialect dialect = new GherkinDialectProvider(feature.getLanguage()).getDefaultDialect();
            for (String keyword : dialect.getStepKeywords()) {
                if (trimmedSourceLine.startsWith(keyword)) {
                    return keyword;
                }
            }
        }
        return "";
    }

    private TestSourceRead getTestSourceReadEvent(final String uri) {
        if (pathToReadEventMap.containsKey(uri)) {
            return pathToReadEventMap.get(uri);
        }
        return null;
    }

    /**
     * Representation of Examples row.
     */
    private static class ExamplesRowWrapperNode extends Node {
        private final int bodyRowIndex;

        ExamplesRowWrapperNode(final Node examplesRow, final int bodyRowIndex) {
            super(examplesRow.getLocation());
            this.bodyRowIndex = bodyRowIndex;
        }

        public int getBodyRowIndex() {
            return bodyRowIndex;
        }
    }

    /**
     * Representation of leaf node.
     */
    private static class AstNode {
        private final Node node;
        private final AstNode parent;

        AstNode(final Node node, final AstNode parent) {
            this.node = node;
            this.parent = parent;
        }

        public Node getNode() {
            return node;
        }

        public AstNode getParent() {
            return parent;
        }
    }
}
