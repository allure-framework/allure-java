/*
 *  Copyright 2016-2024 Qameta Software Inc
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
import java.util.Objects;

/**
 * The type Status details.
 *
 * @author baev (Dmitry Baev)
 * @see io.qameta.allure.model.WithStatusDetails
 * @since 2.0
 */
public class StatusDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean known;
    private boolean muted;
    private boolean flaky;
    private String message;
    private String trace;

    /**
     * Is known boolean.
     *
     * @return the boolean
     */
    public boolean isKnown() {
        return known;
    }

    /**
     * Sets known.
     *
     * @param value the value
     * @return self for method chaining
     */
    public StatusDetails setKnown(final boolean value) {
        this.known = value;
        return this;
    }

    /**
     * Is muted boolean.
     *
     * @return the boolean
     */
    public boolean isMuted() {
        return muted;
    }

    /**
     * Sets muted.
     *
     * @param value the value
     * @return self for method chaining
     */
    public StatusDetails setMuted(final boolean value) {
        this.muted = value;
        return this;
    }

    /**
     * Is flaky boolean.
     *
     * @return the boolean
     */
    public boolean isFlaky() {
        return flaky;
    }

    /**
     * Sets flaky.
     *
     * @param value the value
     * @return self for method chaining
     */
    public StatusDetails setFlaky(final boolean value) {
        this.flaky = value;
        return this;
    }

    /**
     * Gets message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets message.
     *
     * @param value the value
     * @return self for method chaining
     */
    public StatusDetails setMessage(final String value) {
        this.message = value;
        return this;
    }

    /**
     * Gets trace.
     *
     * @return the trace
     */
    public String getTrace() {
        return trace;
    }

    /**
     * Sets trace.
     *
     * @param value the value
     * @return self for method chaining
     */
    public StatusDetails setTrace(final String value) {
        this.trace = value;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final StatusDetails that = (StatusDetails) o;
        return Objects.equals(message, that.message)
                && Objects.equals(trace, that.trace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(message, trace);
    }

    /**
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "StatusDetails(" +
                "known=" + this.known + ", " +
                "muted=" + this.muted + ", " +
                "flaky=" + this.flaky + ", " +
                "message=" + this.message + ", " +
                "trace=" + this.trace + ")";
    }
}
