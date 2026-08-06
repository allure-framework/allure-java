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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Test-run-level attachments and errors.
 *
 * @since 3.0
 */
public class Globals implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<GlobalAttachment> attachments = new ArrayList<>();
    private List<GlobalError> errors = new ArrayList<>();

    /**
     * Gets global attachments.
     *
     * @return the global attachments
     */
    public List<GlobalAttachment> getAttachments() {
        return attachments;
    }

    /**
     * Sets global attachments.
     *
     * @param value the global attachments
     * @return self for method chaining
     */
    public Globals setAttachments(final List<GlobalAttachment> value) {
        this.attachments = value;
        return this;
    }

    /**
     * Gets global errors.
     *
     * @return the global errors
     */
    public List<GlobalError> getErrors() {
        return errors;
    }

    /**
     * Sets global errors.
     *
     * @param value the global errors
     * @return self for method chaining
     */
    public Globals setErrors(final List<GlobalError> value) {
        this.errors = value;
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
        final Globals globals = (Globals) o;
        return Objects.equals(attachments, globals.attachments)
                && Objects.equals(errors, globals.errors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(attachments, errors);
    }
}
