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
package io.qameta.allure.kotlin.extensions

import io.qameta.allure.model.Parameter

/**
 * Receiver for an executable Allure step.
 *
 * A scope targets the step that created it, so [name] and [parameter] keep updating that step even when nested work
 * is recorded. The Kotlin API represents the Java parameter overloads as one function with optional named arguments.
 */
public interface StepScope {
    /** Renames this step to [name]. */
    public fun name(name: String): Unit

    /**
     * Adds a parameter to this step and returns [value].
     *
     * [excluded] controls history-key calculation and [mode] controls how the value is displayed.
     */
    public fun <T> parameter(
        name: String,
        value: T,
        excluded: Boolean? = null,
        mode: Parameter.Mode? = null,
    ): T
}
