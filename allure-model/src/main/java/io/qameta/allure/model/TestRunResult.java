
package io.qameta.allure.model;

import java.io.Serializable;

/**
 * <p>Java class for TestRunResult complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TestRunResult"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="uuid" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
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
