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
 * Test stages.
 *
 * @author baev (Dmitry Baev)
 * @since 2.0
 */
public enum Stage {

    /**
     * Scheduled stage.
     */
    SCHEDULED("scheduled"),
    /**
     * Running stage.
     */
    RUNNING("running"),
    /**
     * Finished stage.
     */
    FINISHED("finished"),
    /**
     * Pending stage.
     */
    PENDING("pending"),
    /**
     * Interrupted stage.
     */
    INTERRUPTED("interrupted");

    private final String value;

    Stage(final String v) {
        value = v;
    }

    /**
     * From value stage.
     *
     * @param v the v
     * @return the stage
     */
    public static Stage fromValue(final String v) {
        for (Stage c : values()) {
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

    /**
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "Stage(" + this.value + ")";
    }
}
