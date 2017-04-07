package io.qameta.allure.listener;

import io.qameta.allure.model.FixtureResult;

/**
 * Notifies about Allure test fixtures lifecycle events.
 *
 * @since 2.0
 */
public interface FixtureLifecycleListener {

    default void beforeFixtureStart(FixtureResult result) {
    }

    default void afterFixtureStart(FixtureResult result) {
    }

    default void beforeFixtureUpdate(FixtureResult result) {
    }

    default void afterFixtureUpdate(FixtureResult result) {
    }

    default void beforeFixtureStop(FixtureResult result) {
    }

    default void afterFixtureStop(FixtureResult result) {
    }

}
