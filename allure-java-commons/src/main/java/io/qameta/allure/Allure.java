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
package io.qameta.allure;

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
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * The class contains some useful methods to work with {@link AllureLifecycle}.
 */
@SuppressWarnings({"PMD.ClassNamingConventions", "PMD.ExcessivePublicCount", "PMD.TooManyMethods"})
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
     * Returns {@link AllureLifecycle} for low lever operations with results.
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
            getLifecycle().updateStep(s -> s
                    .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                    .setStatusDetails(getStatusDetails(throwable).orElse(null)));
            ExceptionUtils.sneakyThrow(throwable);
            return null;
        } finally {
            getLifecycle().stopStep(uuid);
        }
    }

    /**
     * Adds epic label to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #label(String, String)}.
     *
     * @param value the value of label.
     */
    public static void epic(final String value) {
        label(EPIC_LABEL_NAME, value);
    }

    /**
     * Adds feature label to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #label(String, String)}.
     *
     * @param value the value of label.
     */
    public static void feature(final String value) {
        label(FEATURE_LABEL_NAME, value);
    }

    /**
     * Adds story label to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #label(String, String)}.
     *
     * @param value the value of label.
     */
    public static void story(final String value) {
        label(STORY_LABEL_NAME, value);
    }

    /**
     * Adds suite label to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #label(String, String)}.
     *
     * @param value the value of label.
     */
    public static void suite(final String value) {
        label(SUITE_LABEL_NAME, value);
    }

    /**
     * Adds label to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment.
     *
     * @param name  the name of label.
     * @param value the value of label.
     */
    public static void label(final String name, final String value) {
        final Label label = new Label().setName(name).setValue(value);
        getLifecycle().updateTestCase(testResult -> testResult.getLabels().add(label));
    }

    /**
     * Adds parameter to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment.
     *
     * @param name  the name of parameter.
     * @param value the value of parameter.
     */
    public static <T> T parameter(final String name, final T value) {
        final Parameter parameter = createParameter(name, value);
        getLifecycle().updateTestCase(testResult -> testResult.getParameters().add(parameter));
        return value;
    }

    /**
     * Adds issue link to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #link(String, String, String)}.
     *
     * @param name the name of link.
     * @param url  the link's url.
     */
    public static void issue(final String name, final String url) {
        link(name, ISSUE_LINK_TYPE, url);
    }

    /**
     * Adds tms link to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #link(String, String, String)}.
     *
     * @param name the name of link.
     * @param url  the link's url.
     */
    public static void tms(final String name, final String url) {
        link(name, TMS_LINK_TYPE, url);
    }

    /**
     * Adds link to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #link(String, String)}
     *
     * @param url the link's url.
     */
    public static void link(final String url) {
        link("link", url);
    }

    /**
     * Adds link to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Shortcut for {@link #link(String, String, String)}
     *
     * @param name the name of link.
     * @param url  the link's url.
     */
    public static void link(final String name, final String url) {
        link(name, null, url);
    }

    /**
     * Adds link to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment.
     *
     * @param name the name of link.
     * @param type the type of link, used to display link icon in the report.
     * @param url  the link's url.
     */
    public static void link(final String name, final String type, final String url) {
        final Link link = new Link().setName(name).setType(type).setUrl(url);
        getLifecycle().updateTestCase(testResult -> testResult.getLinks().add(link));
    }

    /**
     * Adds description to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Expecting description provided in Markdown format.
     *
     * @param description the description in markdown format.
     * @see #descriptionHtml(String)
     */
    public static void description(final String description) {
        getLifecycle().updateTestCase(executable -> executable.setDescription(description));
    }

    /**
     * Adds descriptionHtml to current test or step (or fixture) if any. Takes no effect
     * if no test run at the moment. Note that description will take no effect if descriptionHtml is
     * specified.
     *
     * @param descriptionHtml the description in html format.
     * @see #description(String)
     */
    public static void descriptionHtml(final String descriptionHtml) {
        getLifecycle().updateTestCase(executable -> executable.setDescriptionHtml(descriptionHtml));
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
     * @deprecated use {@link #label(String, String)} instead.
     */
    @Deprecated
    public static void addLabels(final Label... labels) {
        getLifecycle().updateTestCase(testResult -> testResult.getLabels().addAll(asList(labels)));
    }

    /**
     * @deprecated use {@link #link(String, String, String)} instead.
     */
    @Deprecated
    public static void addLinks(final Link... links) {
        getLifecycle().updateTestCase(testResult -> testResult.getLinks().addAll(asList(links)));
    }

    /**
     * @deprecated use {@link #description(String)} instead.
     */
    @Deprecated
    public static void addDescription(final String description) {
        description(description);
    }

    /**
     * @deprecated use {@link #descriptionHtml(String)} instead.
     */
    @Deprecated
    public static void addDescriptionHtml(final String descriptionHtml) {
        descriptionHtml(descriptionHtml);
    }

    public static void addAttachment(final String name, final String content) {
        getLifecycle().addAttachment(name, TEXT_PLAIN, TXT_EXTENSION, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void addAttachment(final String name, final String type, final String content) {
        getLifecycle().addAttachment(name, type, TXT_EXTENSION, content.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static void addAttachment(final String name, final String type,
                                     final String content, final String fileExtension) {
        getLifecycle().addAttachment(name, type, fileExtension, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void addAttachment(final String name, final InputStream content) {
        getLifecycle().addAttachment(name, null, null, content);
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static void addAttachment(final String name, final String type,
                                     final InputStream content, final String fileExtension) {
        getLifecycle().addAttachment(name, type, fileExtension, content);
    }

    public static CompletableFuture<byte[]> addByteAttachmentAsync(
            final String name, final String type, final Supplier<byte[]> body) {
        return addByteAttachmentAsync(name, type, "", body);
    }

    public static CompletableFuture<byte[]> addByteAttachmentAsync(
            final String name, final String type, final String fileExtension, final Supplier<byte[]> body) {
        final String source = getLifecycle().prepareAttachment(name, type, fileExtension);
        return supplyAsync(body).whenComplete((result, ex) ->
                getLifecycle().writeAttachment(source, new ByteArrayInputStream(result)));
    }

    public static CompletableFuture<InputStream> addStreamAttachmentAsync(
            final String name, final String type, final Supplier<InputStream> body) {
        return addStreamAttachmentAsync(name, type, "", body);
    }

    public static CompletableFuture<InputStream> addStreamAttachmentAsync(
            final String name, final String type, final String fileExtension, final Supplier<InputStream> body) {
        final String source = lifecycle.prepareAttachment(name, type, fileExtension);
        return supplyAsync(body).whenComplete((result, ex) -> lifecycle.writeAttachment(source, result));
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
    public interface StepContext {

        void name(String name);

        <T> T parameter(String name, T value);

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
            final Parameter param = createParameter(name, value);
            getLifecycle().updateStep(uuid, stepResult -> stepResult.getParameters().add(param));
            return value;
        }
    }

}
