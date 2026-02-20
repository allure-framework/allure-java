/*
 *  Copyright 2016-2026 Qameta Software Inc
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

import io.qameta.allure.model.TestResult;

/**
 * Listener that notifies about Allure Lifecycle events.
 *
 * @since 2.0
 */
public interface TestLifecycleListener extends LifecycleListener {

    default void beforeTestSchedule(final TestResult result) {
        //do nothing
    }

    default void afterTestSchedule(final TestResult result) {
        //do nothing
    }

    default void beforeTestUpdate(final TestResult result) {
        //do nothing
    }

    default void afterTestUpdate(final TestResult result) {
        //do nothing
    }

    default void beforeTestStart(final TestResult result) {
        //do nothing
    }

    default void afterTestStart(final TestResult result) {
        //do nothing
    }

    default void beforeTestStop(final TestResult result) {
        //do nothing
    }

    default void afterTestStop(final TestResult result) {
        //do nothing
    }

    default void beforeTestWrite(final TestResult result) {
        //do nothing
    }

    default void afterTestWrite(final TestResult result) {
        //do nothing
    }

}
