
package io.qameta.allure.model;

import java.io.Serializable;

/**
 * POJO that stores information about test run.
 * @deprecated scheduled for removal in 3.0 release
 */
@Deprecated
public class TestRunResult implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String uuid;

    protected String name;

    /**
     * Gets the value of the uuid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the value of the uuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public TestRunResult setUuid(final String value) {
        this.uuid = value;
        return this;
    }

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
    public TestRunResult setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestRunResult withUuid(final String value) {
        return setUuid(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public TestRunResult withName(final String value) {
        return setName(value);
    }
}
