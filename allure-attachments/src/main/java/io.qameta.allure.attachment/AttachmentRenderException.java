package io.qameta.allure.attachment;

/**
 * @author charlie (Dmitry Baev).
 */
public class AttachmentRenderException extends RuntimeException {

    public AttachmentRenderException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
