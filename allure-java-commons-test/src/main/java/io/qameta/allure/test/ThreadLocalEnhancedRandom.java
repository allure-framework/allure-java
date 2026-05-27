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
package io.qameta.allure.test;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;

/**
 * Supports Allure Java test support integration with Allure reporting.
 *
 * <p>Use this type through the module that owns it when translating framework execution, result metadata, or attachments into Allure report data.</p>
 */
public final class ThreadLocalEnhancedRandom {

    private static final ThreadLocal<EnhancedRandom> INSTANCE = ThreadLocal
            .withInitial(EnhancedRandomBuilder::aNewEnhancedRandom);

    private ThreadLocalEnhancedRandom() {
        throw new IllegalStateException("do not instance");
    }

    /**
     * Returns the current.
     *
     * @return the current
     */
    public static EnhancedRandom current() {
        return INSTANCE.get();
    }

}
