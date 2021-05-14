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
import java.util.Objects;

/**
 * POJO that stores link information.
 */
public class Link implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String url;

    protected String type;

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public Link setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets the value of the url property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public Link setUrl(final String value) {
        this.url = value;
        return this;
    }

    /**
     * Gets the value of the type property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public Link setType(final String value) {
        this.type = value;
        return this;
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Link withName(final String value) {
        return setName(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Link withUrl(final String value) {
        return setUrl(value);
    }

    /**
     * @deprecated use set method
     */
    @Deprecated
    public Link withType(final String value) {
        return setType(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Link link = (Link) o;
        return Objects.equals(getName(), link.getName()) && Objects.equals(getUrl(), link.getUrl()) && Objects.equals(
                getType(), link.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getUrl(), getType());
    }
}
