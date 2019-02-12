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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Diff;
import net.javacrumbs.jsonunit.core.internal.Options;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.math.BigDecimal;

/**
 * JsonPatchMatcher is extension of JsonUnit matcher,
 * that generates pretty html attachment for differences.
 *
 * @param <T> the type
 */
@SuppressWarnings("unused")
public final class JsonPatchMatcher<T> implements AllureConfigurableJsonMatcher<T> {

    private static final String EMPTY_PATH = "";
    private static final String FULL_JSON = "fullJson";
    private Configuration configuration = Configuration.empty();
    private final Object expected;
    private String differences;

    private JsonPatchMatcher(final Object expected) {
        this.expected = expected;
    }

    public static <T> AllureConfigurableJsonMatcher<T> jsonEquals(final Object expected) {
        return new JsonPatchMatcher<T>(expected);
    }

    @Override
    public AllureConfigurableJsonMatcher<T> withTolerance(final BigDecimal tolerance) {
        configuration = configuration.withTolerance(tolerance);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> withTolerance(final double tolerance) {
        configuration = configuration.withTolerance(tolerance);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> when(final Option first, final Option... next) {
        configuration = configuration.when(first, next);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> withOptions(final Options options) {
        configuration = configuration.withOptions(options);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> withMatcher(final String matcherName, final Matcher matcher) {
        configuration = configuration.withMatcher(matcherName, matcher);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> whenIgnoringPaths(final String... paths) {
        configuration = configuration.whenIgnoringPaths(paths);
        return this;
    }

    @Override
    public boolean matches(final Object actual) {
        final JsonPatchListener listener = new JsonPatchListener();
        final Diff diff = Diff.create(expected, actual, FULL_JSON, EMPTY_PATH,
                configuration.withDifferenceListener(listener));
        if (!diff.similar()) {
            differences = diff.differences();
            render(listener);
        }
        return diff.similar();
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("has no difference");
    }

    @Override
    public void describeMismatch(final Object item, final Description description) {
        description.appendText(differences);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {
        //do nothing
    }

    private void render(final JsonPatchListener listener) {
        final ObjectMapper mapper = new ObjectMapper();
        final String patch = listener.getJsonPatch();
        try {
            final String actual = mapper.writeValueAsString(listener.getContext().getActualSource());
            final String expected = mapper.writeValueAsString(listener.getContext().getExpectedSource());
            final DiffAttachment attachment = new DiffAttachment(actual, expected, patch);
            new DefaultAttachmentProcessor().addAttachment(attachment,
                    new FreemarkerAttachmentRenderer("diff.ftl"));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not process actual/expected json", e);
        }
    }
}
