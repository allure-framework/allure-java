package io.qameta.allure.attachment;

/**
 * @param <T> the type of attachment data
 * @author charlie (Dmitry Baev).
 */
public interface AttachmentRenderer<T extends AttachmentData> {

    AttachmentContent render(T attachmentData) throws AttachmentRenderException;

}
