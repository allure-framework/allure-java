package io.qameta.allure;

import java.util.Objects;

/**
 * @author charlie (Dmitry Baev).
 */
public class ReadError {

    private final String message;

    private final Throwable exception;

    public ReadError(final Throwable exception, final String message, final Object... args) {
        this.message = Objects.nonNull(args) ? String.format(message, args) : message;
        this.exception = exception;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getException() {
        return exception;
    }
}
