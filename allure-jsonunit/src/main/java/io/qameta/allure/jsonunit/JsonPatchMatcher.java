package io.qameta.allure.jsonunit;

import com.google.gson.GsonBuilder;
import io.qameta.allure.Step;
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
 * @param <T> the type
 */
public final class JsonPatchMatcher<T> implements AllureConfigurableJsonMatcher<T> {
    private static final String EMPTY_PATH = "";
    private static final String FULL_JSON = "fullJson";
    private Configuration configuration = Configuration.empty();
    private final Object expected;
    private String differences;

    private JsonPatchMatcher(final Object expected) {
        this.expected = expected;
    }

    @Step("Has no difference")
    public static <T> AllureConfigurableJsonMatcher<T> jsonEquals(final Object expected) {
        return new JsonPatchMatcher<T>(expected);
    }

    public AllureConfigurableJsonMatcher<T> withTolerance(final BigDecimal tolerance) {
        configuration = configuration.withTolerance(tolerance);
        return this;
    }

    public AllureConfigurableJsonMatcher<T> withTolerance(final double tolerance) {
        configuration = configuration.withTolerance(tolerance);
        return this;
    }

    public AllureConfigurableJsonMatcher<T> when(final Option first, final Option... next) {
        configuration = configuration.when(first, next);
        return this;
    }

    public AllureConfigurableJsonMatcher<T> withOptions(final Options options) {
        configuration = configuration.withOptions(options);
        return this;
    }

    public AllureConfigurableJsonMatcher<T> withMatcher(final String matcherName, final Matcher matcher) {
        configuration = configuration.withMatcher(matcherName, matcher);
        return this;
    }

    public AllureConfigurableJsonMatcher<T> whenIgnoringPaths(final String... paths) {
        configuration = configuration.whenIgnoringPaths(paths);
        return this;
    }

    private void render(final JsonPatchListener listener) {
        final Object actual = new GsonBuilder().create().toJson(listener.getContext().getActualSource());
        final Object expected = new GsonBuilder().create().toJson(listener.getContext().getExpectedSource());
        final String patch = listener.getJsonPatch();
        final DiffAttachment attachment = new DiffAttachment(actual.toString(), expected.toString(), patch);
        new DefaultAttachmentProcessor().addAttachment(attachment,
                new FreemarkerAttachmentRenderer("diff.ftl"));
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
}
