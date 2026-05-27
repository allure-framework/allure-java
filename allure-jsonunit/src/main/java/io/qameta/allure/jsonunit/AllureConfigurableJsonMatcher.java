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

import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Options;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;
import org.hamcrest.Matcher;

import java.math.BigDecimal;

/**
 * @param <T> the type of matcher
 * @see net.javacrumbs.jsonunit.ConfigurableJsonMatcher
 * @deprecated Use {@link net.javacrumbs.jsonunit.ConfigurableJsonMatcher}
 */
@Deprecated
public interface AllureConfigurableJsonMatcher<T> extends Matcher<T> {

    /**
     * Configures the tolerance.
     *
     * @param tolerance the tolerance
     * @return this instance for method chaining
     */
    AllureConfigurableJsonMatcher<T> withTolerance(BigDecimal tolerance);

    /**
     * Configures the tolerance.
     *
     * @param tolerance the tolerance
     * @return this instance for method chaining
     */
    AllureConfigurableJsonMatcher<T> withTolerance(double tolerance);

    /**
     * Applies JsonUnit matching options.
     *
     * @param first the first
     * @param next the next
     * @return this matcher for method chaining
     */
    AllureConfigurableJsonMatcher<T> when(Option first, Option... next);

    /**
     * Configures the options.
     *
     * @param options the options
     * @return this instance for method chaining
     */
    AllureConfigurableJsonMatcher<T> withOptions(Options options);

    /**
     * Configures the matcher.
     *
     * @param matcherName the matcher name
     * @param matcher the matcher
     * @return this instance for method chaining
     */
    AllureConfigurableJsonMatcher<T> withMatcher(String matcherName, Matcher<?> matcher);

    /**
     * Applies JsonUnit matching options.
     *
     * @param paths the paths
     * @return this matcher for method chaining
     */
    AllureConfigurableJsonMatcher<T> whenIgnoringPaths(String... paths);

    /**
     * Configures the difference listener.
     *
     * @param differenceListener the difference listener
     * @return this instance for method chaining
     */
    AllureConfigurableJsonMatcher<T> withDifferenceListener(DifferenceListener differenceListener);
}
