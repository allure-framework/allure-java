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
package io.qameta.allure.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The model object that stores links between test results and test fixtures.
 * <p>
 * During report generation all {@link #befores} and {@link #afters} is added to each
 * test result that {@link TestResult#getUuid()} matches values, specified in {@link #children}.
 * <p>
 * Containers that have empty {@link #children} are simply ignored.
 *
 * @author baev (Dmitry Baev)
 * @see io.qameta.allure.model.TestResult
 * @see io.qameta.allure.model.WithLinks
 * @since 2.0
 */
@SuppressWarnings("PMD.ExcessivePublicCount")
public class TestResultContainer implements Serializable, WithLinks {

    private static final long serialVersionUID = 1L;

    private String uuid;
    private String name;
    private List<String> children;
    private String description;
    private String descriptionHtml;
    private List<FixtureResult> befores;
    private List<FixtureResult> afters;
    private List<Link> links;
    private Long start;
    private Long stop;

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
    public TestResultContainer setUuid(final String value) {
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
    public TestResultContainer setName(final String value) {
        this.name = value;
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
    public TestResultContainer setDescription(final String value) {
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
    public TestResultContainer setDescriptionHtml(final String value) {
        this.descriptionHtml = value;
        return this;
    }

    /**
     * Gets start.
     *
     * @return the start
     */
    public Long getStart() {
        return start;
    }

    /**
     * Sets start.
     *
     * @param value the value
     * @return self for method chaining
     */
    public TestResultContainer setStart(final Long value) {
        this.start = value;
        return this;
    }

    /**
     * Gets stop.
     *
     * @return the stop
     */
    public Long getStop() {
        return stop;
    }

    /**
     * Sets stop.
     *
     * @param value the value
     * @return self for method chaining
     */
    public TestResultContainer setStop(final Long value) {
        this.stop = value;
        return this;
    }

    /**
     * Gets children.
     *
     * @return the children
     */
    public List<String> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    /**
     * Sets children.
     *
     * @param children the children
     * @return self for method chaining
     */
    public TestResultContainer setChildren(final List<String> children) {
        this.children = children;
        return this;
    }

    /**
     * Gets befores.
     *
     * @return the befores
     */
    public List<FixtureResult> getBefores() {
        if (befores == null) {
            befores = new ArrayList<>();
        }
        return befores;
    }

    /**
     * Sets befores.
     *
     * @param befores the befores
     * @return self for method chaining
     */
    public TestResultContainer setBefores(final List<FixtureResult> befores) {
        this.befores = befores;
        return this;
    }

    /**
     * Gets afters.
     *
     * @return the afters
     */
    public List<FixtureResult> getAfters() {
        if (afters == null) {
            afters = new ArrayList<>();
        }
        return afters;
    }

    /**
     * Sets afters.
     *
     * @param afters the afters
     * @return self for method chaining
     */
    public TestResultContainer setAfters(final List<FixtureResult> afters) {
        this.afters = afters;
        return this;
    }

    /**
     * Gets links.
     *
     * @return the links
     */
    @Override
    public List<Link> getLinks() {
        if (links == null) {
            links = new ArrayList<>();
        }
        return links;
    }

    /**
     * Sets links.
     *
     * @param links the links
     * @return self for method chaining
     */
    public TestResultContainer setLinks(final List<Link> links) {
        this.links = links;
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
        final TestResultContainer that = (TestResultContainer) o;
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
