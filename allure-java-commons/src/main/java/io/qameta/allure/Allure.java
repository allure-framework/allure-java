package io.qameta.allure;

import io.qameta.allure.model.Label;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * The class contains some useful methods to work with {@link AllureLifecycle}.
 */
public final class Allure {

    private static final String TXT_EXTENSION = ".txt";
    private static final String TEXT_PLAIN = "text/plain";

    private static AllureLifecycle lifecycle;

    private Allure() {
        throw new IllegalStateException("Do not instance");
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = new AllureLifecycle();
        }
        return lifecycle;
    }

    public static void addLabels(final Label... labels) {
        getLifecycle().updateTestCase(testResult -> testResult.withLabels(labels));
    }

    public static void addLinks(final io.qameta.allure.model.Link... links) {
        getLifecycle().updateTestCase(testResult -> testResult.withLinks(links));
    }

    public static void addDescription(final String description) {
        getLifecycle().updateTestCase(executable -> executable.withDescription(description));
    }

    public static void addDescriptionHtml(final String descriptionHtml) {
        getLifecycle().updateTestCase(executable -> executable.withDescriptionHtml(descriptionHtml));
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

    public static void setLifecycle(final AllureLifecycle lifecycle) {
        Allure.lifecycle = lifecycle;
    }
}
