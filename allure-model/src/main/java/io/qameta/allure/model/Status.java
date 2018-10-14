
package io.qameta.allure.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * <p>Java class for Status.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Status"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="failed"/&gt;
 *     &lt;enumeration value="broken"/&gt;
 *     &lt;enumeration value="passed"/&gt;
 *     &lt;enumeration value="skipped"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
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
        for (Status c: Status.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
