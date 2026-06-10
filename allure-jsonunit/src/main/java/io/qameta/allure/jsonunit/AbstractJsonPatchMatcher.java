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
package io.qameta.allure.jsonunit;

import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.ConfigurationWhen.ApplicableForPath;
import net.javacrumbs.jsonunit.core.ConfigurationWhen.PathsParam;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Diff;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;
import org.hamcrest.Matcher;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Contains basic matcher functionality and implementation of methods for matching configuration.
 *
 * @param <T> the type
 */
@SuppressWarnings("unchecked")
public abstract class AbstractJsonPatchMatcher<T> {

    private static final String EMPTY_PATH = "";
    private static final String FULL_JSON = "fullJson";

    private Configuration configuration = Configuration.empty();
    private String differences;

    /**
     * Configures the tolerance.
     *
     * @param tolerance the tolerance
     * @return this instance for method chaining
     */
    public T withTolerance(final BigDecimal tolerance) {
        this.configuration = configuration.withTolerance(tolerance);
        return (T) this;
    }

    /**
     * Configures the tolerance.
     *
     * @param tolerance the tolerance
     * @return this instance for method chaining
     */
    public T withTolerance(final double tolerance) {
        this.configuration = configuration.withTolerance(tolerance);
        return (T) this;
    }

    /**
     * Applies JsonUnit matching options.
     *
     * @param first the first
     * @param next the next
     * @return this matcher for method chaining
     */
    public T when(final Option first, final Option... next) {
        this.configuration = configuration.when(first, next);
        return (T) this;
    }

    /**
     * Applies JsonUnit matching options.
     *
     * @param pathsParam the paths param
     * @param applicableForPaths the applicable for paths
     * @return this matcher for method chaining
     */
    public T when(final PathsParam pathsParam, final ApplicableForPath... applicableForPaths) {
        this.configuration = this.configuration.when(pathsParam, applicableForPaths);
        return (T) this;
    }

    /**
     * Configures the options.
     *
     * @param first the first
     * @param next the next
     * @return this instance for method chaining
     */
    public T withOptions(final Option first, final Option... next) {
        this.configuration = configuration.withOptions(first, next);
        return (T) this;
    }

    /**
     * Configures the options.
     *
     * @param options the options
     * @return this instance for method chaining
     */
    public T withOptions(final Collection<Option> options) {
        this.configuration = configuration.withOptions(options);
        return (T) this;
    }

    /**
     * Configures the matcher.
     *
     * @param matcherName the matcher name
     * @param matcher the matcher
     * @return this instance for method chaining
     */
    public T withMatcher(final String matcherName, final Matcher matcher) {
        this.configuration = configuration.withMatcher(matcherName, matcher);
        return (T) this;
    }

    /**
     * Applies JsonUnit matching options.
     *
     * @param paths the paths
     * @return this matcher for method chaining
     */
    public T whenIgnoringPaths(final String... paths) {
        this.configuration = configuration.whenIgnoringPaths(paths);
        return (T) this;
    }

    /**
     * Configures the difference listener.
     *
     * @param differenceListener the difference listener
     * @return this instance for method chaining
     */
    public T withDifferenceListener(final DifferenceListener differenceListener) {
        this.configuration = configuration.withDifferenceListener(differenceListener);
        return (T) this;
    }

    /**
     * Returns the matches.
     *
     * @param expected the expected
     * @param actual the actual
     * @return true when the condition is satisfied; false otherwise
     */
    public boolean matches(final Object expected, final Object actual) {
        final Diff diff = Diff.create(expected, actual, FULL_JSON, EMPTY_PATH, configuration);
        final boolean similar = diff.similar();
        if (!similar) {
            differences = diff.differences();
            render(configuration.getDifferenceListener());
        }
        return similar;
    }

    /**
     * Handles the render callback.
     *
     * @param listener the listener
     */
    protected abstract void render(DifferenceListener listener);

    /**
     * Returns the differences.
     *
     * @return the differences
     */
    public String getDifferences() {
        return differences;
    }

    /**
     * Returns the configuration.
     *
     * @return the configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }
}
