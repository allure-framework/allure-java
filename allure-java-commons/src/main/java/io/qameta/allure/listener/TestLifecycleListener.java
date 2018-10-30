package io.qameta.allure.listener;

import io.qameta.allure.model.TestResult;

/**
 * Listener that notifies about Allure Lifecycle events.
 *
 * @since 2.0
 */
public interface TestLifecycleListener extends LifecycleListener {

    default void beforeTestSchedule(TestResult result) {
        //do nothing
    }

    default void afterTestSchedule(TestResult result) {
        //do nothing
    }

    default void beforeTestUpdate(TestResult result) {
        //do nothing
    }

    default void afterTestUpdate(TestResult result) {
        //do nothing
    }

    default void beforeTestStart(TestResult result) {
        //do nothing
    }

    default void afterTestStart(TestResult result) {
        //do nothing
    }

    default void beforeTestStop(TestResult result) {
        //do nothing
    }

    default void afterTestStop(TestResult result) {
        //do nothing
    }

    default void beforeTestWrite(TestResult result) {
        //do nothing
    }

    default void afterTestWrite(TestResult result) {
        //do nothing
    }

}
