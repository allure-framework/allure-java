package io.qameta.allure.listener;

import io.qameta.allure.model.TestResultContainer;

/**
 * Notifies about Allure test container lifecycle.
 *
 * @since 2.0
 */
public interface ContainerLifecycleListener {

    default void beforeContainerStart(TestResultContainer container) {
    }

    default void afterContainerStart(TestResultContainer container) {
    }

    default void beforeContainerUpdate(TestResultContainer container) {
    }

    default void afterContainerUpdate(TestResultContainer container) {
    }

    default void beforeContainerStop(TestResultContainer container) {
    }

    default void afterContainerStop(TestResultContainer container) {
    }

    default void beforeContainerWrite(TestResultContainer container) {
    }

    default void afterContainerWrite(TestResultContainer container) {
    }

}
