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
package io.qameta.allure.awaitility;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.IgnoredException;
import org.awaitility.core.StartEvaluationEvent;
import org.awaitility.core.TimeoutEvent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
 * @author a-simeshin (Simeshin Artem)
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

    private String currentConditionStepUUID;

    private static final InheritableThreadLocal<AllureLifecycle> LIFECYCLE = new InheritableThreadLocal<AllureLifecycle>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    public static AllureLifecycle getLifecycle() {
        return LIFECYCLE.get();
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
        currentConditionStepUUID = UUID.randomUUID().toString();
        final String nameWoAlias = String.format(onStartStepTextPattern, startEvaluationEvent.getDescription());
        final String nameWithAlias = String.format(onStartStepTextPattern, startEvaluationEvent.getAlias());
        final String stepName = startEvaluationEvent.getAlias() != null ? nameWithAlias : nameWoAlias;
        getLifecycle().startStep(
                currentConditionStepUUID,
                new StepResult()
                        .setName(stepName)
                        .setDescription("Awaitility condition started")
                        .setStatus(Status.FAILED)
        );
    }

    /**
     * Logging timeout evaluation result. Method should create second-level step with useful info about timeout.
     *
     * @param timeoutEvent poling ended with timeout
     */
    @Override
    public void onTimeout(final TimeoutEvent timeoutEvent) {
        getLifecycle().updateStep(awaitilityCondition -> {
            final String currentTimeoutStepUUID = UUID.randomUUID().toString();
            getLifecycle().startStep(
                    currentConditionStepUUID,
                    currentTimeoutStepUUID,
                    new StepResult()
                            .setName(String.format(onTimeoutStepTextPattern, timeoutEvent.getDescription()))
                            .setDescription("Awaitility condition timeout")
                            .setStatus(Status.BROKEN)
            );
            getLifecycle().stopStep(currentTimeoutStepUUID);
        });
        getLifecycle().stopStep(currentConditionStepUUID);
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

        getLifecycle().updateStep(awaitilityCondition -> {
            final String lastAwaitStepUUID = UUID.randomUUID().toString();
            getLifecycle().startStep(
                    currentConditionStepUUID,
                    lastAwaitStepUUID,
                    new StepResult()
                            .setName(message)
                            .setDescription("Awaitility condition satisfied or not, but awaiting still in progress")
                            .setStatus(Status.PASSED)
            );
            getLifecycle().stopStep(lastAwaitStepUUID);
            if (condition.isSatisfied()) {
                awaitilityCondition.setStatus(Status.PASSED);
                getLifecycle().stopStep(currentConditionStepUUID);
            }
        });
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
        if (logIgnoredExceptions) {
            getLifecycle().updateStep(awaitilityCondition -> {
                final String currentExceptionIgnoredStepUUID = UUID.randomUUID().toString();
                final String message = String.format(
                        onExceptionStepTextPattern, ignoredException.getThrowable().getMessage());
                final StringWriter stringWriter = new StringWriter();
                ignoredException.getThrowable().printStackTrace(new PrintWriter(stringWriter));
                final String stackTrace = stringWriter.toString();
                getLifecycle().startStep(
                        currentConditionStepUUID,
                        currentExceptionIgnoredStepUUID,
                        new StepResult()
                                .setName(message)
                                .setDescription("Exception occurred and ignored, but awaiting still in progress")
                                .setStatus(Status.SKIPPED)
                );
                getLifecycle().addAttachment(
                        ignoredException.getThrowable().getMessage(), "text/plain", ".txt",
                        stackTrace.getBytes(StandardCharsets.UTF_8));
                getLifecycle().stopStep(currentExceptionIgnoredStepUUID);
            });
        }
    }

    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        LIFECYCLE.set(allure);
    }

}
