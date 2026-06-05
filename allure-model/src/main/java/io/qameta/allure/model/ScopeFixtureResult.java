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
 * Represents a fixture result linked to a logical scope.
 */
public class ScopeFixtureResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uuid;
    private FixtureResult value;
    private ScopeFixtureType type;
    private String scopeUuid;

    /**
     * Gets uuid.
     *
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets uuid.
     *
     * @param value the value
     * @return self for method chaining
     */
    public ScopeFixtureResult setUuid(final String value) {
        this.uuid = value;
        return this;
    }

    /**
     * Gets fixture result.
     *
     * @return the fixture result
     */
    public FixtureResult getValue() {
        return value;
    }

    /**
     * Sets fixture result.
     *
     * @param value the value
     * @return self for method chaining
     */
    public ScopeFixtureResult setValue(final FixtureResult value) {
        this.value = value;
        return this;
    }

    /**
     * Gets fixture type.
     *
     * @return the fixture type
     */
    public ScopeFixtureType getType() {
        return type;
    }

    /**
     * Sets fixture type.
     *
     * @param value the value
     * @return self for method chaining
     */
    public ScopeFixtureResult setType(final ScopeFixtureType value) {
        this.type = value;
        return this;
    }

    /**
     * Gets owning scope uuid.
     *
     * @return the owning scope uuid
     */
    public String getScopeUuid() {
        return scopeUuid;
    }

    /**
     * Sets owning scope uuid.
     *
     * @param value the value
     * @return self for method chaining
     */
    public ScopeFixtureResult setScopeUuid(final String value) {
        this.scopeUuid = value;
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
        final ScopeFixtureResult that = (ScopeFixtureResult) o;
        return Objects.equals(uuid, that.uuid)
                && Objects.equals(scopeUuid, that.scopeUuid)
                && type == that.type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(uuid, type, scopeUuid);
    }
}
