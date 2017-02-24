package io.qameta.allure;

import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author charlie (Dmitry Baev).
 */
public final class Allure {

    private static AllureLifecycle lifecycle = AllureLifecycle.INSTANCE;

    Allure() {
        throw new IllegalStateException("Do not instance");
    }

    public static void addStep(String name) {
        lifecycle.addStep(new StepResult()
                .withName(name)
                .withStart(System.currentTimeMillis())
                .withStop(System.currentTimeMillis())
                .withStatus(Status.PASSED)
                .withStage(Stage.FINISHED)
        );
    }

    public static void addStep(String name, Status status, StatusDetails statusDetails) {
        lifecycle.addStep(new StepResult()
                .withName(name)
                .withStart(System.currentTimeMillis())
                .withStop(System.currentTimeMillis())
                .withStatus(status)
                .withStatusDetails(statusDetails)
                .withStage(Stage.FINISHED)
        );
    }

    public static void addAttachment(String name, String content) {
        lifecycle.addAttachment(name, "text/plain", ".txt", content.getBytes(StandardCharsets.UTF_8));
    }

    public static void addAttachment(String name, String type, String content) {
        lifecycle.addAttachment(name, type, ".txt", content.getBytes(StandardCharsets.UTF_8));
    }

    public static void addAttachment(String name, String type, String content, String fileExtension) {
        lifecycle.addAttachment(name, type, fileExtension, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void addAttachment(String name, InputStream content) {
        lifecycle.addAttachment(name, null, null, content);
    }

    public static void addAttachment(String name, String type, InputStream content, String fileExtension) {
        lifecycle.addAttachment(name, type, fileExtension, content);
    }

    public static void setLifecycle(AllureLifecycle lifecycle) {
        Allure.lifecycle = lifecycle;
    }
}
