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
import java.util.Collection;
import java.util.List;

/**
 * POJO that stores common information about executable items.
 *
 * @deprecated scheduled to removal in 3.0 release.
 */
@Deprecated
@SuppressWarnings("PMD.ExcessivePublicCount")
public abstract class ExecutableItem implements Serializable, WithAttachments,
        WithParameters, WithStatusDetails, WithSteps {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected Status status;

    protected StatusDetails statusDetails;

    protected Stage stage;

    protected String description;

    protected String descriptionHtml;

    protected List<StepResult> steps;

    protected List<Attachment> attachments;

    protected List<Parameter> parameters;

    protected Long start;

    protected Long stop;

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
    public ExecutableItem setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets the value of the status property.
     *
     * @return possible object is
     * {@link Status }
     */
    @Override
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     *
     * @param value allowed object is
     *              {@link Status }
     */
    public ExecutableItem setStatus(final Status value) {
        this.status = value;
        return this;
    }

    /**
     * Gets the value of the statusDetails property.
     *
     * @return possible object is
     * {@link StatusDetails }
     */
    @Override
    public StatusDetails getStatusDetails() {
        return statusDetails;
    }

    /**
     * Sets the value of the statusDetails property.
     *
     * @param value allowed object is
     *              {@link StatusDetails }
     */
    public ExecutableItem setStatusDetails(final StatusDetails value) {
        this.statusDetails = value;
        return this;
    }

    /**
     * Gets the value of the stage property.
     *
     * @return possible object is
     * {@link Stage }
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Sets the value of the stage property.
     *
     * @param value allowed object is
     *              {@link Stage }
     */
    public ExecutableItem setStage(final Stage value) {
        this.stage = value;
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
    public ExecutableItem setDescription(final String value) {
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
    public ExecutableItem setDescriptionHtml(final String value) {
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
    public ExecutableItem setStart(final Long value) {
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
    public ExecutableItem setStop(final Long value) {
        this.stop = value;
        return this;
    }

    @Override
    public List<StepResult> getSteps() {
        if (steps == null) {
            steps = new ArrayList<>();
        }
        return steps;
    }

    public ExecutableItem setSteps(final List<StepResult> steps) {
        this.steps = steps;
        return this;
    }

    public ExecutableItem setSteps(final StepResult... values) {
        if (values != null) {
            for (StepResult value : values) {
                getSteps().add(value);
            }
        }
        return this;
    }

    public ExecutableItem setSteps(final Collection<StepResult> values) {
        if (values != null) {
            getSteps().addAll(values);
        }
        return this;
    }

    @Override
    public List<Attachment> getAttachments() {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        return attachments;
    }

    public ExecutableItem setAttachments(final List<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    public ExecutableItem setAttachments(final Attachment... values) {
        if (values != null) {
            for (Attachment value : values) {
                getAttachments().add(value);
            }
        }
        return this;
    }

    public ExecutableItem setAttachments(final Collection<Attachment> values) {
        if (values != null) {
            getAttachments().addAll(values);
        }
        return this;
    }

    @Override
    public List<Parameter> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        return parameters;
    }

    public ExecutableItem setParameters(final List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    public ExecutableItem setParameters(final Parameter... values) {
        if (values != null) {
            for (Parameter value : values) {
                getParameters().add(value);
            }
        }
        return this;
    }

    public ExecutableItem setParameters(final Collection<Parameter> values) {
        if (values != null) {
            getParameters().addAll(values);
        }
        return this;
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withName(final String value) {
        return setName(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withStatus(final Status value) {
        return setStatus(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withStatusDetails(final StatusDetails value) {
        return setStatusDetails(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withStage(final Stage value) {
        return setStage(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withDescription(final String value) {
        return setDescription(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withDescriptionHtml(final String value) {
        return setDescriptionHtml(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withStart(final Long value) {
        return setStart(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withStop(final Long value) {
        return setStop(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withSteps(final StepResult... values) {
        return setSteps(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withSteps(final Collection<StepResult> values) {
        return setSteps(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withSteps(final List<StepResult> steps) {
        return setSteps(steps);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withAttachments(final Attachment... values) {
        return setAttachments(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withAttachments(final Collection<Attachment> values) {
        return setAttachments(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withAttachments(final List<Attachment> attachments) {
        return setAttachments(attachments);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withParameters(final Parameter... values) {
        return setParameters(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withParameters(final Collection<Parameter> values) {
        return setParameters(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    public ExecutableItem withParameters(final List<Parameter> parameters) {
        return setParameters(parameters);
    }
}
