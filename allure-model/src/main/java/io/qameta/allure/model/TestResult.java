
package io.qameta.allure.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>Java class for TestResult complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TestResult"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:model.allure.qameta.io}ExecutableItem"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="uuid" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="historyId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="testCaseId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="rerunOf" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="fullName" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="labels" type="{urn:model.allure.qameta.io}Labels" minOccurs="0"/&gt;
 *         &lt;element name="links" type="{urn:model.allure.qameta.io}Links" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessivePublicCount"})
public class TestResult extends ExecutableItem implements Serializable, WithLinks {

    private static final long serialVersionUID = 1L;

    protected String uuid;

    protected String historyId;

    protected String testCaseId;

    protected String rerunOf;

    protected String fullName;

    protected List<Label> labels;

    protected List<Link> links;

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
    public TestResult setUuid(final String value) {
        this.uuid = value;
        return this;
    }

    /**
     * Gets the value of the historyId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHistoryId() {
        return historyId;
    }

    /**
     * Sets the value of the historyId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public TestResult setHistoryId(final String value) {
        this.historyId = value;
        return this;
    }

    /**
     * Gets the value of the testCaseId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTestCaseId() {
        return testCaseId;
    }

    /**
     * Sets the value of the testCaseId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public TestResult setTestCaseId(final String value) {
        this.testCaseId = value;
        return this;
    }

    /**
     * Gets the value of the rerunOf property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRerunOf() {
        return rerunOf;
    }

    /**
     * Sets the value of the rerunOf property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public TestResult setRerunOf(final String value) {
        this.rerunOf = value;
        return this;
    }

    /**
     * Gets the value of the fullName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the value of the fullName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public TestResult setFullName(final String value) {
        this.fullName = value;
        return this;
    }

    public List<Label> getLabels() {
        if (labels == null) {
            labels = new ArrayList<>();
        }
        return labels;
    }

    public TestResult setLabels(final List<Label> labels) {
        this.labels = labels;
        return this;
    }

    public TestResult setLabels(final Label... values) {
        if (values != null) {
            for (Label value: values) {
                getLabels().add(value);
            }
        }
        return this;
    }

    public TestResult setLabels(final Collection<Label> values) {
        if (values != null) {
            getLabels().addAll(values);
        }
        return this;
    }

    public List<Link> getLinks() {
        if (links == null) {
            links = new ArrayList<>();
        }
        return links;
    }

    public TestResult setLinks(final List<Link> links) {
        this.links = links;
        return this;
    }

    public TestResult setLinks(final Link... values) {
        if (values != null) {
            for (Link value: values) {
                getLinks().add(value);
            }
        }
        return this;
    }

    public TestResult setLinks(final Collection<Link> values) {
        if (values != null) {
            getLinks().addAll(values);
        }
        return this;
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withUuid(final String value) {
        return setUuid(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withHistoryId(final String value) {
        return setHistoryId(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withTestCaseId(final String value) {
        return setTestCaseId(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withRerunOf(final String value) {
        return setRerunOf(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withFullName(final String value) {
        return setFullName(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withLabels(final Label... values) {
        return setLabels(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withLabels(final Collection<Label> values) {
        return setLabels(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withLabels(final List<Label> labels) {
        return setLabels(labels);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withLinks(final Link... values) {
        return setLinks(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withLinks(final Collection<Link> values) {
        return setLinks(values);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestResult withLinks(final List<Link> links) {
        return setLinks(links);
    }

    @Override
    public TestResult withName(final String value) {
        setName(value);
        return this;
    }

    @Override
    public TestResult withStatus(final Status value) {
        setStatus(value);
        return this;
    }

    @Override
    public TestResult withStatusDetails(final StatusDetails value) {
        setStatusDetails(value);
        return this;
    }

    @Override
    public TestResult withStage(final Stage value) {
        setStage(value);
        return this;
    }

    @Override
    public TestResult withDescription(final String value) {
        setDescription(value);
        return this;
    }

    @Override
    public TestResult withDescriptionHtml(final String value) {
        setDescriptionHtml(value);
        return this;
    }

    @Override
    public TestResult withStart(final Long value) {
        setStart(value);
        return this;
    }

    @Override
    public TestResult withStop(final Long value) {
        setStop(value);
        return this;
    }

    @Override
    public TestResult withSteps(final StepResult... values) {
        if (values != null) {
            for (StepResult value: values) {
                getSteps().add(value);
            }
        }
        return this;
    }

    @Override
    public TestResult withSteps(final Collection<StepResult> values) {
        if (values != null) {
            getSteps().addAll(values);
        }
        return this;
    }

    @Override
    public TestResult withSteps(final List<StepResult> steps) {
        setSteps(steps);
        return this;
    }

    @Override
    public TestResult withAttachments(final Attachment... values) {
        if (values != null) {
            for (Attachment value: values) {
                getAttachments().add(value);
            }
        }
        return this;
    }

    @Override
    public TestResult withAttachments(final Collection<Attachment> values) {
        if (values != null) {
            getAttachments().addAll(values);
        }
        return this;
    }

    @Override
    public TestResult withAttachments(final List<Attachment> attachments) {
        setAttachments(attachments);
        return this;
    }

    @Override
    public TestResult withParameters(final Parameter... values) {
        if (values != null) {
            for (Parameter value: values) {
                getParameters().add(value);
            }
        }
        return this;
    }

    @Override
    public TestResult withParameters(final Collection<Parameter> values) {
        if (values != null) {
            getParameters().addAll(values);
        }
        return this;
    }

    @Override
    public TestResult withParameters(final List<Parameter> parameters) {
        setParameters(parameters);
        return this;
    }
}
