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

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.ArgumentCaptor.forClass;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.function.BiConsumer;

import org.hamcrest.Description;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Options;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;

class JsonPatchMatcherTests {

    private static final String JSON = "{\"key1\":\"value1\"}";
    private static final String EMPTY = "";

    @Test
    void shouldMatchWhenIgnoringPaths() {
        final String actual = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        final boolean result = JsonPatchMatcher.jsonEquals(JSON)
                .whenIgnoringPaths("key2", "key3")
                .matches(actual);
        assertThat(result).isTrue();
    }

    @Test
    void shouldMatchWithOptions() {
        final boolean result = JsonPatchMatcher.jsonEquals("[1,2]")
                .withOptions(Options.empty().with(Option.IGNORING_ARRAY_ORDER))
                .matches("[2,1]");
        assertThat(result).isTrue();
    }

    @Test
    void shouldMatchWhenOptions() {
        final boolean result = JsonPatchMatcher.jsonEquals("[1,2]")
                .when(Option.IGNORING_ARRAY_ORDER).matches("[2,1]");
        assertThat(result).isTrue();
    }

    @Test
    void shouldMatchWithToleranceDouble() {
        final boolean result = JsonPatchMatcher.jsonEquals("1.01")
                .withTolerance(0.01).matches("1");
        assertThat(result).isTrue();
    }

    @Test
    void shouldSetDifferenceListener() {
        final DifferenceListener listener = mock(DifferenceListener.class);
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals(JSON)
                .withDifferenceListener(listener);
        acceptConfiguration(matcher, (f, c) -> {
            assertThat(c.getDifferenceListener()).isSameAs(listener);
        });
    }

    @Test
    void shouldMatchWithToleranceBigDecimal() {
        final boolean result = JsonPatchMatcher.jsonEquals("1.01")
                .withTolerance(BigDecimal.valueOf(0.01)).matches("1");
        assertThat(result).isTrue();
    }

    @Test
    void shouldMatchWithMatcher() {
        final String expected = "{\"key\":\"${json-unit.matches:matcher}\"}";
        final String actual = "{\"key\":\"value\"}";
        final boolean result = JsonPatchMatcher.jsonEquals(expected)
                .withMatcher("matcher", equalTo("value"))
                .matches(actual);
        assertThat(result).isTrue();
    }

    @Test
    void shouldDescribeTo() {
        final Description description = mock(Description.class);
        JsonPatchMatcher.jsonEquals(EMPTY).describeTo(description);
        verify(description).appendText("has no difference");
        verifyNoMoreInteractions(description);
    }

    @Test
    void shouldDescribeMismatch() {
        final Description description = mock(Description.class);
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals("data");
        matcher.matches(EMPTY);
        final ArgumentCaptor<String> captor = forClass(String.class);
        matcher.describeMismatch(null, description);
        verify(description).appendText(captor.capture());
        verifyNoMoreInteractions(description);
        assertThat(captor.getValue()).isNotBlank();
    }

    @Test
    void shouldMatchAndNoRender() {
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals(JSON);
        final JsonPatchListener listener = mockConfiguration(matcher);
        final boolean result = matcher.matches(JSON);
        assertThat(result).isTrue();
        verify(listener, never()).getDiffModel();
    }

    @Test
    void shouldNotMatchAndRender() {
        final JsonPatchMatcher<?> matcher = (JsonPatchMatcher<?>) JsonPatchMatcher.jsonEquals(JSON);
        final JsonPatchListener listener = mockConfiguration(matcher);
        final DiffModel diffModel = new DiffModel(EMPTY, EMPTY, EMPTY);
        doReturn(diffModel).when(listener).getDiffModel();
        final boolean result = matcher.matches(EMPTY);
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
        try {
            final Field configurationField = matcher.getClass().getSuperclass().getDeclaredField("configuration");
            configurationField.setAccessible(true);
            consumer.accept(configurationField, (Configuration) configurationField.get(matcher));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access configuration field", e);
        }
    }
}
