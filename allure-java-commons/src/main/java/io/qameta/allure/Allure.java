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
import io.qameta.allure.listener.LifecycleNotifier;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static io.qameta.allure.util.ResultsUtils.EPIC_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.FEATURE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.ISSUE_LINK_TYPE;
import static io.qameta.allure.util.ResultsUtils.STORY_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.SUITE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TMS_LINK_TYPE;
import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * The class contains some useful methods to work with {@link AllureLifecycle}.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public final class Allure {

    private static final String TXT_EXTENSION = ".txt";
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
     * Sets {@link AllureLifecycle}.
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
        final String uuid = UUID.randomUUID().toString();
        getLifecycle().startStep(uuid, new StepResult().setName(name).setStatus(status));
        getLifecycle().stopStep(uuid);
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
        final String uuid = UUID.randomUUID().toString();
        getLifecycle().startStep(uuid, new StepResult().setName("step"));

        try {
            final T result = runnable.run(new DefaultStepContext(uuid));
            getLifecycle().updateStep(uuid, step -> step.setStatus(Status.PASSED));
            return result;
        } catch (Throwable throwable) {
            getLifecycle().updateStep(
                    s -> s
                            .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                            .setStatusDetails(getStatusDetails(throwable).orElse(null))
            );
            throw ExceptionUtils.sneakyThrow(throwable);
        } finally {
            getLifecycle().stopStep(uuid);
        }
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
        getLifecycle().addLabel(label);
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
        getLifecycle().addParameter(parameter);
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
        getLifecycle().addLink(link);
    }

    /**
     * Adds description to current test if any. Takes no effect
     * if no test run at the moment. Expecting description provided in Markdown format.
     *
     * @param description the description in markdown format.
     * @see #descriptionHtml(String)
     */
    public static void description(final String description) {
        getLifecycle().setDescription(description);
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
        getLifecycle().setDescriptionHtml(descriptionHtml);
    }

    /**
     * Adds attachment.
     *
     * @param name    the name of attachment.
     * @param content the attachment content.
     */
    public static void attachment(final String name, final String content) {
        addAttachment(name, content);
    }

    /**
     * Adds attachment.
     *
     * @param name    the name of attachment.
     * @param content the stream that contains attachment content.
     */
    public static void attachment(final String name, final InputStream content) {
        addAttachment(name, content);
    }

    /**
     * Adds the attachment.
     *
     * @param name the display name or logical name to use
     * @param content the attachment content
     */
    public static void addAttachment(final String name, final String content) {
        addAttachmentAsStep(name, TEXT_PLAIN, TXT_EXTENSION, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Adds the attachment.
     *
     * @param name the display name or logical name to use
     * @param type the event or label type
     * @param content the attachment content
     */
    public static void addAttachment(final String name, final String type, final String content) {
        addAttachmentAsStep(name, type, TXT_EXTENSION, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Adds the attachment.
     *
     * @param name the display name or logical name to use
     * @param type the event or label type
     * @param content the attachment content
     * @param fileExtension the attachment file extension
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static void addAttachment(final String name, final String type,
                                     final String content, final String fileExtension) {
        addAttachmentAsStep(name, type, fileExtension, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Adds the attachment.
     *
     * @param name the display name or logical name to use
     * @param content the attachment content
     */
    public static void addAttachment(final String name, final InputStream content) {
        addAttachmentAsStep(name, null, null, content);
    }

    /**
     * Adds the attachment.
     *
     * @param name the display name or logical name to use
     * @param type the event or label type
     * @param content the attachment content
     * @param fileExtension the attachment file extension
     */
    public static void addAttachment(final String name, final String type,
                                     final InputStream content, final String fileExtension) {
        addAttachmentAsStep(name, type, fileExtension, content);
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
                HttpExchange.FILE_EXTENSION,
                HttpExchangeSerializer.toJsonBytes(exchange)
        );
    }

    /**
     * Adds the byte attachment async.
     *
     * @param name the display name or logical name to use
     * @param type the event or label type
     * @param body the attachment body
     * @return this instance for method chaining
     */
    public static CompletableFuture<byte[]> addByteAttachmentAsync(
                                                                   final String name, final String type, final Supplier<byte[]> body) {
        return addByteAttachmentAsync(name, type, "", body);
    }

    /**
     * Adds the byte attachment async.
     *
     * @param name the display name or logical name to use
     * @param type the event or label type
     * @param fileExtension the attachment file extension
     * @param body the attachment body
     * @return this instance for method chaining
     */
    public static CompletableFuture<byte[]> addByteAttachmentAsync(
                                                                   final String name, final String type, final String fileExtension, final Supplier<byte[]> body) {
        final AllureLifecycle lifecycle = getLifecycle();
        final PreparedAttachment attachment = prepareAttachmentAsStep(lifecycle, name, type, fileExtension);
        return supplyAsync(body).whenComplete((result, ex) -> {
            if (Objects.nonNull(ex)) {
                attachment.fail(ex);
                return;
            }
            try {
                lifecycle.writeAttachment(attachment.source(), new ByteArrayInputStream(result));
            } catch (Throwable throwable) {
                attachment.fail(throwable);
                throw ExceptionUtils.sneakyThrow(throwable);
            }
        });
    }

    /**
     * Adds the stream attachment async.
     *
     * @param name the display name or logical name to use
     * @param type the event or label type
     * @param body the attachment body
     * @return this instance for method chaining
     */
    public static CompletableFuture<InputStream> addStreamAttachmentAsync(
                                                                          final String name, final String type, final Supplier<InputStream> body) {
        return addStreamAttachmentAsync(name, type, "", body);
    }

    /**
     * Adds the stream attachment async.
     *
     * @param name the display name or logical name to use
     * @param type the event or label type
     * @param fileExtension the attachment file extension
     * @param body the attachment body
     * @return this instance for method chaining
     */
    public static CompletableFuture<InputStream> addStreamAttachmentAsync(
                                                                          final String name, final String type, final String fileExtension, final Supplier<InputStream> body) {
        final AllureLifecycle lifecycle = getLifecycle();
        final PreparedAttachment attachment = prepareAttachmentAsStep(lifecycle, name, type, fileExtension);
        return supplyAsync(body).whenComplete((result, ex) -> {
            if (Objects.nonNull(ex)) {
                attachment.fail(ex);
                return;
            }
            try {
                lifecycle.writeAttachment(attachment.source(), result);
            } catch (Throwable throwable) {
                attachment.fail(throwable);
                throw ExceptionUtils.sneakyThrow(throwable);
            }
        });
    }

    private static void addAttachmentAsStep(final String name, final String type,
                                            final String fileExtension, final byte[] body) {
        addAttachmentAsStep(name, type, fileExtension, new ByteArrayInputStream(body));
    }

    private static void addAttachmentAsStep(final String name, final String type,
                                            final String fileExtension, final InputStream content) {
        final AllureLifecycle lifecycle = getLifecycle();
        if (isDirectAttachmentWrite(lifecycle)) {
            lifecycle.addAttachment(name, type, fileExtension, content);
            return;
        }

        final String uuid = UUID.randomUUID().toString();
        lifecycle.startStep(uuid, new StepResult().setName(attachmentStepName(name)));
        try {
            lifecycle.addAttachment(name, type, fileExtension, content);
            lifecycle.updateStep(uuid, step -> step.setStatus(Status.PASSED));
        } catch (Throwable throwable) {
            lifecycle.updateStep(
                    uuid,
                    step -> step
                            .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                            .setStatusDetails(getStatusDetails(throwable).orElse(null))
            );
            throw ExceptionUtils.sneakyThrow(throwable);
        } finally {
            lifecycle.stopStep(uuid);
        }
    }

    private static PreparedAttachment prepareAttachmentAsStep(final AllureLifecycle lifecycle,
                                                              final String name,
                                                              final String type,
                                                              final String fileExtension) {
        if (isDirectAttachmentWrite(lifecycle)) {
            return new PreparedAttachment(
                    lifecycle.prepareAttachment(name, type, fileExtension),
                    null
            );
        }

        final String uuid = UUID.randomUUID().toString();
        final StepResult step = new StepResult()
                .setName(attachmentStepName(name))
                .setStatus(Status.PASSED);
        lifecycle.startStep(uuid, step);
        try {
            return new PreparedAttachment(
                    lifecycle.prepareAttachment(name, type, fileExtension),
                    step
            );
        } catch (Throwable throwable) {
            step.setStatus(getStatus(throwable).orElse(Status.BROKEN))
                    .setStatusDetails(getStatusDetails(throwable).orElse(null));
            throw ExceptionUtils.sneakyThrow(throwable);
        } finally {
            lifecycle.stopStep(uuid);
        }
    }

    private static boolean isDirectAttachmentWrite(final AllureLifecycle lifecycle) {
        return LifecycleNotifier.isListenerCallbackRunning()
                || lifecycle.getCurrentTestCaseOrStep().isEmpty();
    }

    private static String attachmentStepName(final String name) {
        return Objects.isNull(name) || name.isEmpty() ? "Attachment" : name;
    }

    private record PreparedAttachment(String source, StepResult step) {

        void fail(final Throwable throwable) {
            if (Objects.nonNull(step)) {
                step.setStatus(getStatus(throwable).orElse(Status.BROKEN))
                        .setStatusDetails(getStatusDetails(throwable).orElse(null));
            }
        }

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

        private final String uuid;

        private DefaultStepContext(final String uuid) {
            this.uuid = uuid;
        }

        @Override
        public void name(final String name) {
            getLifecycle().updateStep(uuid, stepResult -> stepResult.setName(name));
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
            getLifecycle().updateStep(uuid, stepResult -> stepResult.getParameters().add(param));
            return value;
        }
    }

}
