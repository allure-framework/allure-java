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
package io.qameta.allure.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Test statuses.
 */
public enum Status {

    FAILED("failed"),
    BROKEN("broken"),
    PASSED("passed"),
    SKIPPED("skipped");

    private final String value;

    Status(final String v) {
        value = v;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static Status fromValue(final String v) {
        for (Status c : Status.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
