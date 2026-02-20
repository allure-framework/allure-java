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
import java.util.Objects;

/**
 * Model object that could be used to pass links to external resources to test results.
 *
 * @author baev (Dmitry Baev)
 * @see io.qameta.allure.model.WithLinks
 * @see io.qameta.allure.model.TestResult
 * @since 2.0
 */
public class Link implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String url;
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
    public Link setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets url.
     *
     * @param value the value
     * @return self for method chaining
     */
    public Link setUrl(final String value) {
        this.url = value;
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
    public Link setType(final String value) {
        this.type = value;
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
        final Link link = (Link) o;
        return Objects.equals(name, link.name)
                && Objects.equals(url, link.url)
                && Objects.equals(type, link.type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, url, type);
    }
}
