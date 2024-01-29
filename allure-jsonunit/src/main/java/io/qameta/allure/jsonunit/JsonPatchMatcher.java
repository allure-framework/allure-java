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

import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;

import org.hamcrest.Description;

/**
 * JsonPatchMatcher is extension of JsonUnit matcher,
 * that generates pretty html attachment for differences.
 *
 * @param <T> the type
 */
@SuppressWarnings("unused")
public final class JsonPatchMatcher<T> extends AbstractJsonPatchMatcher<AllureConfigurableJsonMatcher<T>>
        implements AllureConfigurableJsonMatcher<T> {

    private final Object expected;

    private JsonPatchMatcher(final Object expected) {
        this.expected = expected;
    }

    public static <T> AllureConfigurableJsonMatcher<T> jsonEquals(final Object expected) {
        return new JsonPatchMatcher<T>(expected);
    }

    @Override
    public boolean matches(final Object actual) {
        super.withDifferenceListener(new JsonPatchListener());
        return super.matches(expected, actual);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("has no difference");
    }

    @Override
    public void describeMismatch(final Object item, final Description description) {
        description.appendText(super.getDifferences());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {
        //do nothing
    }

    @Override
    protected void render(final DifferenceListener listener) {
        final JsonPatchListener jsonDiffListener = (JsonPatchListener) listener;
        final DiffAttachment attachment = new DiffAttachment(jsonDiffListener.getDiffModel());
        new DefaultAttachmentProcessor().addAttachment(attachment,
                new FreemarkerAttachmentRenderer("diff.ftl"));
    }
}
