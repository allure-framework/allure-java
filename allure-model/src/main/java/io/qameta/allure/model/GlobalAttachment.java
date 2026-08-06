/*
 *  Copyright 2016-2026 Qameta Software Inc
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

import java.util.Objects;

/**
 * An attachment that belongs to the test run rather than to a test or fixture.
 *
 * @since 3.0
 */
public class GlobalAttachment extends Attachment {

    private static final long serialVersionUID = 1L;

    private long timestamp;

    /**
     * Gets the time when the attachment was added.
     *
     * @return the timestamp in milliseconds since the epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the time when the attachment was added.
     *
     * @param value the timestamp in milliseconds since the epoch
     * @return self for method chaining
     */
    public GlobalAttachment setTimestamp(final long value) {
        this.timestamp = value;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GlobalAttachment setName(final String value) {
        super.setName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GlobalAttachment setSource(final String value) {
        super.setSource(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GlobalAttachment setType(final String value) {
        super.setType(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GlobalAttachment setSize(final Long value) {
        super.setSize(value);
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
        final GlobalAttachment that = (GlobalAttachment) o;
        return timestamp == that.timestamp
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getSource(), that.getSource())
                && Objects.equals(getType(), that.getType())
                && Objects.equals(getSize(), that.getSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getSource(), getType(), getSize(), timestamp);
    }
}
