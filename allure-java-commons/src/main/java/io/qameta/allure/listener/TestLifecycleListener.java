/*
 *  Copyright 2019 Qameta Software OÜ
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
