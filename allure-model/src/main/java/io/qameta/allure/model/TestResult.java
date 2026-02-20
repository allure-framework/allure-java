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
 * The model object that stores information about test that was run.
 * Test results are the main entity of Allure.
 *
 * @author baev (Dmitry Baev)
 * @see io.qameta.allure.model.ExecutableItem
 * @see io.qameta.allure.model.WithAttachments
 * @see io.qameta.allure.model.WithLinks
 * @see io.qameta.allure.model.WithParameters
 * @see io.qameta.allure.model.WithStatus
 * @see io.qameta.allure.model.WithStatusDetails
 * @see io.qameta.allure.model.WithSteps
 * @since 2.0
 */
public class TestResult implements Serializable, ExecutableItem, WithLinks {

    private static final long serialVersionUID = 1L;

    private String uuid;
    private String historyId;
    private String testCaseId;
    private String testCaseName;
    private String fullName;
    private List<Label> labels = new ArrayList<>();
    private List<Link> links = new ArrayList<>();
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
     * @return self for method chaining.
     */
    public TestResult setUuid(final String value) {
        this.uuid = value;
        return this;
    }

    /**
     * Gets history id.
     *
     * @return the history id
     */
    public String getHistoryId() {
        return historyId;
    }

    /**
     * Sets history id.
     *
     * @param value the value
     * @return self for method chaining.
     */
    public TestResult setHistoryId(final String value) {
        this.historyId = value;
        return this;
    }

    /**
     * Gets test case id.
     *
     * @return the test case id
     */
    public String getTestCaseId() {
        return testCaseId;
    }

    /**
     * Sets test case id.
     *
     * @param value the value
     * @return self for method chaining.
     */
    public TestResult setTestCaseId(final String value) {
        this.testCaseId = value;
        return this;
    }

    /**
     * Gets test case name.
     *
     * @return the test case name
     */
    public String getTestCaseName() {
        return testCaseName;
    }

    /**
     * Sets test case name.
     *
     * @param value the value
     * @return self for method chaining.
     */
    public TestResult setTestCaseName(final String value) {
        this.testCaseName = value;
        return this;
    }

    /**
     * Gets full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets full name.
     *
     * @param value the value
     * @return self for method chaining.
     */
    public TestResult setFullName(final String value) {
        this.fullName = value;
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
     * @return self for method chaining.
     */
    public TestResult setLabels(final List<Label> labels) {
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
     * @return self for method chaining.
     */
    public TestResult setLinks(final List<Link> links) {
        this.links = links;
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
     * @return self for method chaining.
     */
    public TestResult setName(final String value) {
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
     * @return self for method chaining.
     */
    public TestResult setStatus(final Status value) {
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
     * @return self for method chaining.
     */
    public TestResult setStatusDetails(final StatusDetails value) {
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
     * @return self for method chaining.
     */
    public TestResult setStage(final Stage value) {
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
     * @return self for method chaining.
     */
    public TestResult setDescription(final String value) {
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
     * @return self for method chaining.
     */
    public TestResult setDescriptionHtml(final String value) {
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
     * @return self for method chaining.
     */
    public TestResult setSteps(final List<StepResult> steps) {
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
     * @return self for method chaining.
     */
    public TestResult setAttachments(final List<Attachment> attachments) {
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
     * @return self for method chaining.
     */
    public TestResult setParameters(final List<Parameter> parameters) {
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
     * @return self for method chaining.
     */
    public TestResult setStart(final Long value) {
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
     * @return self for method chaining.
     */
    public TestResult setStop(final Long value) {
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
        final TestResult that = (TestResult) o;
        return Objects.equals(uuid, that.uuid)
                && Objects.equals(historyId, that.historyId)
                && Objects.equals(testCaseId, that.testCaseId)
                && Objects.equals(fullName, that.fullName)
                && Objects.equals(name, that.name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(uuid, historyId, testCaseId, fullName, name);
    }
}
