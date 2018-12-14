package io.qameta.allure.util;

/**
 * @author charlie (Dmitry Baev).
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
        throw new IllegalStateException("Do not instance");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void sneakyThrow(final Throwable throwable) throws T {
        throw (T) throwable;
    }
}
