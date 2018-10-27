package io.qameta.allure.model;

import java.io.Serializable;

/**
 * POJO that stores attachment information.
 */
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String source;

    protected String type;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public Attachment setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets the value of the source property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public Attachment setSource(final String value) {
        this.source = value;
        return this;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public Attachment setType(final String value) {
        this.type = value;
        return this;
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Attachment withName(final String value) {
        return setName(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Attachment withSource(final String value) {
        return setSource(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Attachment withType(final String value) {
        return setType(value);
    }
}
