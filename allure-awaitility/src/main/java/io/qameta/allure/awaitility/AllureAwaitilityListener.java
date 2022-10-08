/*
 *  Copyright 2022 Qameta Software OÜ
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
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.StartEvaluationEvent;
import org.awaitility.core.TimeoutEvent;

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
 *     public AllureLifecycle getLifecycle() {
 *         Awaitility.await()
 *                 .conditionEvaluationListener(new AllureAwaitilityListener())
 *                 .until(() -> somethingHappen());
 * </code>
 * </pre>
 * </p>
 * <p>
 * Usage globally for all conditions
 * <pre>
 * <code>
 *     Awaitility.setDefaultConditionEvaluationListener(new AllureAwaitilityListener());
 * </code>
 * </pre>
 * </p>
 *
 * @author a-simeshin (Simeshin Artem)
 * @see org.awaitility.core.ConditionEvaluationListener
 */
@SuppressWarnings("unused")
public class AllureAwaitilityListener implements ConditionEvaluationListener<Object> {

    private TimeUnit unit;
    private final String onStartStepTextPattern;
    private final String onSatisfiedStepTextPattern;
    private final String onAwaitStepTextPattern;
    private final String onTimeoutStepTextPattern;

    private String currentConditionStepUUID;

    private static InheritableThreadLocal<AllureLifecycle> lifecycle = new InheritableThreadLocal<AllureLifecycle>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    public static AllureLifecycle getLifecycle() {
        return lifecycle.get();
    }

    /**
     * Default all args constructor with default params.
     */
    public AllureAwaitilityListener() {
        this.unit = MILLISECONDS;
        this.onStartStepTextPattern = "Awaitility: %s";
        this.onSatisfiedStepTextPattern = "%s after %d %s (remaining time %d %s, last poll interval was %s)";
        this.onAwaitStepTextPattern = "%s (elapsed time %d %s, remaining time %d %s (last poll interval was %s))";
        this.onTimeoutStepTextPattern = "Condition timeout. %s";
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

    @Override
    public void conditionEvaluated(final EvaluatedCondition<Object> condition) {
        final String description = condition.getDescription();
        final long elapsedTime = unit.convert(condition.getElapsedTimeInMS(), MILLISECONDS);
        final long remainingTime = unit.convert(condition.getRemainingTimeInMS(), MILLISECONDS);
        final String unitAsString = unit.toString().toLowerCase();

        String message;
        if (condition.isSatisfied()) {
            message =
                    String.format(onSatisfiedStepTextPattern, description, elapsedTime, unitAsString, remainingTime,
                            unitAsString, new TemporalDuration(condition.getPollInterval()));
        } else {
            message =
                    String.format(onAwaitStepTextPattern, description, elapsedTime, unitAsString, remainingTime,
                            unitAsString, new TemporalDuration(condition.getPollInterval()));
        }

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
     * For tests only.
     *
     * @param allure allure lifecycle to set
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        lifecycle.set(allure);
    }

}
