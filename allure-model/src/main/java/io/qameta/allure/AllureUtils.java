package io.qameta.allure;

import java.util.UUID;

/**
 * @author charlie (Dmitry Baev).
 */
public final class AllureUtils {

    private AllureUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static String generateTestResultName() {
        return generateTestResultName(UUID.randomUUID().toString());
    }

    public static String generateTestResultName(final String uuid) {
        return uuid + AllureConstants.TEST_RESULT_FILE_SUFFIX;
    }

    public static String generateTestResultContainerName() {
        return generateTestResultContainerName(UUID.randomUUID().toString());
    }

    public static String generateTestResultContainerName(final String uuid) {
        return uuid + AllureConstants.TEST_RESULT_CONTAINER_FILE_SUFFIX;
    }
}
