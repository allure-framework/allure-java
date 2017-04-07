package io.qameta.allure.listener;

import io.qameta.allure.model.TestResult;

/**
 * Listener that notifies about Allure Lifecycle events.
 *
 * @since 2.0
 */
public interface TestLifecycleListener {

    default void beforeTestSchedule(TestResult result) {
    }

    default void afterTestSchedule(TestResult result) {
    }

    default void beforeTestUpdate(TestResult result) {
    }

    default void afterTestUpdate(TestResult result) {
    }

    default void beforeTestStart(TestResult result) {
    }

    default void afterTestStart(TestResult result) {
    }

    default void beforeTestStop(TestResult result) {
    }

    default void afterTestStop(TestResult result) {
    }

    default void beforeTestWrite(TestResult result) {
    }

    default void afterTestWrite(TestResult result) {
    }

}
