/*
 *  Copyright 2016-2024 Qameta Software Inc
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
 * The model object that stores information about executed test fixtures (set up and tear down methods).
 * In order to link test fixture to test result {@link TestResultContainer} is used.
 *
 * @author baev (Dmitry Baev)
 * @see io.qameta.allure.model.TestResult
 * @see io.qameta.allure.model.TestResultContainer
 * @since 2.0
 */
public class FixtureResult implements Serializable, ExecutableItem {

    private static final long serialVersionUID = 1L;

    private String name;
    private Status status;
    private StatusDetails statusDetails;
    private Stage stage;
    private String description;
    private String descriptionHtml;
    private List<StepResult> steps = new ArrayList<>();
    private List<Attachment> attachments = new ArrayList<>();
    private List<Parameter> parameters = new ArrayList<>();
    private Long start;
    private Long stop;

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
    public FixtureResult setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    @Override
    public Status getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param value the value
     * @return self for method chaining
     */
    public FixtureResult setStatus(final Status value) {
        this.status = value;
        return this;
    }

    /**
     * Gets status details.
     *
     * @return the status details
     */
    @Override
    public StatusDetails getStatusDetails() {
        return statusDetails;
    }

    /**
     * Sets status details.
     *
     * @param value the value
     * @return self for method chaining
     */
    public FixtureResult setStatusDetails(final StatusDetails value) {
        this.statusDetails = value;
        return this;
    }

    /**
     * Gets stage.
     *
     * @return the stage
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Sets stage.
     *
     * @param value the value
     * @return self for method chaining
     */
    public FixtureResult setStage(final Stage value) {
        this.stage = value;
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
    public FixtureResult setDescription(final String value) {
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
    public FixtureResult setDescriptionHtml(final String value) {
        this.descriptionHtml = value;
        return this;
    }

    /**
     * Gets steps.
     *
     * @return the steps
     */
    @Override
    public List<StepResult> getSteps() {
        return steps;
    }

    /**
     * Sets steps.
     *
     * @param steps the steps
     * @return self for method chaining
     */
    public FixtureResult setSteps(final List<StepResult> steps) {
        this.steps = steps;
        return this;
    }

    /**
     * Gets attachments.
     *
     * @return the attachments
     */
    @Override
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * Sets attachments.
     *
     * @param attachments the attachments
     * @return self for method chaining
     */
    public FixtureResult setAttachments(final List<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    /**
     * Gets parameters.
     *
     * @return the parameters
     */
    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * Sets parameters.
     *
     * @param parameters the parameters
     * @return self for method chaining
     */
    public FixtureResult setParameters(final List<Parameter> parameters) {
        this.parameters = parameters;
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
    public FixtureResult setStart(final Long value) {
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
    public FixtureResult setStop(final Long value) {
        this.stop = value;
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
        final FixtureResult that = (FixtureResult) o;
        return Objects.equals(name, that.name)
                && status == that.status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, status);
    }

    /**
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "FixtureResult(" +
                "name=" + this.name + ", " +
                "status=" + this.status + ", " +
                "statusDetails=" + this.statusDetails + ", " +
                "stage=" + this.stage + ", " +
                "description=" + this.description + ", " +
                "descriptionHtml=" + this.descriptionHtml + ", " +
                "steps=" + this.steps + ", " +
                "attachments=" + this.attachments + ", " +
                "parameters=" + this.parameters + ", " +
                "start=" + this.start + ", " +
                "stop=" + this.stop + ")";
    }
}
