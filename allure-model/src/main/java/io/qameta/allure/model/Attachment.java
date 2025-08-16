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

/**
 * The model object that used to link attachment files, stored in results directory,
 * to test results.
 *
 * @author baev (Dmitry Baev)
 * @see io.qameta.allure.model.WithAttachments
 * @since 2.0
 */
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String source;
    private String type;

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param value the value
     * @return self for method chaining
     */
    public Attachment setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets source.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets source.
     *
     * @param value the value
     * @return self for method chaining
     */
    public Attachment setSource(final String value) {
        this.source = value;
        return this;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets type.
     *
     * @param value the value
     * @return self for method chaining
     */
    public Attachment setType(final String value) {
        this.type = value;
        return this;
    }

    /**
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "Attachment(" +
                "name=" + this.name + ", " +
                "source=" + this.source + ", " +
                "type=" + this.type + ")";
    }
}
