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
package io.qameta.allure.listener;

import io.qameta.allure.model.FixtureResult;

/**
 * Notifies about Allure test fixtures lifecycle events.
 *
 * @since 2.0
 */
public interface FixtureLifecycleListener extends LifecycleListener {

    default void beforeFixtureStart(final FixtureResult result) {
        //do nothing
    }

    default void afterFixtureStart(final FixtureResult result) {
        //do nothing
    }

    default void beforeFixtureUpdate(final FixtureResult result) {
        //do nothing
    }

    default void afterFixtureUpdate(final FixtureResult result) {
        //do nothing
    }

    default void beforeFixtureStop(final FixtureResult result) {
        //do nothing
    }

    default void afterFixtureStop(final FixtureResult result) {
        //do nothing
    }

}
