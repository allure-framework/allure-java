package io.qameta.allure.attachment;

/**
 * @author charlie (Dmitry Baev).
 */
public class DefaultAttachmentContent implements AttachmentContent {

    private final String content;

    private final String contentType;

    private final String fileExtension;

    public DefaultAttachmentContent(final String content,
                                    final String contentType,
                                    final String fileExtension) {
        this.content = content;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getFileExtension() {
        return fileExtension;
    }
}
