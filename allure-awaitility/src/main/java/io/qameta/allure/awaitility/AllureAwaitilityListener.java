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
 *
 * @see org.awaitility.core.ConditionEvaluationListener
 * @see Awaitility#setDefaultConditionEvaluationListener(ConditionEvaluationListener)
 * @see ConditionFactory#conditionEvaluationListener(ConditionEvaluationListener)
 * @see <a href="https://github.com/awaitility/awaitility/wiki/Usage#condition-evaluation-listener"> awaitlity wiki</a>
 */
@SuppressWarnings("unused")
public class AllureAwaitilityListener implements ConditionEvaluationListener<Object> {

    private TimeUnit unit;
    private boolean logIgnoredExceptions;
    private final String onStartStepTextPattern;
    private final String onSatisfiedStepTextPattern;
    private final String onAwaitStepTextPattern;
    private final String onTimeoutStepTextPattern;
    private final String onExceptionStepTextPattern;

    private AllureExternalKey currentConditionStepKey;

    private AllureThreadBinding currentConditionBinding;

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
        this.unit = MILLISECONDS;
        this.logIgnoredExceptions = true;
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
        this.unit = unit;
        return this;
    }

    /**
     * Set logging ignored exceptions. True by default.
     *
     * @param logging to log or not
     * @return this factory
     */
    public AllureAwaitilityListener setLogIgnoredExceptions(final boolean logging) {
        this.logIgnoredExceptions = logging;
        return this;
    }

    /**
     * Method creates top-level step with short description about condition.
     *
     * @param startEvaluationEvent condition evaluation started
     */
    @Override
    public void beforeEvaluation(final StartEvaluationEvent<Object> startEvaluationEvent) {
        currentConditionStepKey = null;
        getLifecycle().getCurrentExecutableKey().ifPresent(parent -> {
            final String nameWoAlias = String.format(onStartStepTextPattern, startEvaluationEvent.getDescription());
            final String nameWithAlias = String.format(onStartStepTextPattern, startEvaluationEvent.getAlias());
            final String stepName = startEvaluationEvent.getAlias() != null ? nameWithAlias : nameWoAlias;
            final AllureExternalKey conditionStepKey = AllureExternalKey.random(AllureAwaitilityListener.class);
            currentConditionStepKey = conditionStepKey;
            getLifecycle().startStep(
                    parent,
                    conditionStepKey,
                    new StepResult()
                            .setName(stepName)
                            .setDescription("Awaitility condition started")
                            .setStatus(Status.FAILED)
            );
            // bind the polling thread to the condition step, so steps produced while evaluating the
            // condition — for example assertion steps from untilAsserted polls — nest under it
            currentConditionBinding = getLifecycle().bindDetached(conditionStepKey);
        });
    }

    /**
     * Logging timeout evaluation result. Method should create second-level step with useful info about timeout.
     *
     * @param timeoutEvent poling ended with timeout
     */
    @Override
    public void onTimeout(final TimeoutEvent timeoutEvent) {
        if (currentConditionStepKey == null) {
            return;
        }
        closeConditionBinding();
        getLifecycle().logStep(
                currentConditionStepKey,
                new StepResult()
                        .setName(String.format(onTimeoutStepTextPattern, timeoutEvent.getDescription()))
                        .setDescription("Awaitility condition timeout")
                        .setStatus(Status.BROKEN)
        );
        getLifecycle().stopStep(currentConditionStepKey);
    }

    /**
     * Logging any evaluation result. Method should create second-level step with evaluation result and useful info.
     *
     * @param condition evaluation result for poling iteration
     */
    @Override
    public void conditionEvaluated(final EvaluatedCondition<Object> condition) {
        final String description = condition.getDescription();
        final long elapsedTime = unit.convert(condition.getElapsedTimeInMS(), MILLISECONDS);
        final long remainingTime = unit.convert(condition.getRemainingTimeInMS(), MILLISECONDS);
        final String unitAsString = unit.toString().toLowerCase();

        final String message = String.format(
                condition.isSatisfied() ? onSatisfiedStepTextPattern : onAwaitStepTextPattern,
                description,
                elapsedTime,
                unitAsString,
                remainingTime,
                unitAsString,
                new TemporalDuration(condition.getPollInterval())
        );

        if (currentConditionStepKey == null) {
            return;
        }
        getLifecycle().logStep(
                currentConditionStepKey,
                new StepResult()
                        .setName(message)
                        .setDescription("Awaitility condition satisfied or not, but awaiting still in progress")
                        .setStatus(Status.PASSED)
        );
        if (condition.isSatisfied()) {
            closeConditionBinding();
            getLifecycle().updateStep(
                    currentConditionStepKey, awaitilityCondition -> awaitilityCondition.setStatus(Status.PASSED)
            );
            getLifecycle().stopStep(currentConditionStepKey);
        }
    }

    private void closeConditionBinding() {
        if (currentConditionBinding != null) {
            currentConditionBinding.close();
            currentConditionBinding = null;
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
        if (logIgnoredExceptions && currentConditionStepKey != null) {
            final AllureExternalKey exceptionIgnoredStepKey = AllureExternalKey.random(AllureAwaitilityListener.class);
            final String message = String.format(
                    onExceptionStepTextPattern, ignoredException.getThrowable().getMessage()
            );
            final StringWriter stringWriter = new StringWriter();
            ignoredException.getThrowable().printStackTrace(new PrintWriter(stringWriter));
            final String stackTrace = stringWriter.toString();
            getLifecycle().startStep(
                    currentConditionStepKey,
                    exceptionIgnoredStepKey,
                    new StepResult()
                            .setName(message)
                            .setDescription("Exception occurred and ignored, but awaiting still in progress")
                            .setStatus(Status.SKIPPED)
            );
            getLifecycle().addAttachment(
                    exceptionIgnoredStepKey,
                    ignoredException.getThrowable().getMessage(),
                    "text/plain",
                    new ByteArrayInputStream(stackTrace.getBytes(StandardCharsets.UTF_8)),
                    AttachmentOptions.empty()
            );
            getLifecycle().stopStep(exceptionIgnoredStepKey);
        }
    }

}
