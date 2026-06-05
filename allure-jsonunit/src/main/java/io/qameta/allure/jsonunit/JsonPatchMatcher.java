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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.qameta.allure.Allure;
import net.javacrumbs.jsonunit.ConfigurableJsonMatcher;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;
import org.hamcrest.Description;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * JsonPatchMatcher is extension of JsonUnit matcher,
 * that generates pretty html attachment for differences.
 *
 * @param <T> the type
 */
@SuppressWarnings("unused")
public final class JsonPatchMatcher<T> extends AbstractJsonPatchMatcher<ConfigurableJsonMatcher<T>>
        implements
            ConfigurableJsonMatcher<T> {

    private static final Configuration FREEMARKER = configuration();

    private final Object expected;

    private JsonPatchMatcher(final Object expected) {
        this.expected = expected;
    }

    public static <T> ConfigurableJsonMatcher<T> jsonEquals(final Object expected) {
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
        Allure.addAttachment("JSON difference", "text/html", render(attachment), ".html");
    }

    private static String render(final DiffAttachment attachment) {
        try (Writer writer = new StringWriter()) {
            final Template template = FREEMARKER.getTemplate("diff.ftl");
            template.process(Map.of("data", attachment), writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Could not render JSON difference attachment", e);
        }
    }

    private static Configuration configuration() {
        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setLocalizedLookup(false);
        configuration.setTemplateUpdateDelayMilliseconds(0);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        configuration.setClassLoaderForTemplateLoading(JsonPatchMatcher.class.getClassLoader(), "tpl");
        return configuration;
    }
}
