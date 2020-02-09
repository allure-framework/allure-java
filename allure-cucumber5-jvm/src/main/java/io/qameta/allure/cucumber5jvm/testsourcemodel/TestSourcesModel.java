package io.qameta.allure.cucumber5jvm.testsourcemodel;

import gherkin.AstBuilder;
import gherkin.Parser;
import gherkin.ParserException;
import gherkin.TokenMatcher;
import gherkin.ast.Examples;
import gherkin.ast.Feature;
import gherkin.ast.GherkinDocument;
import gherkin.ast.Node;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Step;
import gherkin.ast.TableRow;
import io.cucumber.plugin.event.TestSourceRead;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public final class TestSourcesModel {
    private final Map<URI, TestSourceRead> pathToReadEventMap = new HashMap<>();
    private final Map<URI, GherkinDocument> pathToAstMap = new HashMap<>();
    private final Map<URI, Map<Integer, AstNode>> pathToNodeMap = new HashMap<>();

    public static ScenarioDefinition getScenarioDefinition(final AstNode astNode) {
        return astNode.node instanceof ScenarioDefinition ? (ScenarioDefinition) astNode.node
                : (ScenarioDefinition) astNode.parent.parent.node;
    }

    public void addTestSourceReadEvent(final URI path, final TestSourceRead event) {
        pathToReadEventMap.put(path, event);
    }

    public Feature getFeature(final URI path) {
        if (!pathToAstMap.containsKey(path)) {
            parseGherkinSource(path);
        }
        if (pathToAstMap.containsKey(path)) {
            return pathToAstMap.get(path).getFeature();
        }
        return null;
    }

    public AstNode getAstNode(final URI path, final int line) {
        if (!pathToNodeMap.containsKey(path)) {
            parseGherkinSource(path);
        }
        if (pathToNodeMap.containsKey(path)) {
            return pathToNodeMap.get(path).get(line);
        }
        return null;
    }

    private void parseGherkinSource(final URI path) {
        if (!pathToReadEventMap.containsKey(path)) {
            return;
        }
        final Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        final TokenMatcher matcher = new TokenMatcher();
        try {
            final GherkinDocument gherkinDocument = parser.parse(pathToReadEventMap.get(path).getSource(),
                    matcher);
            pathToAstMap.put(path, gherkinDocument);
            final Map<Integer, AstNode> nodeMap = new HashMap<>();
            final AstNode currentParent = new AstNode(gherkinDocument.getFeature(), null);
            for (ScenarioDefinition child : gherkinDocument.getFeature().getChildren()) {
                processScenarioDefinition(nodeMap, child, currentParent);
            }
            pathToNodeMap.put(path, nodeMap);
        } catch (ParserException e) {
            throw new IllegalStateException("You are using a plugin that only supports till Gherkin 5.\n"
                    + "Please check if the Gherkin provided follows the standard of Gherkin 5\n", e
            );
        }
    }

    private void processScenarioDefinition(final Map<Integer, AstNode> nodeMap, final ScenarioDefinition child,
                                           final AstNode currentParent) {
        final AstNode childNode = new AstNode(child, currentParent);
        nodeMap.put(child.getLocation().getLine(), childNode);
        for (Step step : child.getSteps()) {
            nodeMap.put(step.getLocation().getLine(), createAstNode(step, childNode));
        }
        if (child instanceof ScenarioOutline) {
            processScenarioOutlineExamples(nodeMap, (ScenarioOutline) child, childNode);
        }
    }

    private void processScenarioOutlineExamples(final Map<Integer, AstNode> nodeMap,
                                                final ScenarioOutline scenarioOutline,
                                                final AstNode childNode) {
        for (Examples examples : scenarioOutline.getExamples()) {
            final AstNode examplesNode = createAstNode(examples, childNode);
            final TableRow headerRow = examples.getTableHeader();
            final AstNode headerNode = createAstNode(headerRow, examplesNode);
            nodeMap.put(headerRow.getLocation().getLine(), headerNode);
            for (int i = 0; i < examples.getTableBody().size(); ++i) {
                final TableRow examplesRow = examples.getTableBody().get(i);
                final Node rowNode = createExamplesRowWrapperNode(examplesRow, i);
                final AstNode expandedScenarioNode = createAstNode(rowNode, examplesNode);
                nodeMap.put(examplesRow.getLocation().getLine(), expandedScenarioNode);
            }
        }
    }

    private static ExamplesRowWrapperNode createExamplesRowWrapperNode(final Node examplesRow, final int bodyRowIndex) {
        return new ExamplesRowWrapperNode(examplesRow, bodyRowIndex);
    }

    private static AstNode createAstNode(final Node node, final AstNode astNode) {
        return new AstNode(node, astNode);
    }

    static class ExamplesRowWrapperNode extends Node {
        private final int bodyRowIndex;

        public int getBodyRowIndex() {
            return bodyRowIndex;
        }

        ExamplesRowWrapperNode(final Node examplesRow, final int bodyRowIndex) {
            super(examplesRow.getLocation());
            this.bodyRowIndex = bodyRowIndex;
        }
    }

    static class AstNode {
        private final Node node;
        private final AstNode parent;

        public Node getNode() {
            return node;
        }

        public AstNode getParent() {
            return parent;
        }

        AstNode(final Node node, final AstNode parent) {
            this.node = node;
            this.parent = parent;
        }
    }
}
