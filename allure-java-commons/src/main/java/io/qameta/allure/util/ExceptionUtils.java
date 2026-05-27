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
package io.qameta.allure.util;

/**
 * Utility methods for translating exceptions into Allure status details.
 *
 * <p>Use these helpers when an integration needs the same message, trace, and known/unknown failure classification that the built-in lifecycle code applies.</p>
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
        throw new IllegalStateException("Do not instance");
    }

    /**
     * Returns the sneaky throw.
     *
     * @param throwable the throwable
     * @return the sneaky throw
     * @throws T if the underlying framework operation fails
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T sneakyThrow(final Throwable throwable) throws T {
        throw (T) throwable;
    }
}
