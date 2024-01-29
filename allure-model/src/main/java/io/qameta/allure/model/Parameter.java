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
 * The model object that could be used to pass information about test parameters to test results.
 * Note that parameters with empty (blank) name will be omitted during report generation.
 * <p>
 * Parameters are used in history key generation for test results. In general words,
 * if test's results have the same parameters they will be considered as retries. But if
 * at least one parameter is differ (or, for example, missed) results will be considered
 * as separate results.
 * <p>
 * You can exclude parameters from history key calculation by setting {@link #excluded} property
 * to <code>true</code>.
 *
 * @author baev (Dmitry Baev)
 * @see io.qameta.allure.model.TestResult
 * @see io.qameta.allure.model.WithParameters
 * @since 2.0
 */
public class Parameter implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String value;
    private Boolean excluded;
    private Mode mode;

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
     * @return self for method chaining.
     */
    public Parameter setName(final String value) {
        this.name = value;
        return this;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets value.
     *
     * @param value the value
     * @return self for method chaining.
     */
    public Parameter setValue(final String value) {
        this.value = value;
        return this;
    }

    /**
     * Gets excluded.
     *
     * @return the excluded
     */
    public Boolean getExcluded() {
        return excluded;
    }

    /**
     * Sets excluded.
     *
     * @param excluded the excluded
     * @return self for method chaining.
     */
    public Parameter setExcluded(final Boolean excluded) {
        this.excluded = excluded;
        return this;
    }

    /**
     * Gets mode.
     *
     * @return the mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Sets mode.
     *
     * @param mode the mode
     * @return self for method chaining.
     */
    public Parameter setMode(final Mode mode) {
        this.mode = mode;
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
        final Parameter parameter = (Parameter) o;
        return Objects.equals(name, parameter.name)
                && Objects.equals(value, parameter.value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    /**
     * The parameter render mode.
     *
     * @author baev (Dmitry Baev)
     * @since 2.15
     */
    public enum Mode {

        /**
         * Completely hide parameter from report.
         */
        HIDDEN,

        /**
         * Display parameter, but mask it's value.
         */
        MASKED,

        /**
         * Default mode. Displays both parameter name and value.
         */
        DEFAULT

    }
}
