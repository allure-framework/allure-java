package io.qameta.allure;

import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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

    public static void addStep(final String name) {
        lifecycle.addStep(new StepResult()
                .withName(name)
                .withStart(System.currentTimeMillis())
                .withStop(System.currentTimeMillis())
                .withStatus(Status.PASSED)
                .withStage(Stage.FINISHED)
        );
    }

    public static void addStep(final String name, final Status status, final StatusDetails statusDetails) {
        lifecycle.addStep(new StepResult()
                .withName(name)
                .withStart(System.currentTimeMillis())
                .withStop(System.currentTimeMillis())
                .withStatus(status)
                .withStatusDetails(statusDetails)
                .withStage(Stage.FINISHED)
        );
    }

    public static void addAttachment(final String name, final String content) {
        lifecycle.addAttachment(name, TEXT_PLAIN, TXT_EXTENSION, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void addAttachment(final String name, final String type, final String content) {
        lifecycle.addAttachment(name, type, TXT_EXTENSION, content.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static void addAttachment(final String name, final String type,
                                     final String content, final String fileExtension) {
        lifecycle.addAttachment(name, type, fileExtension, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void addAttachment(final String name, final InputStream content) {
        lifecycle.addAttachment(name, null, null, content);
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static void addAttachment(final String name, final String type,
                                     final InputStream content, final String fileExtension) {
        lifecycle.addAttachment(name, type, fileExtension, content);
    }

    public static void setLifecycle(final AllureLifecycle lifecycle) {
        Allure.lifecycle = lifecycle;
    }
}
