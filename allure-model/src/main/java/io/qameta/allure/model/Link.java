package io.qameta.allure.model;

import java.io.Serializable;

/**
 * POJO that stores link information.
 */
public class Link implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String url;

    protected String type;

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
    public Link setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets the value of the url property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public Link setUrl(final String value) {
        this.url = value;
        return this;
    }

    /**
     * Gets the value of the type property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public Link setType(final String value) {
        this.type = value;
        return this;
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Link withName(final String value) {
        return setName(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Link withUrl(final String value) {
        return setUrl(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Link withType(final String value) {
        return setType(value);
    }
}
