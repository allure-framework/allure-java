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
