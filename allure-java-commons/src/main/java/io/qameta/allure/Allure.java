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
package io.qameta.allure;

import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeSerializer;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.qameta.allure.util.ResultsUtils.EPIC_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.FEATURE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.ISSUE_LINK_TYPE;
import static io.qameta.allure.util.ResultsUtils.STORY_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.SUITE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TMS_LINK_TYPE;
import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * The class contains some useful methods to work with {@link AllureLifecycle}.
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class Allure {

    private static final String TEXT_PLAIN = "text/plain";

    private static AllureLifecycle lifecycle;

    /**
     * Do not instance.
     */
    private Allure() {
        throw new IllegalStateException("Do not instance");
    }

    /**
     * Returns {@link AllureLifecycle} for low level operations with results.
     *
     * @return the lifecycle.
     */
    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = new AllureLifecycle();
        }
        return lifecycle;
    }

    /**
     * Sets the process-wide {@link AllureLifecycle}.
     */
    public static void setLifecycle(final AllureLifecycle lifecycle) {
        Allure.lifecycle = lifecycle;
    }

    /**
     * Adds passed step with provided name in current test or step (or test fixture). Takes no effect
     * if no test run at the moment. Shortcut for {@link #step(String, Status)}.
     *
     * @param name the name of step.
     */
    public static void step(final String name) {
        step(name, Status.PASSED);
    }

    /**
     * Adds step with provided name and status in current test or step (or test fixture). Takes no effect
     * if no test run at the moment.
     *
     * @param name   the name of step.
     * @param status the step status.
     */
    public static void step(final String name, final Status status) {
        getLifecycle().logStep(new StepResult().setName(name).setStatus(status));
    }

    /**
     * Syntax sugar for {@link #step(String, ThrowableRunnable)}.
     *
     * @param name     the name of step.
     * @param runnable the step's body.
     */
    public static void step(final String name, final ThrowableRunnableVoid runnable) {
        step(name, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Syntax sugar for {@link #step(ThrowableContextRunnable)}.
     *
     * @param name     the name of step.
     * @param runnable the step's body.
     */
    public static <T> T step(final String name, final ThrowableRunnable<T> runnable) {
        return step(step -> {
            step.name(name);
            return runnable.run();
        });
    }

    /**
     * Syntax sugar for {@link #step(ThrowableContextRunnable)}.
     *
     * @param runnable the step's body.
     */
    public static void step(final ThrowableContextRunnableVoid<StepContext> runnable) {
        step(step -> {
            runnable.run(step);
            return null;
        });
    }

    /**
     * Syntax sugar for {@link #step(ThrowableContextRunnable)}.
     *
     * @param name     the name of step.
     * @param runnable the step's body.
     */
    public static void step(final String name, final ThrowableContextRunnableVoid<StepContext> runnable) {
        step(step -> {
            step.name(name);
            runnable.run(step);
            return null;
        });
    }

    /**
     * Syntax sugar for {@link #step(ThrowableContextRunnable)}.
     *
     * @param name     the name of step.
     * @param runnable the step's body.
     */
    public static <T> T step(final String name, final ThrowableContextRunnable<T, StepContext> runnable) {
        return step(step -> {
            step.name(name);
            return runnable.run(step);
        });
    }

    /**
     * Run provided {@link ThrowableRunnable} as step with given name. Takes no effect
     * if no test run at the moment.
     *
     * @param runnable the step's body.
     */
    public static <T> T step(final ThrowableContextRunnable<T, StepContext> runnable) {
        final AllureExternalKey key = AllureExternalKey.random(Allure.class);
        getLifecycle().startStep(key, new StepResult().setName("step"));

        try {
            final T result = runnable.run(new DefaultStepContext(key));
            getLifecycle().updateStep(key, step -> step.setStatus(Status.PASSED));
            return result;
        } catch (Throwable throwable) {
            getLifecycle().updateStep(
                    key,
                    s -> s
                            .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                            .setStatusDetails(getStatusDetails(throwable).orElse(null))
            );
            throw ExceptionUtils.sneakyThrow(throwable);
        } finally {
            getLifecycle().stopStep();
        }
    }

    /**
     * Starts a stage — a lightweight marker for a semantic test phase, rendered as a regular step. A stage has no
     * explicit stop: it stays open, collecting the steps and attachments that follow, until the next stage starts
     * or the enclosing step, test, or fixture ends. A stage started inside a step becomes a child of that step.
     * Takes no effect if no test run at the moment.
     *
     * <pre><code>
     * Allure.stage("prepare data");
     * final Customer customer = createCustomer();
     *
     * Allure.stage("submit order");
     * final Order order = submitOrder(customer);
     *
     * Allure.stage("verify result");
     * assertThat(order.getStatus()).isEqualTo("created");
     * </code></pre>
     *
     * @param name the name of the stage.
     */
    public static void stage(final String name) {
        getLifecycle().startStage(new StepResult().setName(name));
    }

    /**
     * Adds epic label to current test if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #label(String, String)}.
     *
     * @param value the value of label.
     */
    public static void epic(final String value) {
        label(EPIC_LABEL_NAME, value);
    }

    /**
     * Adds feature label to current test if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #label(String, String)}.
     *
     * @param value the value of label.
     */
    public static void feature(final String value) {
        label(FEATURE_LABEL_NAME, value);
    }

    /**
     * Adds story label to current test if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #label(String, String)}.
     *
     * @param value the value of label.
     */
    public static void story(final String value) {
        label(STORY_LABEL_NAME, value);
    }

    /**
     * Adds suite label to current test if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #label(String, String)}.
     *
     * @param value the value of label.
     */
    public static void suite(final String value) {
        label(SUITE_LABEL_NAME, value);
    }

    /**
     * Adds label to current test if any. Takes no effect
     * if no test run at the moment.
     *
     * @param name  the name of label.
     * @param value the value of label.
     */
    public static void label(final String name, final String value) {
        final Label label = new Label().setName(name).setValue(value);
        getLifecycle().updateTestMetadata(metadata -> metadata.getLabels().add(label));
    }

    /**
     * Adds parameter to current test if any. Takes no effect
     * if no test run at the moment.
     * <p>
     * Shortcut for {@link #parameter(String, Object, Boolean, Parameter.Mode)}.
     *
     * @param name  the name of parameter.
     * @param value the value of parameter.
     */
    public static <T> T parameter(final String name, final T value) {
        return parameter(name, value, null, null);
    }

    /**
     * Adds parameter to current test if any. Takes no effect
     * if no test run at the moment.
     * <p>
     * Shortcut for {@link #parameter(String, Object, Boolean, Parameter.Mode)}.
     *
     * @param name     the name of parameter.
     * @param value    the value of parameter.
     * @param excluded true if parameter should be excluded from history key calculation, false otherwise.
     * @return the specified value.
     */
    public static <T> T parameter(final String name, final T value, final Boolean excluded) {
        return parameter(name, value, excluded, null);
    }

    /**
     * Adds parameter to current test if any. Takes no effect
     * if no test run at the moment.
     * <p>
     * Shortcut for {@link #parameter(String, Object, Boolean, Parameter.Mode)}.
     *
     * @param name  the name of parameter.
     * @param value the value of parameter.
     * @param mode  the parameter mode.
     * @return the specified value.
     */
    public static <T> T parameter(final String name, final T value,
                                  final Parameter.Mode mode) {
        return parameter(name, value, null, mode);
    }

    /**
     * Adds parameter to current test if any. Takes no effect
     * if no test run at the moment.
     *
     * @param name     the name of parameter.
     * @param value    the value of parameter.
     * @param excluded true if parameter should be excluded from history key calculation, false otherwise.
     * @param mode     the parameter mode.
     * @return the specified value.
     */
    public static <T> T parameter(final String name, final T value,
                                  final Boolean excluded, final Parameter.Mode mode) {
        final Parameter parameter = createParameter(name, value, excluded, mode);
        getLifecycle().updateTestMetadata(metadata -> metadata.getParameters().add(parameter));
        return value;
    }

    /**
     * Adds issue link to current test if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #link(String, String, String)}.
     *
     * @param name the name of link.
     * @param url  the link's url.
     */
    public static void issue(final String name, final String url) {
        link(name, ISSUE_LINK_TYPE, url);
    }

    /**
     * Adds tms link to current test if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #link(String, String, String)}.
     *
     * @param name the name of link.
     * @param url  the link's url.
     */
    public static void tms(final String name, final String url) {
        link(name, TMS_LINK_TYPE, url);
    }

    /**
     * Adds link to current test if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #link(String, String)}
     *
     * @param url the link's url.
     */
    public static void link(final String url) {
        link(null, url);
    }

    /**
     * Adds link to current test if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #link(String, String, String)}
     *
     * @param name the name of link.
     * @param url  the link's url.
     */
    public static void link(final String name, final String url) {
        link(name, null, url);
    }

    /**
     * Adds link to current test if any. Takes no effect
     * if no test run at the moment.
     *
     * @param name the name of link.
     * @param type the type of link, used to display link icon in the report.
     * @param url  the link's url.
     */
    public static void link(final String name, final String type, final String url) {
        final Link link = new Link().setName(name).setType(type).setUrl(url);
        getLifecycle().updateTestMetadata(metadata -> metadata.getLinks().add(link));
    }

    /**
     * Adds description to current test if any. Takes no effect
     * if no test run at the moment. Expecting description provided in Markdown format.
     *
     * @param description the description in markdown format.
     * @see #descriptionHtml(String)
     */
    public static void description(final String description) {
        getLifecycle().updateTestMetadata(metadata -> metadata.setDescription(description));
    }

    /**
     * Adds descriptionHtml to current test if any. Takes no effect
     * if no test run at the moment. Note that description will take no effect if descriptionHtml is
     * specified.
     *
     * @param descriptionHtml the description in html format.
     * @see #description(String)
     */
    public static void descriptionHtml(final String descriptionHtml) {
        getLifecycle().updateTestMetadata(metadata -> metadata.setDescriptionHtml(descriptionHtml));
    }

    /**
     * Adds attachment.
     *
     * @param name    the name of attachment.
     * @param content the attachment content.
     */
    public static void attachment(final String name, final String content) {
        addAttachmentAsStep(
                name,
                TEXT_PLAIN,
                content.getBytes(StandardCharsets.UTF_8),
                AttachmentOptions.empty()
        );
    }

    /**
     * Adds attachment.
     *
     * @param name    the name of attachment.
     * @param content the stream that contains attachment content.
     */
    public static void attachment(final String name, final InputStream content) {
        addAttachmentAsStep(name, null, content, AttachmentOptions.empty());
    }

    /**
     * Adds attachment.
     *
     * @param name    the name of attachment.
     * @param type    the content type of attachment.
     * @param content the attachment content.
     */
    public static void attachment(final String name, final String type, final String content) {
        addAttachmentAsStep(
                name,
                type,
                content.getBytes(StandardCharsets.UTF_8),
                AttachmentOptions.empty()
        );
    }

    /**
     * Adds attachment.
     *
     * @param name          the name of attachment.
     * @param type          the content type of attachment.
     * @param content       the attachment content.
     * @param options       the attachment options.
     */
    public static void attachment(final String name, final String type,
                                  final String content, final AttachmentOptions options) {
        addAttachmentAsStep(name, type, content.getBytes(StandardCharsets.UTF_8), options);
    }

    /**
     * Adds attachment.
     *
     * @param name          the name of attachment.
     * @param type          the content type of attachment.
     * @param content       the stream that contains attachment content.
     * @param options       the attachment options.
     */
    public static void attachment(final String name, final String type,
                                  final InputStream content, final AttachmentOptions options) {
        addAttachmentAsStep(name, type, content, options);
    }

    /**
     * Adds an async attachment and waits for its content before the owning executable ends.
     *
     * @param name the name of attachment.
     * @param type the content type of attachment.
     * @param body the future stream that contains attachment content.
     * @return future completed when attachment content is written.
     */
    public static CompletableFuture<Void> attachmentAsync(
                                                          final String name, final String type, final CompletionStage<? extends InputStream> body) {
        return attachmentAsync(name, type, body, AttachmentOptions.empty());
    }

    /**
     * Adds an async attachment and waits for its content before the owning executable ends.
     *
     * @param name          the name of attachment.
     * @param type          the content type of attachment.
     * @param body          the future stream that contains attachment content.
     * @param options       the attachment options.
     * @return future completed when attachment content is written.
     */
    public static CompletableFuture<Void> attachmentAsync(
                                                          final String name, final String type,
                                                          final CompletionStage<? extends InputStream> body,
                                                          final AttachmentOptions options) {
        return getLifecycle().addAttachmentStepAsync(name, type, body, options);
    }

    /**
     * Adds an HTTP exchange attachment.
     *
     * <p>Build the exchange with the desired capture options before calling this method. This method only
     * writes the already captured exchange.</p>
     *
     * @param name the attachment name
     * @param exchange the HTTP exchange payload
     */
    public static void addHttpExchange(final String name, final HttpExchange exchange) {
        addAttachmentAsStep(
                name,
                HttpExchange.CONTENT_TYPE,
                HttpExchangeSerializer.toJsonBytes(exchange),
                AttachmentOptions.empty()
        );
    }

    private static void addAttachmentAsStep(final String name, final String type,
                                            final byte[] body, final AttachmentOptions options) {
        addAttachmentAsStep(name, type, new ByteArrayInputStream(body), options);
    }

    private static void addAttachmentAsStep(final String name, final String type,
                                            final InputStream content, final AttachmentOptions options) {
        getLifecycle().addAttachmentStep(name, type, content, options);
    }

    /**
     * Runnable that allows to throw an exception and return any type.
     *
     * @param <T> the type of return value.
     */
    @FunctionalInterface
    public interface ThrowableRunnable<T> {

        T run() throws Throwable;

    }

    /**
     * Runnable that allows to throw an exception and return void.
     */
    @FunctionalInterface
    public interface ThrowableRunnableVoid {

        void run() throws Throwable;

    }

    /**
     * Runnable that allows to throw an exception and return any type.
     *
     * @param <T> the type of return value.
     * @param <U> the type of context.
     */
    @FunctionalInterface
    public interface ThrowableContextRunnable<T, U> {

        T run(U context) throws Throwable;

    }

    /**
     * Callable that allows to throw an exception and return void.
     *
     * @param <T> the type of context.
     */
    @FunctionalInterface
    public interface ThrowableContextRunnableVoid<T> {

        void run(T context) throws Throwable;

    }

    /**
     * Step context.
     */
    @SuppressWarnings("MultipleStringLiterals")
    public interface StepContext {

        /**
         * Sets step's name.
         *
         * @param name the deserted name of step.
         */
        void name(String name);

        /**
         * Adds parameter to a step.
         *
         * @param name  the name of parameter.
         * @param value the value.
         * @param <T>   the type of value.
         * @return the value.
         */
        <T> T parameter(String name, T value);

        /**
         * Adds parameter to a step.
         *
         * @param name     the name of parameter.
         * @param value    the value.
         * @param excluded true if parameter should be excluded from history key generation, false otherwise.
         * @param <T>      the type of value.
         * @return the value.
         */
        default <T> T parameter(final String name, final T value, final Boolean excluded) {
            throw new UnsupportedOperationException("method is not implemented");
        }

        /**
         * Adds parameter to a step.
         *
         * @param name  the name of parameter.
         * @param value the value.
         * @param mode  the parameter's mode.
         * @param <T>   the type of value.
         * @return the value.
         */
        default <T> T parameter(final String name, final T value, final Parameter.Mode mode) {
            throw new UnsupportedOperationException("method is not implemented");
        }

        /**
         * Adds parameter to a step.
         *
         * @param name     the name of parameter.
         * @param value    the value.
         * @param excluded true if parameter should be excluded from history key generation, false otherwise.
         * @param mode     the parameter's mode.
         * @param <T>      the type of value.
         * @return the value.
         */
        default <T> T parameter(final String name, final T value,
                                final Boolean excluded, final Parameter.Mode mode) {
            throw new UnsupportedOperationException("method is not implemented");
        }

    }

    /**
     * Basic implementation of step context.
     */
    private static final class DefaultStepContext implements StepContext {

        private final AllureExternalKey key;

        private DefaultStepContext(final AllureExternalKey key) {
            this.key = key;
        }

        @Override
        public void name(final String name) {
            getLifecycle().updateStep(key, stepResult -> stepResult.setName(name));
        }

        @Override
        public <T> T parameter(final String name, final T value) {
            return parameter(name, value, null, null);
        }

        @Override
        public <T> T parameter(final String name, final T value, final Boolean excluded) {
            return parameter(name, value, excluded, null);
        }

        @Override
        public <T> T parameter(final String name, final T value, final Parameter.Mode mode) {
            return parameter(name, value, null, mode);
        }

        @Override
        public <T> T parameter(final String name, final T value, final Boolean excluded, final Parameter.Mode mode) {
            final Parameter param = createParameter(name, value, excluded, mode);
            getLifecycle().updateStep(key, stepResult -> stepResult.getParameters().add(param));
            return value;
        }
    }

}
