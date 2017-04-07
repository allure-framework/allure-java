package io.qameta.allure.listener;

import io.qameta.allure.model.StepResult;

/**
 * Notifies about Allure step lifecycle events.
 *
 * @since 2.0
 */
public interface StepLifecycleListener {

    default void beforeStepStart(StepResult result) {
        //do nothing
    }

    default void afterStepStart(StepResult result) {
        //do nothing
    }

    default void beforeStepUpdate(StepResult result) {
        //do nothing
    }

    default void afterStepUpdate(StepResult result) {
        //do nothing
    }

    default void beforeStepStop(StepResult result) {
        //do nothing
    }

    default void afterStepStop(StepResult result) {
        //do nothing
    }

}
