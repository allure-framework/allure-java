package io.qameta.allure.attachment;

/**
 * @param <T> the type of attachment data.
 * @author charlie (Dmitry Baev).
 */
public interface AttachmentProcessor<T extends AttachmentData> {

    void addAttachment(T attachmentData, AttachmentRenderer<T> renderer);

}
