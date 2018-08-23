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
