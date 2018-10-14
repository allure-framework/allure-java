
package io.qameta.allure.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>Java class for TestResultContainer complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TestResultContainer"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="uuid" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="children" type="{urn:model.allure.qameta.io}Ids"/&gt;
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="descriptionHtml" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="befores" type="{urn:model.allure.qameta.io}Befores"/&gt;
 *         &lt;element name="afters" type="{urn:model.allure.qameta.io}Afters"/&gt;
 *         &lt;element name="links" type="{urn:model.allure.qameta.io}Links"/&gt;
 *       &lt;/all&gt;
 *       &lt;attribute name="start" type="{http://www.w3.org/2001/XMLSchema}long" /&gt;
 *       &lt;attribute name="stop" type="{http://www.w3.org/2001/XMLSchema}long" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
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
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the value of the uuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public TestResultContainer setUuid(final String value) {
        this.uuid = value;
        return this;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public TestResultContainer setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public TestResultContainer setDescription(final String value) {
        this.description = value;
        return this;
    }

    /**
     * Gets the value of the descriptionHtml property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    /**
     * Sets the value of the descriptionHtml property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public TestResultContainer setDescriptionHtml(final String value) {
        this.descriptionHtml = value;
        return this;
    }

    /**
     * Gets the value of the start property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getStart() {
        return start;
    }

    /**
     * Sets the value of the start property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public TestResultContainer setStart(final Long value) {
        this.start = value;
        return this;
    }

    /**
     * Gets the value of the stop property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getStop() {
        return stop;
    }

    /**
     * Sets the value of the stop property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
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

    public TestResultContainer setChildren(final List<String> children) {
        this.children = children;
        return this;
    }

    public TestResultContainer setChildren(final String... values) {
        if (values != null) {
            for (String value: values) {
                getChildren().add(value);
            }
        }
        return this;
    }

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

    public TestResultContainer setBefores(final List<FixtureResult> befores) {
        this.befores = befores;
        return this;
    }

    public TestResultContainer setBefores(final FixtureResult... values) {
        if (values != null) {
            for (FixtureResult value: values) {
                getBefores().add(value);
            }
        }
        return this;
    }

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

    public TestResultContainer setAfters(final List<FixtureResult> afters) {
        this.afters = afters;
        return this;
    }

    public TestResultContainer setAfters(final FixtureResult... values) {
        if (values != null) {
            for (FixtureResult value: values) {
                getAfters().add(value);
            }
        }
        return this;
    }

    public TestResultContainer setAfters(final Collection<FixtureResult> values) {
        if (values != null) {
            getAfters().addAll(values);
        }
        return this;
    }

    public List<Link> getLinks() {
        if (links == null) {
            links = new ArrayList<>();
        }
        return links;
    }

    public TestResultContainer setLinks(final List<Link> links) {
        this.links = links;
        return this;
    }

    public TestResultContainer setLinks(final Link... values) {
        if (values != null) {
            for (Link value: values) {
                getLinks().add(value);
            }
        }
        return this;
    }

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
    public TestResultContainer withUuid(final String value) {
        return setUuid(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withName(final String value) {
        return setName(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withDescription(final String value) {
        return setDescription(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withDescriptionHtml(final String value) {
        return setDescriptionHtml(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withStart(final Long value) {
        return setStart(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withStop(final Long value) {
        return setStop(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withChildren(final String... values) {
        return setChildren(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withChildren(final Collection<String> values) {
        return setChildren(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withChildren(final List<String> children) {
        return setChildren(children);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withBefores(final FixtureResult... values) {
        return setBefores(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withBefores(final Collection<FixtureResult> values) {
        return setBefores(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withBefores(final List<FixtureResult> befores) {
        return setBefores(befores);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withAfters(final FixtureResult... values) {
        return setAfters(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withAfters(final Collection<FixtureResult> values) {
        return setAfters(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withAfters(final List<FixtureResult> afters) {
        return setAfters(afters);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withLinks(final Link... values) {
        return setLinks(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withLinks(final Collection<Link> values) {
        return setLinks(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResultContainer withLinks(final List<Link> links) {
        return setLinks(links);
    }
}
