package io.qameta.allure.listener;

import io.qameta.allure.model.StepResult;

/**
 * Notifies about Allure step lifecycle events.
 *
 * @since 2.0
 */
public interface StepLifecycleListener {

    default void beforeStepStart(StepResult result) {
    }

    default void afterStepStart(StepResult result) {
    }

    default void beforeStepUpdate(StepResult result) {
    }

    default void afterStepUpdate(StepResult result) {
    }

    default void beforeStepStop(StepResult result) {
    }

    default void afterStepStop(StepResult result) {
    }

}
