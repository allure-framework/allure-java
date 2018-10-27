package io.qameta.allure.model;

import java.io.Serializable;

/**
 * POJO that stores parameter information.
 */
public class Parameter implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String value;

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public Parameter setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets the value of the value property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public Parameter setValue(final String value) {
        this.value = value;
        return this;
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Parameter withName(final String value) {
        return setName(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Parameter withValue(final String value) {
        return setValue(value);
    }
}
