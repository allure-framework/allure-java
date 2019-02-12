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
package io.qameta.allure.jsonunit;

import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Options;
import org.hamcrest.Matcher;

import java.math.BigDecimal;

/**
 * @param <T> the type of matcher
 * @see net.javacrumbs.jsonunit.ConfigurableJsonMatcher
 */
public interface AllureConfigurableJsonMatcher<T> extends Matcher<T> {

    AllureConfigurableJsonMatcher<T> withTolerance(BigDecimal tolerance);

    AllureConfigurableJsonMatcher<T> withTolerance(double tolerance);

    AllureConfigurableJsonMatcher<T> when(Option first, Option... next);

    AllureConfigurableJsonMatcher<T> withOptions(Options options);

    AllureConfigurableJsonMatcher<T> withMatcher(String matcherName, Matcher<?> matcher);

    AllureConfigurableJsonMatcher<T> whenIgnoringPaths(String... paths);
}
