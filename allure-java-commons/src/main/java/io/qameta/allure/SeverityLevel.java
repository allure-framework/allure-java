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
package io.qameta.allure;

/**
 * Enumerates standard Allure severity levels.
 *
 * <p>Use these values with {@link Severity} to mark the business impact of a test case. The enum value is serialized as the lowercase Allure label value expected by report tooling.</p>
 */
public enum SeverityLevel {

    BLOCKER("blocker"),
    CRITICAL("critical"),
    NORMAL("normal"),
    MINOR("minor"),
    TRIVIAL("trivial");

    private final String value;

    SeverityLevel(final String v) {
        value = v;
    }

    /**
     * Returns the annotation value.
     *
     * @return the annotation value
     */
    public String value() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return value();
    }
}
