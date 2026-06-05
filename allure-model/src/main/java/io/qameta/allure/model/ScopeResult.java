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
package io.qameta.allure.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a logical framework scope that links fixtures and metadata to tests.
 * <p>
 * Scopes are the adapter-facing concept for suites, classes, specs, files, describe blocks,
 * or synthetic per-test fixture groups. They are converted to Allure container files only
 * when writing fixture results.
 */
public class ScopeResult implements Serializable, WithLinks {

    private static final long serialVersionUID = 1L;

    private String uuid;
    private String name;
    private List<String> tests = new ArrayList<>();
    private List<String> childScopes = new ArrayList<>();
    private List<String> testChildren = new ArrayList<>();
    private List<ScopeFixtureResult> fixtures = new ArrayList<>();
    private List<Label> labels = new ArrayList<>();
    private List<Link> links = new ArrayList<>();
    private List<Parameter> parameters = new ArrayList<>();
    private String description;
    private String descriptionHtml;

    /**
     * Gets uuid.
     *
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets uuid.
     *
     * @param value the value
     * @return self for method chaining
     */
    public ScopeResult setUuid(final String value) {
        this.uuid = value;
        return this;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param value the value
     * @return self for method chaining
     */
    public ScopeResult setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets tests.
     *
     * @return the tests
     */
    public List<String> getTests() {
        return tests;
    }

    /**
     * Sets tests.
     *
     * @param tests the tests
     * @return self for method chaining
     */
    public ScopeResult setTests(final List<String> tests) {
        this.tests = tests;
        return this;
    }

    /**
     * Gets child scopes.
     *
     * @return the child scopes
     */
    public List<String> getChildScopes() {
        return childScopes;
    }

    /**
     * Sets child scopes.
     *
     * @param childScopes the child scopes
     * @return self for method chaining
     */
    public ScopeResult setChildScopes(final List<String> childScopes) {
        this.childScopes = childScopes;
        return this;
    }

    /**
     * Gets test children.
     *
     * @return the test children
     */
    public List<String> getTestChildren() {
        return testChildren;
    }

    /**
     * Sets test children.
     *
     * @param testChildren the test children
     * @return self for method chaining
     */
    public ScopeResult setTestChildren(final List<String> testChildren) {
        this.testChildren = testChildren;
        return this;
    }

    /**
     * Gets fixtures.
     *
     * @return the fixtures
     */
    public List<ScopeFixtureResult> getFixtures() {
        return fixtures;
    }

    /**
     * Sets fixtures.
     *
     * @param fixtures the fixtures
     * @return self for method chaining
     */
    public ScopeResult setFixtures(final List<ScopeFixtureResult> fixtures) {
        this.fixtures = fixtures;
        return this;
    }

    /**
     * Gets labels.
     *
     * @return the labels
     */
    public List<Label> getLabels() {
        return labels;
    }

    /**
     * Sets labels.
     *
     * @param labels the labels
     * @return self for method chaining
     */
    public ScopeResult setLabels(final List<Label> labels) {
        this.labels = labels;
        return this;
    }

    /**
     * Gets links.
     *
     * @return the links
     */
    @Override
    public List<Link> getLinks() {
        return links;
    }

    /**
     * Sets links.
     *
     * @param links the links
     * @return self for method chaining
     */
    public ScopeResult setLinks(final List<Link> links) {
        this.links = links;
        return this;
    }

    /**
     * Gets parameters.
     *
     * @return the parameters
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * Sets parameters.
     *
     * @param parameters the parameters
     * @return self for method chaining
     */
    public ScopeResult setParameters(final List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Gets description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets description.
     *
     * @param value the value
     * @return self for method chaining
     */
    public ScopeResult setDescription(final String value) {
        this.description = value;
        return this;
    }

    /**
     * Gets description html.
     *
     * @return the description html
     */
    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    /**
     * Sets description html.
     *
     * @param value the value
     * @return self for method chaining
     */
    public ScopeResult setDescriptionHtml(final String value) {
        this.descriptionHtml = value;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ScopeResult that = (ScopeResult) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(name, that.name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(uuid, name);
    }
}
