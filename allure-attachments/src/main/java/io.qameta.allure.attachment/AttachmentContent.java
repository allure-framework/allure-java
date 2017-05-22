package io.qameta.allure.attachment;

/**
 * @author charlie (Dmitry Baev).
 */
public interface AttachmentContent {

    String getContent();

    String getContentType();

    String getFileExtension();

}
