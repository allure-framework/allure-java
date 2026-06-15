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

import io.qameta.allure.Allure;
import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class JsonPatchMatcherTests {

    private static final String JSON = "{\"key1\":\"value1\"}";
    private static final String EMPTY = "";

    @Test
    void shouldMatchWhenIgnoringPaths() {
        final String actual = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals(JSON)
                .whenIgnoringPaths("key2", "key3");
        final boolean result = matches(matcher, actual);
        assertThat(result).isTrue();
    }

    @Test
    void shouldMatchWithOptions() {
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals("[1,2]")
                .withOptions(List.of(Option.IGNORING_ARRAY_ORDER));
        final boolean result = matches(matcher, "[2,1]");
        assertThat(result).isTrue();
    }

    @Test
    void shouldMatchWhenOptions() {
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals("[1,2]")
                .when(Option.IGNORING_ARRAY_ORDER);
        final boolean result = matches(matcher, "[2,1]");
        assertThat(result).isTrue();
    }

    @Test
    void shouldMatchWithToleranceDouble() {
        final boolean result = matches(
                (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals("1.01").withTolerance(0.01),
                "1"
        );
        assertThat(result).isTrue();
    }

    @Test
    void shouldSetDifferenceListener() {
        final DifferenceListener listener = mock(DifferenceListener.class);
        final JsonPatchMatcher<?> matcher = Allure.step("Configure JSON patch matcher difference listener", () -> {
            return (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals(JSON)
                    .withDifferenceListener(listener);
        });
        acceptConfiguration(matcher, (f, c) -> {
            assertThat(c.getDifferenceListener()).isSameAs(listener);
        });
    }

    @Test
    void shouldMatchWithToleranceBigDecimal() {
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals("1.01")
                .withTolerance(BigDecimal.valueOf(0.01));
        final boolean result = matches(matcher, "1");
        assertThat(result).isTrue();
    }

    @Test
    void shouldMatchWithMatcher() {
        final String expected = "{\"key\":\"${json-unit.matches:matcher}\"}";
        final String actual = "{\"key\":\"value\"}";
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals(expected)
                .withMatcher("matcher", equalTo("value"));
        final boolean result = matches(matcher, actual);
        assertThat(result).isTrue();
    }

    @Test
    void shouldDescribeTo() {
        final Description description = mock(Description.class);
        describeTo((JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals(EMPTY), description);
        verify(description).appendText("has no difference");
        verifyNoMoreInteractions(description);
    }

    @Test
    void shouldDescribeMismatch() {
        final Description description = mock(Description.class);
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals("data");
        matches(matcher, EMPTY);
        final ArgumentCaptor<String> captor = forClass(String.class);
        describeMismatch(matcher, description);
        verify(description).appendText(captor.capture());
        verifyNoMoreInteractions(description);
        assertThat(captor.getValue()).isNotBlank();
    }

    @Test
    void shouldMatchAndNoRender() {
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals(JSON);
        final JsonPatchListener listener = mockConfiguration(matcher);
        final boolean result = matches(matcher, JSON);
        assertThat(result).isTrue();
        verify(listener, never()).getDiffModel();
    }

    @Test
    void shouldNotMatchAndRender() {
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals(JSON);
        final JsonPatchListener listener = mockConfiguration(matcher);
        final DiffModel diffModel = new DiffModel(EMPTY, EMPTY, EMPTY);
        doReturn(diffModel).when(listener).getDiffModel();
        final boolean result = matches(matcher, EMPTY);
        assertThat(result).isFalse();
        verify(listener).getDiffModel();
    }

    private static JsonPatchListener mockConfiguration(JsonPatchMatcher<?> matcher) {
        final JsonPatchListener jsonPatchListener = mock(JsonPatchListener.class);
        acceptConfiguration(matcher, (f, c) -> {
            try {
                final Configuration configurationSpy = spy(c.withDifferenceListener(jsonPatchListener));
                doReturn(configurationSpy).when(configurationSpy).withDifferenceListener(any());
                f.set(matcher, configurationSpy);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to mock configuration", e);
            }
        });
        return jsonPatchListener;
    }

    private static void acceptConfiguration(JsonPatchMatcher<?> matcher, BiConsumer<Field, Configuration> consumer) {
        Allure.step("Read JSON patch matcher configuration", () -> {
            try {
                final Field configurationField = matcher.getClass().getSuperclass().getDeclaredField("configuration");
                configurationField.setAccessible(true);
                consumer.accept(configurationField, (Configuration) configurationField.get(matcher));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to access configuration field", e);
            }
        });
    }

    private static boolean matches(final JsonPatchMatcher<?> matcher, final String actual) {
        return Allure.step("Evaluate JSON patch matcher", step -> {
            step.parameter("actual", actual);
            return matcher.matches(actual);
        });
    }

    private static void describeTo(final JsonPatchMatcher<?> matcher, final Description description) {
        Allure.step("Describe JSON patch matcher", () -> {
            matcher.describeTo(description);
        });
    }

    private static void describeMismatch(final JsonPatchMatcher<?> matcher, final Description description) {
        Allure.step("Describe JSON patch matcher mismatch", () -> {
            matcher.describeMismatch(null, description);
        });
    }
}
