package io.qameta.allure.exceptions;

/**
 * Signals a AllureOld error. Thrown to indicate problems with AllureOld
 * lifecycle.
 *
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 10.24.13
 *         <p/>
 */
@SuppressWarnings("unused")
public class AllureException extends RuntimeException {

    /**
     * Construct an new exception with message
     *
     * @param message initial message value
     */
    public AllureException(String message) {
        super(message);
    }

    /**
     * Construct an new exception with message and some cause
     *
     * @param message initial message value
     * @param cause   initial cause value
     */
    public AllureException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct an new exception with cause
     *
     * @param cause initial cause value
     */
    public AllureException(Throwable cause) {
        super(cause);
    }
}
