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
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO that stores fixture result information.
 */
@SuppressWarnings("deprecation")
public class FixtureResult extends ExecutableItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public FixtureResult setName(final String value) {
        super.setName(value);
        return this;
    }

    @Override
    public FixtureResult setStatus(final Status value) {
        super.setStatus(value);
        return this;
    }

    @Override
    public FixtureResult setStatusDetails(final StatusDetails value) {
        super.setStatusDetails(value);
        return this;
    }

    @Override
    public FixtureResult setStage(final Stage value) {
        super.setStage(value);
        return this;
    }

    @Override
    public FixtureResult setDescription(final String value) {
        super.setDescription(value);
        return this;
    }

    @Override
    public FixtureResult setDescriptionHtml(final String value) {
        super.setDescriptionHtml(value);
        return this;
    }

    @Override
    public FixtureResult setStart(final Long value) {
        super.setStart(value);
        return this;
    }

    @Override
    public FixtureResult setStop(final Long value) {
        super.setStop(value);
        return this;
    }

    @Override
    @JsonProperty
    public FixtureResult setSteps(final List<StepResult> steps) {
        super.setSteps(steps);
        return this;
    }

    @Override
    @JsonIgnore
    public FixtureResult setSteps(final StepResult... values) {
        super.setSteps(values);
        return this;
    }

    @Override
    @JsonIgnore
    public FixtureResult setSteps(final Collection<StepResult> values) {
        super.setSteps(values);
        return this;
    }

    @Override
    @JsonProperty
    public FixtureResult setAttachments(final List<Attachment> attachments) {
        super.setAttachments(attachments);
        return this;
    }

    @Override
    @JsonIgnore
    public FixtureResult setAttachments(final Attachment... values) {
        super.setAttachments(values);
        return this;
    }

    @Override
    @JsonIgnore
    public FixtureResult setAttachments(final Collection<Attachment> values) {
        super.setAttachments(values);
        return this;
    }

    @Override
    @JsonProperty
    public FixtureResult setParameters(final List<Parameter> parameters) {
        super.setParameters(parameters);
        return this;
    }

    @Override
    @JsonIgnore
    public FixtureResult setParameters(final Parameter... values) {
        super.setParameters(values);
        return this;
    }

    @Override
    @JsonIgnore
    public FixtureResult setParameters(final Collection<Parameter> values) {
        super.setParameters(values);
        return this;
    }

    @Override
    public FixtureResult withName(final String value) {
        return setName(value);
    }

    @Override
    public FixtureResult withStatus(final Status value) {
        return setStatus(value);
    }

    @Override
    public FixtureResult withStatusDetails(final StatusDetails value) {
        return setStatusDetails(value);
    }

    @Override
    public FixtureResult withStage(final Stage value) {
        return setStage(value);
    }

    @Override
    public FixtureResult withDescription(final String value) {
        return setDescription(value);
    }

    @Override
    public FixtureResult withDescriptionHtml(final String value) {
        return setDescriptionHtml(value);
    }

    @Override
    public FixtureResult withStart(final Long value) {
        return setStart(value);
    }

    @Override
    public FixtureResult withStop(final Long value) {
        return setStop(value);
    }

    @Override
    public FixtureResult withSteps(final StepResult... values) {
        return setSteps(values);
    }

    @Override
    public FixtureResult withSteps(final Collection<StepResult> values) {
        return setSteps(values);
    }

    @Override
    public FixtureResult withSteps(final List<StepResult> steps) {
        return setSteps(steps);
    }

    @Override
    public FixtureResult withAttachments(final Attachment... values) {
        return setAttachments(values);
    }

    @Override
    public FixtureResult withAttachments(final Collection<Attachment> values) {
        return setAttachments(values);
    }

    @Override
    public FixtureResult withAttachments(final List<Attachment> attachments) {
        return setAttachments(attachments);
    }

    @Override
    public FixtureResult withParameters(final Parameter... values) {
        return setParameters(values);
    }

    @Override
    public FixtureResult withParameters(final Collection<Parameter> values) {
        return setParameters(values);
    }

    @Override
    public FixtureResult withParameters(final List<Parameter> parameters) {
        return setParameters(parameters);
    }

}
