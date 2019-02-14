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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * POJO that stores information about test fixtures.
 */
@SuppressWarnings("PMD.ExcessivePublicCount")
public class TestResultContainer implements Serializable, WithLinks {

    private static final long serialVersionUID = 1L;

    protected String uuid;

    protected String name;

    protected List<String> children;

    protected String description;

    protected String descriptionHtml;

    protected List<FixtureResult> befores;

    protected List<FixtureResult> afters;

    protected List<Link> links;

    protected Long start;

    protected Long stop;

    /**
     * Gets the value of the uuid property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the value of the uuid property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public TestResultContainer setUuid(final String value) {
        this.uuid = value;
        return this;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public TestResultContainer setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets the value of the description property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public TestResultContainer setDescription(final String value) {
        this.description = value;
        return this;
    }

    /**
     * Gets the value of the descriptionHtml property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    /**
     * Sets the value of the descriptionHtml property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public TestResultContainer setDescriptionHtml(final String value) {
        this.descriptionHtml = value;
        return this;
    }

    /**
     * Gets the value of the start property.
     *
     * @return possible object is
     * {@link Long }
     */
    public Long getStart() {
        return start;
    }

    /**
     * Sets the value of the start property.
     *
     * @param value allowed object is
     *              {@link Long }
     */
    public TestResultContainer setStart(final Long value) {
        this.start = value;
        return this;
    }

    /**
     * Gets the value of the stop property.
     *
     * @return possible object is
     * {@link Long }
     */
    public Long getStop() {
        return stop;
    }

    /**
     * Sets the value of the stop property.
     *
     * @param value allowed object is
     *              {@link Long }
     */
    public TestResultContainer setStop(final Long value) {
        this.stop = value;
        return this;
    }

    public List<String> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    @JsonProperty
    public TestResultContainer setChildren(final List<String> children) {
        this.children = children;
        return this;
    }

    /**
     * @deprecated use {@link #setChildren(List)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer setChildren(final String... values) {
        if (values != null) {
            for (String value : values) {
                getChildren().add(value);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #setChildren(List)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer setChildren(final Collection<String> values) {
        if (values != null) {
            getChildren().addAll(values);
        }
        return this;
    }

    public List<FixtureResult> getBefores() {
        if (befores == null) {
            befores = new ArrayList<>();
        }
        return befores;
    }

    @JsonProperty
    public TestResultContainer setBefores(final List<FixtureResult> befores) {
        this.befores = befores;
        return this;
    }

    /**
     * @deprecated use {@link #setBefores(List)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer setBefores(final FixtureResult... values) {
        if (values != null) {
            for (FixtureResult value : values) {
                getBefores().add(value);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #setBefores(List)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer setBefores(final Collection<FixtureResult> values) {
        if (values != null) {
            getBefores().addAll(values);
        }
        return this;
    }

    public List<FixtureResult> getAfters() {
        if (afters == null) {
            afters = new ArrayList<>();
        }
        return afters;
    }

    @JsonProperty
    public TestResultContainer setAfters(final List<FixtureResult> afters) {
        this.afters = afters;
        return this;
    }

    /**
     * @deprecated use {@link #setAfters(List)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer setAfters(final FixtureResult... values) {
        if (values != null) {
            for (FixtureResult value : values) {
                getAfters().add(value);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #setAfters(List)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer setAfters(final Collection<FixtureResult> values) {
        if (values != null) {
            getAfters().addAll(values);
        }
        return this;
    }

    @Override
    public List<Link> getLinks() {
        if (links == null) {
            links = new ArrayList<>();
        }
        return links;
    }

    @JsonProperty
    public TestResultContainer setLinks(final List<Link> links) {
        this.links = links;
        return this;
    }

    /**
     * @deprecated use {@link #setLinks(List)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer setLinks(final Link... values) {
        if (values != null) {
            for (Link value : values) {
                getLinks().add(value);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #setLinks(List)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer setLinks(final Collection<Link> values) {
        if (values != null) {
            getLinks().addAll(values);
        }
        return this;
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withUuid(final String value) {
        return setUuid(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withName(final String value) {
        return setName(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withDescription(final String value) {
        return setDescription(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withDescriptionHtml(final String value) {
        return setDescriptionHtml(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withStart(final Long value) {
        return setStart(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withStop(final Long value) {
        return setStop(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withChildren(final String... values) {
        return setChildren(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withChildren(final Collection<String> values) {
        return setChildren(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withChildren(final List<String> children) {
        return setChildren(children);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withBefores(final FixtureResult... values) {
        return setBefores(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withBefores(final Collection<FixtureResult> values) {
        return setBefores(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withBefores(final List<FixtureResult> befores) {
        return setBefores(befores);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withAfters(final FixtureResult... values) {
        return setAfters(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withAfters(final Collection<FixtureResult> values) {
        return setAfters(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withAfters(final List<FixtureResult> afters) {
        return setAfters(afters);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withLinks(final Link... values) {
        return setLinks(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withLinks(final Collection<Link> values) {
        return setLinks(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    @JsonIgnore
    public TestResultContainer withLinks(final List<Link> links) {
        return setLinks(links);
    }
}
