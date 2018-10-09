
package io.qameta.allure.model;

import java.io.Serializable;

/**
 * <p>Java class for StatusDetails complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StatusDetails"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="known" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="muted" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="flaky" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="message" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="trace" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
public class StatusDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    protected boolean known;

    protected boolean muted;

    protected boolean flaky;

    protected String message;

    protected String trace;

    /**
     * Gets the value of the known property.
     * 
     */
    public boolean isKnown() {
        return known;
    }

    /**
     * Sets the value of the known property.
     * 
     */
    public void setKnown(final boolean value) {
        this.known = value;
    }

    /**
     * Gets the value of the muted property.
     * 
     */
    public boolean isMuted() {
        return muted;
    }

    /**
     * Sets the value of the muted property.
     * 
     */
    public void setMuted(final boolean value) {
        this.muted = value;
    }

    /**
     * Gets the value of the flaky property.
     * 
     */
    public boolean isFlaky() {
        return flaky;
    }

    /**
     * Sets the value of the flaky property.
     * 
     */
    public void setFlaky(final boolean value) {
        this.flaky = value;
    }

    /**
     * Gets the value of the message property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the value of the message property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessage(final String value) {
        this.message = value;
    }

    /**
     * Gets the value of the trace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrace() {
        return trace;
    }

    /**
     * Sets the value of the trace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrace(final String value) {
        this.trace = value;
    }

    public StatusDetails withKnown(final boolean value) {
        setKnown(value);
        return this;
    }

    public StatusDetails withMuted(final boolean value) {
        setMuted(value);
        return this;
    }

    public StatusDetails withFlaky(final boolean value) {
        setFlaky(value);
        return this;
    }

    public StatusDetails withMessage(final String value) {
        setMessage(value);
        return this;
    }

    public StatusDetails withTrace(final String value) {
        setTrace(value);
        return this;
    }

}
