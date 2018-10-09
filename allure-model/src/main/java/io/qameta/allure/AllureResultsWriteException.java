package io.qameta.allure;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureResultsWriteException extends RuntimeException {

    public AllureResultsWriteException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
