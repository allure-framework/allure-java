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

import io.qameta.allure.model.TestResultContainer;

/**
 * Notifies about Allure test container lifecycle.
 *
 * @since 2.0
 */
public interface ContainerLifecycleListener extends LifecycleListener {

    default void beforeContainerStart(final TestResultContainer container) {
        //do nothing
    }

    default void afterContainerStart(final TestResultContainer container) {
        //do nothing
    }

    default void beforeContainerUpdate(final TestResultContainer container) {
        //do nothing
    }

    default void afterContainerUpdate(final TestResultContainer container) {
        //do nothing
    }

    default void beforeContainerStop(final TestResultContainer container) {
        //do nothing
    }

    default void afterContainerStop(final TestResultContainer container) {
        //do nothing
    }

    default void beforeContainerWrite(final TestResultContainer container) {
        //do nothing
    }

    default void afterContainerWrite(final TestResultContainer container) {
        //do nothing
    }

}
