
package io.qameta.allure.model;

/**
 * <p>Java class for Stage.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Stage"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="scheduled"/&gt;
 *     &lt;enumeration value="running"/&gt;
 *     &lt;enumeration value="finished"/&gt;
 *     &lt;enumeration value="pending"/&gt;
 *     &lt;enumeration value="interrupted"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
public enum Stage {

    SCHEDULED("scheduled"),
    RUNNING("running"),
    FINISHED("finished"),
    PENDING("pending"),
    INTERRUPTED("interrupted");

    private final String value;

    Stage(final String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Stage fromValue(final String v) {
        for (Stage c: Stage.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
