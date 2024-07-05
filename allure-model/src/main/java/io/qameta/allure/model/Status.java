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
package io.qameta.allure.model;

/**
 * Test statuses.
 *
 * @author baev (Dmitry Baev)
 * @see io.qameta.allure.model.WithStatus
 * @since 2.0
 */
public enum Status {

    /**
     * Marks tests that have some failed checks (assertions).
     */
    FAILED("failed"),
    /**
     * Marks tests with unexpected failures during test execution.
     */
    BROKEN("broken"),
    /**
     * Marks passed tests.
     */
    PASSED("passed"),
    /**
     * Marks skipped/interrupted tests.
     */
    SKIPPED("skipped");

    private final String value;

    Status(final String v) {
        value = v;
    }

    /**
     * From value status.
     *
     * @param v the v
     * @return the status
     */
    public static Status fromValue(final String v) {
        for (Status c : values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    /**
     * Value string.
     *
     * @return the string
     */
    public String value() {
        return value;
    }

}
