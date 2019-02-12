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

import io.qameta.allure.model.StepResult;

/**
 * Notifies about Allure step lifecycle events.
 *
 * @since 2.0
 */
public interface StepLifecycleListener extends LifecycleListener {

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
