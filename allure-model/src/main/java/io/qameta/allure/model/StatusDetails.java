/*
 *  Copyright 2019 Qameta Software OÜ
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

import java.io.Serializable;

/**
 * POJO that stores status details.
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
     */
    public boolean isKnown() {
        return known;
    }

    /**
     * Sets the value of the known property.
     */
    public StatusDetails setKnown(final boolean value) {
        this.known = value;
        return this;
    }

    /**
     * Gets the value of the muted property.
     */
    public boolean isMuted() {
        return muted;
    }

    /**
     * Sets the value of the muted property.
     */
    public StatusDetails setMuted(final boolean value) {
        this.muted = value;
        return this;
    }

    /**
     * Gets the value of the flaky property.
     */
    public boolean isFlaky() {
        return flaky;
    }

    /**
     * Sets the value of the flaky property.
     */
    public StatusDetails setFlaky(final boolean value) {
        this.flaky = value;
        return this;
    }

    /**
     * Gets the value of the message property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the value of the message property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public StatusDetails setMessage(final String value) {
        this.message = value;
        return this;
    }

    /**
     * Gets the value of the trace property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getTrace() {
        return trace;
    }

    /**
     * Sets the value of the trace property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public StatusDetails setTrace(final String value) {
        this.trace = value;
        return this;
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public StatusDetails withKnown(final boolean value) {
        return setKnown(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public StatusDetails withMuted(final boolean value) {
        return setMuted(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public StatusDetails withFlaky(final boolean value) {
        return setFlaky(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public StatusDetails withMessage(final String value) {
        return setMessage(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public StatusDetails withTrace(final String value) {
        return setTrace(value);
    }
}
