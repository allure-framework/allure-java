package io.qameta.allure.listener;

import io.qameta.allure.model.FixtureResult;

/**
 * Notifies about Allure test fixtures lifecycle events.
 *
 * @since 2.0
 */
public interface FixtureLifecycleListener extends LifecycleListener {

    default void beforeFixtureStart(FixtureResult result) {
        //do nothing
    }

    default void afterFixtureStart(FixtureResult result) {
        //do nothing
    }

    default void beforeFixtureUpdate(FixtureResult result) {
        //do nothing
    }

    default void afterFixtureUpdate(FixtureResult result) {
        //do nothing
    }

    default void beforeFixtureStop(FixtureResult result) {
        //do nothing
    }

    default void afterFixtureStop(FixtureResult result) {
        //do nothing
    }

}
