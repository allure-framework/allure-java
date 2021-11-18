/*
 *  Copyright 2020 Qameta Software OÜ
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
package io.qameta.allure.cucumber6jvm.testsourcemodel;

import io.cucumber.gherkin.Gherkin;
import io.cucumber.messages.Messages;
import io.cucumber.messages.Messages.GherkinDocument;
import io.cucumber.messages.Messages.GherkinDocument.Feature;
import io.cucumber.messages.Messages.GherkinDocument.Feature.Background;
import io.cucumber.messages.Messages.GherkinDocument.Feature.FeatureChild;
import io.cucumber.messages.Messages.GherkinDocument.Feature.FeatureChild.RuleChild;
import io.cucumber.messages.Messages.GherkinDocument.Feature.Scenario;
import io.cucumber.messages.Messages.GherkinDocument.Feature.Scenario.Examples;
import io.cucumber.messages.Messages.GherkinDocument.Feature.Step;
import io.cucumber.messages.Messages.GherkinDocument.Feature.TableRow;
import io.cucumber.messages.internal.com.google.protobuf.GeneratedMessageV3;
import io.cucumber.plugin.event.TestSourceRead;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static io.cucumber.gherkin.Gherkin.makeSourceEnvelope;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class TestSourcesModel {
    private final Map<URI, TestSourceRead> pathToReadEventMap = new HashMap<>();
    private final Map<URI, GherkinDocument> pathToAstMap = new HashMap<>();
    private final Map<URI, Map<Integer, AstNode>> pathToNodeMap = new HashMap<>();

    public static Scenario getScenarioDefinition(final AstNode astNode) {
        AstNode candidate = astNode;
        while (candidate != null && !(candidate.node instanceof Scenario)) {
            candidate = candidate.parent;
        }
        return candidate == null ? null : (Scenario) candidate.node;
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

    private void parseGherkinSource(final URI path) {
        if (!pathToReadEventMap.containsKey(path)) {
            return;
        }
        final String source = pathToReadEventMap.get(path).getSource();

        final List<Messages.Envelope> sources = singletonList(
                makeSourceEnvelope(source, path.toString()));

        final List<Messages.Envelope> envelopes = Gherkin.fromSources(
                sources,
                true,
                true,
                true,
                () -> String.valueOf(UUID.randomUUID())).collect(toList());

        final GherkinDocument gherkinDocument = envelopes.stream()
                .filter(Messages.Envelope::hasGherkinDocument)
                .map(Messages.Envelope::getGherkinDocument)
                .findFirst()
                .orElse(null);

        pathToAstMap.put(path, gherkinDocument);
        final Map<Integer, AstNode> nodeMap = new HashMap<>();
        final AstNode currentParent = createAstNode(Objects.requireNonNull(gherkinDocument).getFeature(), null);
        for (FeatureChild child : gherkinDocument.getFeature().getChildrenList()) {
            processFeatureDefinition(nodeMap, child, currentParent);
        }
        pathToNodeMap.put(path, nodeMap);

    }

    private void processFeatureDefinition(
            final Map<Integer, AstNode> nodeMap, final FeatureChild child, final AstNode currentParent) {
        if (child.hasBackground()) {
            processBackgroundDefinition(nodeMap, child.getBackground(), currentParent);
        } else if (child.hasScenario()) {
            processScenarioDefinition(nodeMap, child.getScenario(), currentParent);
        } else if (child.hasRule()) {
            final AstNode childNode = createAstNode(child.getRule(), currentParent);
            nodeMap.put(child.getRule().getLocation().getLine(), childNode);
            for (RuleChild ruleChild : child.getRule().getChildrenList()) {
                processRuleDefinition(nodeMap, ruleChild, childNode);
            }
        }
    }

    private void processBackgroundDefinition(
            final Map<Integer, AstNode> nodeMap, final Background background, final AstNode currentParent
    ) {
        final AstNode childNode = createAstNode(background, currentParent);
        nodeMap.put(background.getLocation().getLine(), childNode);
        for (Step step : background.getStepsList()) {
            nodeMap.put(step.getLocation().getLine(), createAstNode(step, childNode));
        }
    }

    private void processScenarioDefinition(
            final Map<Integer, AstNode> nodeMap, final Scenario child, final AstNode currentParent) {
        final AstNode childNode = createAstNode(child, currentParent);
        nodeMap.put(child.getLocation().getLine(), childNode);
        for (Step step : child.getStepsList()) {
            nodeMap.put(step.getLocation().getLine(), createAstNode(step, childNode));
        }
        if (child.getExamplesCount() > 0) {
            processScenarioOutlineExamples(nodeMap, child, childNode);
        }
    }

    private void processRuleDefinition(
            final Map<Integer, AstNode> nodeMap, final RuleChild child, final AstNode currentParent) {
        if (child.hasBackground()) {
            processBackgroundDefinition(nodeMap, child.getBackground(), currentParent);
        } else if (child.hasScenario()) {
            processScenarioDefinition(nodeMap, child.getScenario(), currentParent);
        }
    }

    private void processScenarioOutlineExamples(
            final Map<Integer, AstNode> nodeMap, final Scenario scenarioOutline, final AstNode parent
    ) {
        for (Examples examples : scenarioOutline.getExamplesList()) {
            final AstNode examplesNode = createAstNode(examples, parent);
            final TableRow headerRow = examples.getTableHeader();
            final AstNode headerNode = createAstNode(headerRow, examplesNode);
            nodeMap.put(headerRow.getLocation().getLine(), headerNode);
            for (int i = 0; i < examples.getTableBodyCount(); ++i) {
                final TableRow examplesRow = examples.getTableBody(i);
                final AstNode expandedScenarioNode = createAstNode(examplesRow, examplesNode);
                nodeMap.put(examplesRow.getLocation().getLine(), expandedScenarioNode);
            }
        }
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

    private AstNode createAstNode(final GeneratedMessageV3 node, final AstNode astNode) {
        return new AstNode(node, astNode);
    }

    private static class AstNode {
        private final GeneratedMessageV3 node;
        private final AstNode parent;

        AstNode(final GeneratedMessageV3 node, final AstNode parent) {
            this.node = node;
            this.parent = parent;
        }
    }
}
