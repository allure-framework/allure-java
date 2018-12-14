package io.qameta.allure.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO that stores test result information.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessivePublicCount", "deprecation"})
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
    public TestResult setUuid(final String value) {
        this.uuid = value;
        return this;
    }

    /**
     * Gets the value of the historyId property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getHistoryId() {
        return historyId;
    }

    /**
     * Sets the value of the historyId property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public TestResult setHistoryId(final String value) {
        this.historyId = value;
        return this;
    }

    /**
     * Gets the value of the testCaseId property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getTestCaseId() {
        return testCaseId;
    }

    /**
     * Sets the value of the testCaseId property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public TestResult setTestCaseId(final String value) {
        this.testCaseId = value;
        return this;
    }

    /**
     * Gets the value of the rerunOf property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getRerunOf() {
        return rerunOf;
    }

    /**
     * Sets the value of the rerunOf property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public TestResult setRerunOf(final String value) {
        this.rerunOf = value;
        return this;
    }

    /**
     * Gets the value of the fullName property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the value of the fullName property.
     *
     * @param value allowed object is
     *              {@link String }
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

    @JsonProperty
    public TestResult setLabels(final List<Label> labels) {
        this.labels = labels;
        return this;
    }

    /**
     * @deprecated use {@link #getLabels()} ()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResult setLabels(final Label... values) {
        if (values != null) {
            for (Label value : values) {
                getLabels().add(value);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #getLabels()} ()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResult setLabels(final Collection<Label> values) {
        if (values != null) {
            getLabels().addAll(values);
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
    public TestResult setLinks(final List<Link> links) {
        this.links = links;
        return this;
    }

    /**
     * @deprecated use {@link #getLinks()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResult setLinks(final Link... values) {
        if (values != null) {
            for (Link value : values) {
                getLinks().add(value);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #getLinks()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @JsonIgnore
    public TestResult setLinks(final Collection<Link> values) {
        if (values != null) {
            getLinks().addAll(values);
        }
        return this;
    }

    @Override
    public TestResult setName(final String value) {
        super.setName(value);
        return this;
    }

    @Override
    public TestResult setStatus(final Status value) {
        super.setStatus(value);
        return this;
    }

    @Override
    public TestResult setStatusDetails(final StatusDetails value) {
        super.setStatusDetails(value);
        return this;
    }

    @Override
    public TestResult setStage(final Stage value) {
        super.setStage(value);
        return this;
    }

    @Override
    public TestResult setDescription(final String value) {
        super.setDescription(value);
        return this;
    }

    @Override
    public TestResult setDescriptionHtml(final String value) {
        super.setDescriptionHtml(value);
        return this;
    }

    @Override
    public TestResult setStart(final Long value) {
        super.setStart(value);
        return this;
    }

    @Override
    public TestResult setStop(final Long value) {
        super.setStop(value);
        return this;
    }

    @Override
    @JsonProperty
    public TestResult setSteps(final List<StepResult> steps) {
        super.setSteps(steps);
        return this;
    }

    /**
     * @deprecated use {@link #getSteps()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @Override
    @JsonIgnore
    public TestResult setSteps(final StepResult... values) {
        super.setSteps(values);
        return this;
    }

    /**
     * @deprecated use {@link #getSteps()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @Override
    @JsonIgnore
    public TestResult setSteps(final Collection<StepResult> values) {
        super.setSteps(values);
        return this;
    }

    @Override
    @JsonProperty
    public TestResult setAttachments(final List<Attachment> attachments) {
        super.setAttachments(attachments);
        return this;
    }

    /**
     * @deprecated use {@link #getAttachments()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @Override
    @JsonIgnore
    public TestResult setAttachments(final Attachment... values) {
        super.setAttachments(values);
        return this;
    }

    /**
     * @deprecated use {@link #getAttachments()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @Override
    @JsonIgnore
    public TestResult setAttachments(final Collection<Attachment> values) {
        super.setAttachments(values);
        return this;
    }

    @Override
    @JsonProperty
    public TestResult setParameters(final List<Parameter> parameters) {
        super.setParameters(parameters);
        return this;
    }

    /**
     * @deprecated use {@link #getParameters()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @Override
    @JsonIgnore
    public TestResult setParameters(final Parameter... values) {
        super.setParameters(values);
        return this;
    }

    /**
     * @deprecated use {@link #getParameters()} and {@link Collection#addAll(Collection)} instead.
     */
    @Deprecated
    @Override
    @JsonIgnore
    public TestResult setParameters(final Collection<Parameter> values) {
        super.setParameters(values);
        return this;
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public TestResult withUuid(final String value) {
        return setUuid(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public TestResult withHistoryId(final String value) {
        return setHistoryId(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public TestResult withTestCaseId(final String value) {
        return setTestCaseId(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public TestResult withRerunOf(final String value) {
        return setRerunOf(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public TestResult withFullName(final String value) {
        return setFullName(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public TestResult withLabels(final Label... values) {
        return setLabels(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public TestResult withLabels(final Collection<Label> values) {
        return setLabels(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public TestResult withLabels(final List<Label> labels) {
        return setLabels(labels);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public TestResult withLinks(final Link... values) {
        return setLinks(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
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


    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withName(final String value) {
        return setName(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withStatus(final Status value) {
        return setStatus(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withStatusDetails(final StatusDetails value) {
        return setStatusDetails(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withStage(final Stage value) {
        return setStage(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withDescription(final String value) {
        return setDescription(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withDescriptionHtml(final String value) {
        return setDescriptionHtml(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withStart(final Long value) {
        return setStart(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withStop(final Long value) {
        return setStop(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withSteps(final StepResult... values) {
        return setSteps(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withSteps(final Collection<StepResult> values) {
        return setSteps(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withSteps(final List<StepResult> steps) {
        return setSteps(steps);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withAttachments(final Attachment... values) {
        return setAttachments(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withAttachments(final Collection<Attachment> values) {
        return setAttachments(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withAttachments(final List<Attachment> attachments) {
        return setAttachments(attachments);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withParameters(final Parameter... values) {
        return setParameters(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withParameters(final Collection<Parameter> values) {
        return setParameters(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public TestResult withParameters(final List<Parameter> parameters) {
        return setParameters(parameters);
    }
}
