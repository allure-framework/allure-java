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
package io.qameta.allure.awaitility;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureThreadBinding;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.IgnoredException;
import org.awaitility.core.StartEvaluationEvent;
import org.awaitility.core.TimeoutEvent;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * <p>
 * Class implements Awaitility condition listener to log all awaiting and polls as {@link io.qameta.allure.Step}.
 * </p>
 * <p>
 * Usage with single condition
 * <pre>
 * <code>
 * Awaitility.await()
 *     .conditionEvaluationListener(new AllureAwaitilityListener())
 *     .until(() -&gt; somethingHappen());
 * </code>
 * </pre>
 * </p>
 * <p>
 * Usage globally for all conditions
 * <pre>
 * <code>
 * Awaitility.setDefaultConditionEvaluationListener(new AllureAwaitilityListener());
 * </code>
 * </pre>
 * </p>
 * <p>
 * A listener instance tracks a single condition at a time, so prefer one instance per condition when tests run in
 * parallel. Awaitility fires no callback when a non-ignored exception escapes a condition; such an aborted wait
 * stays the reporting parent until the same listener starts its next condition, which finalizes the aborted one,
 * or until the test ends.
 * </p>
 *
 * @see org.awaitility.core.ConditionEvaluationListener
 * @see Awaitility#setDefaultConditionEvaluationListener(ConditionEvaluationListener)
 * @see ConditionFactory#conditionEvaluationListener(ConditionEvaluationListener)
 * @see <a href="https://github.com/awaitility/awaitility/wiki/Usage#condition-evaluation-listener"> awaitlity wiki</a>
 */
@SuppressWarnings("unused")
public class AllureAwaitilityListener implements ConditionEvaluationListener<Object> {

    private final AtomicReference<TimeUnit> unit = new AtomicReference<>(MILLISECONDS);
    private final AtomicBoolean logIgnoredExceptions = new AtomicBoolean(true);
    private final String onStartStepTextPattern;
    private final String onSatisfiedStepTextPattern;
    private final String onAwaitStepTextPattern;
    private final String onTimeoutStepTextPattern;
    private final String onExceptionStepTextPattern;

    private final AtomicReference<ConditionState> currentCondition = new AtomicReference<>();

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public static AllureLifecycle getLifecycle() {
        return Allure.getLifecycle();
    }

    /**
     * Default all args constructor with default params.
     */
    public AllureAwaitilityListener() {
        this.onStartStepTextPattern = "Awaitility: %s";
        this.onSatisfiedStepTextPattern = "%s after %d %s (remaining time %d %s, last poll interval was %s)";
        this.onAwaitStepTextPattern = "%s (elapsed time %d %s, remaining time %d %s (last poll interval was %s))";
        this.onTimeoutStepTextPattern = "Condition timeout. %s";
        this.onExceptionStepTextPattern = "Exception ignored. %s";
    }

    /**
     * Set default timeunit used in awaitility in project for properly printing.
     *
     * @param unit default timeunit in project
     * @return this factory
     */
    public AllureAwaitilityListener setUnit(final TimeUnit unit) {
        this.unit.set(unit);
        return this;
    }

    /**
     * Set logging ignored exceptions. True by default.
     *
     * @param logging to log or not
     * @return this factory
     */
    public AllureAwaitilityListener setLogIgnoredExceptions(final boolean logging) {
        this.logIgnoredExceptions.set(logging);
        return this;
    }

    /**
     * Method creates top-level step with short description about condition.
     *
     * @param startEvaluationEvent condition evaluation started
     */
    @Override
    public void beforeEvaluation(final StartEvaluationEvent<Object> startEvaluationEvent) {
        finishCurrentCondition();
        final AllureLifecycle lifecycle = getLifecycle();
        lifecycle.getCurrentExecutableKey().ifPresent(parent -> {
            final String nameWoAlias = String.format(onStartStepTextPattern, startEvaluationEvent.getDescription());
            final String nameWithAlias = String.format(onStartStepTextPattern, startEvaluationEvent.getAlias());
            final String stepName = startEvaluationEvent.getAlias() != null ? nameWithAlias : nameWoAlias;
            final AllureExternalKey conditionStepKey = AllureExternalKey.random(AllureAwaitilityListener.class);
            lifecycle.startStep(
                    parent,
                    conditionStepKey,
                    new StepResult()
                            .setName(stepName)
                            .setDescription("Awaitility condition started")
                            .setStatus(Status.FAILED)
            );
            final ConditionState condition = new ConditionState(lifecycle, conditionStepKey);
            currentCondition.set(condition);
            // Keep condition-body steps under the wait. The binding remembers the caller thread's stack, so
            // Awaitility may close it from a polling callback without mutating a reused worker's unrelated context.
            condition.setBinding(lifecycle.bindDetached(conditionStepKey));
        });
    }

    /**
     * Logging timeout evaluation result. Method should create second-level step with useful info about timeout.
     *
     * @param timeoutEvent poling ended with timeout
     */
    @Override
    public void onTimeout(final TimeoutEvent timeoutEvent) {
        final ConditionState condition = currentCondition.get();
        if (condition == null) {
            return;
        }
        condition.getLifecycle().logStep(
                condition.getStepKey(),
                new StepResult()
                        .setName(String.format(onTimeoutStepTextPattern, timeoutEvent.getDescription()))
                        .setDescription("Awaitility condition timeout")
                        .setStatus(Status.BROKEN)
        );
        finishCondition(condition);
    }

    /**
     * Logging any evaluation result. Method should create second-level step with evaluation result and useful info.
     *
     * @param condition evaluation result for poling iteration
     */
    @Override
    public void conditionEvaluated(final EvaluatedCondition<Object> condition) {
        final TimeUnit currentUnit = unit.get();
        final String description = condition.getDescription();
        final long elapsedTime = currentUnit.convert(condition.getElapsedTimeInMS(), MILLISECONDS);
        final long remainingTime = currentUnit.convert(condition.getRemainingTimeInMS(), MILLISECONDS);
        final String unitAsString = currentUnit.toString().toLowerCase();

        final String message = String.format(
                condition.isSatisfied() ? onSatisfiedStepTextPattern : onAwaitStepTextPattern,
                description,
                elapsedTime,
                unitAsString,
                remainingTime,
                unitAsString,
                new TemporalDuration(condition.getPollInterval())
        );

        final ConditionState current = currentCondition.get();
        if (current == null) {
            return;
        }
        current.getLifecycle().logStep(
                current.getStepKey(),
                new StepResult()
                        .setName(message)
                        .setDescription("Awaitility condition satisfied or not, but awaiting still in progress")
                        .setStatus(Status.PASSED)
        );
        if (condition.isSatisfied()) {
            current.getLifecycle().updateStep(
                    current.getStepKey(), awaitilityCondition -> awaitilityCondition.setStatus(Status.PASSED)
            );
            finishCondition(current);
        }
    }

    private void finishCurrentCondition() {
        final ConditionState condition = currentCondition.getAndSet(null);
        if (condition != null) {
            condition.finish();
        }
    }

    private void finishCondition(final ConditionState condition) {
        currentCondition.compareAndSet(condition, null);
        condition.finish();
    }

    private static final class ConditionState {

        private final AllureLifecycle lifecycle;
        private final AllureExternalKey stepKey;
        private final AtomicReference<AllureThreadBinding> binding = new AtomicReference<>();
        private final AtomicBoolean finished = new AtomicBoolean();

        private ConditionState(final AllureLifecycle lifecycle, final AllureExternalKey stepKey) {
            this.lifecycle = lifecycle;
            this.stepKey = stepKey;
        }

        private AllureLifecycle getLifecycle() {
            return lifecycle;
        }

        private AllureExternalKey getStepKey() {
            return stepKey;
        }

        private void setBinding(final AllureThreadBinding value) {
            binding.set(value);
            if (finished.get()) {
                closeBinding();
            }
        }

        private void finish() {
            if (finished.compareAndSet(false, true)) {
                try {
                    closeBinding();
                } finally {
                    lifecycle.stopStep(stepKey);
                }
            }
        }

        private void closeBinding() {
            final AllureThreadBinding current = binding.getAndSet(null);
            if (current != null) {
                current.close();
            }
        }
    }

    /**
     * Logging ignored exceptions while poling conditions. Active only with
     * <pre><code>await().with().ignoreExceptions()</code></pre>
     * or
     * <pre><code>Awaitility.ignoreExceptionsByDefault()</code></pre>
     *
     * @param ignoredException ignored exception
     * @see Awaitility#ignoreExceptionsByDefault()
     * @see Awaitility#ignoreExceptionByDefault(Class)
     * @see ConditionFactory#ignoreExceptions()
     * @see ConditionFactory#ignoreException(Class)
     * @see <a href="https://github.com/awaitility/awaitility/wiki/Usage#ignoring-exceptions">awaitlity wiki</a>
     */
    @Override
    public void exceptionIgnored(final IgnoredException ignoredException) {
        final ConditionState condition = currentCondition.get();
        if (logIgnoredExceptions.get() && condition != null) {
            final AllureExternalKey exceptionIgnoredStepKey = AllureExternalKey.random(AllureAwaitilityListener.class);
            final String message = String.format(
                    onExceptionStepTextPattern, ignoredException.getThrowable().getMessage()
            );
            final StringWriter stringWriter = new StringWriter();
            ignoredException.getThrowable().printStackTrace(new PrintWriter(stringWriter));
            final String stackTrace = stringWriter.toString();
            condition.getLifecycle().startStep(
                    condition.getStepKey(),
                    exceptionIgnoredStepKey,
                    new StepResult()
                            .setName(message)
                            .setDescription("Exception occurred and ignored, but awaiting still in progress")
                            .setStatus(Status.SKIPPED)
            );
            condition.getLifecycle().addAttachment(
                    exceptionIgnoredStepKey,
                    ignoredException.getThrowable().getMessage(),
                    "text/plain",
                    new ByteArrayInputStream(stackTrace.getBytes(StandardCharsets.UTF_8)),
                    AttachmentOptions.empty()
            );
            condition.getLifecycle().stopStep(exceptionIgnoredStepKey);
        }
    }

}
