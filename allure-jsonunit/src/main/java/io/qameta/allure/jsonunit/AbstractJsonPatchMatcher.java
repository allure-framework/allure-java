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
package io.qameta.allure.jsonunit;

import java.math.BigDecimal;

import org.hamcrest.Matcher;

import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.ConfigurationWhen.ApplicableForPath;
import net.javacrumbs.jsonunit.core.ConfigurationWhen.PathsParam;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Diff;
import net.javacrumbs.jsonunit.core.internal.Options;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;

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

    public T withTolerance(final BigDecimal tolerance) {
        this.configuration = configuration.withTolerance(tolerance);
        return (T) this;
    }

    public T withTolerance(final double tolerance) {
        this.configuration = configuration.withTolerance(tolerance);
        return (T) this;
    }

    public T when(final Option first, final Option... next) {
        this.configuration = configuration.when(first, next);
        return (T) this;
    }

    public T when(final PathsParam pathsParam, final ApplicableForPath... applicableForPaths) {
        this.configuration = this.configuration.when(pathsParam, applicableForPaths);
        return (T) this;
    }

    public T withOptions(final Options options) {
        this.configuration = configuration.withOptions(options);
        return (T) this;
    }

    public T withMatcher(final String matcherName, final Matcher matcher) {
        this.configuration = configuration.withMatcher(matcherName, matcher);
        return (T) this;
    }

    public T whenIgnoringPaths(final String... paths) {
        this.configuration = configuration.whenIgnoringPaths(paths);
        return (T) this;
    }

    public T withDifferenceListener(final DifferenceListener differenceListener) {
        this.configuration = configuration.withDifferenceListener(differenceListener);
        return (T) this;
    }

    public boolean matches(final Object expected, final Object actual) {
        final Diff diff = Diff.create(expected, actual, FULL_JSON, EMPTY_PATH, configuration);
        final boolean similar = diff.similar();
        if (!similar) {
            differences = diff.differences();
            render(configuration.getDifferenceListener());
        }
        return similar;
    }

    protected abstract void render(DifferenceListener listener);

    public String getDifferences() {
        return differences;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
