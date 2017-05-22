package io.qameta.allure.attachment;

import io.qameta.allure.Allure;

/**
 * @author charlie (Dmitry Baev).
 */
public class DefaultAttachmentProcessor implements AttachmentProcessor<AttachmentData> {

    @Override
    public void addAttachment(final AttachmentData attachmentData,
                              final AttachmentRenderer<AttachmentData> renderer) {
        final AttachmentContent content = renderer.render(attachmentData);
        Allure.addAttachment(
                attachmentData.getName(),
                content.getContentType(),
                content.getContent(),
                content.getFileExtension()
        );
    }
}
