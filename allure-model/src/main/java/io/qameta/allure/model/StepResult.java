package io.qameta.allure.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * POJO that stores step information.
 */
@SuppressWarnings("deprecation")
public class StepResult extends ExecutableItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public StepResult setName(final String value) {
        super.setName(value);
        return this;
    }

    @Override
    public StepResult setStatus(final Status value) {
        super.setStatus(value);
        return this;
    }

    @Override
    public StepResult setStatusDetails(final StatusDetails value) {
        super.setStatusDetails(value);
        return this;
    }

    @Override
    public StepResult setStage(final Stage value) {
        super.setStage(value);
        return this;
    }

    @Override
    public StepResult setDescription(final String value) {
        super.setDescription(value);
        return this;
    }

    @Override
    public StepResult setDescriptionHtml(final String value) {
        super.setDescriptionHtml(value);
        return this;
    }

    @Override
    public StepResult setStart(final Long value) {
        super.setStart(value);
        return this;
    }

    @Override
    public StepResult setStop(final Long value) {
        super.setStop(value);
        return this;
    }

    @Override
    public StepResult setSteps(final List<StepResult> steps) {
        super.setSteps(steps);
        return this;
    }

    @Override
    public StepResult setSteps(final StepResult... values) {
        super.setSteps(values);
        return this;
    }

    @Override
    public StepResult setSteps(final Collection<StepResult> values) {
        super.setSteps(values);
        return this;
    }

    @Override
    public StepResult setAttachments(final List<Attachment> attachments) {
        super.setAttachments(attachments);
        return this;
    }

    @Override
    public StepResult setAttachments(final Attachment... values) {
        super.setAttachments(values);
        return this;
    }

    @Override
    public StepResult setAttachments(final Collection<Attachment> values) {
        super.setAttachments(values);
        return this;
    }

    @Override
    public StepResult setParameters(final List<Parameter> parameters) {
        super.setParameters(parameters);
        return this;
    }

    @Override
    public StepResult setParameters(final Parameter... values) {
        super.setParameters(values);
        return this;
    }

    @Override
    public StepResult setParameters(final Collection<Parameter> values) {
        super.setParameters(values);
        return this;
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withName(final String value) {
        return setName(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withStatus(final Status value) {
        return setStatus(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withStatusDetails(final StatusDetails value) {
        return setStatusDetails(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withStage(final Stage value) {
        return setStage(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withDescription(final String value) {
        return setDescription(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withDescriptionHtml(final String value) {
        return setDescriptionHtml(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withStart(final Long value) {
        return setStart(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withStop(final Long value) {
        return setStop(value);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withSteps(final StepResult... values) {
        return setSteps(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withSteps(final Collection<StepResult> values) {
        return setSteps(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withSteps(final List<StepResult> steps) {
        return setSteps(steps);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withAttachments(final Attachment... values) {
        return setAttachments(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withAttachments(final Collection<Attachment> values) {
        return setAttachments(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withAttachments(final List<Attachment> attachments) {
        return setAttachments(attachments);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withParameters(final Parameter... values) {
        return setParameters(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withParameters(final Collection<Parameter> values) {
        return setParameters(values);
    }

    /**
     * @deprecated use set method. Scheduled to removal in 3.0 release.
     */
    @Deprecated
    @Override
    public StepResult withParameters(final List<Parameter> parameters) {
        return setParameters(parameters);
    }
}
