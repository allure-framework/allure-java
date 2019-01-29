package io.qameta.allure.attachment;

import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.test.AllureFeatures;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.attachment.testdata.TestData.randomHttpRequestAttachment;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class FreemarkerAttachmentRendererTest {

    @AllureFeatures.Attachments
    @Test
    void shouldRenderRequestAttachment() {
        final HttpRequestAttachment data = randomHttpRequestAttachment();
        final DefaultAttachmentContent content = new FreemarkerAttachmentRenderer("http-request.ftl")
                .render(data);

        assertThat(content)
                .hasFieldOrPropertyWithValue("contentType", "text/html")
                .hasFieldOrPropertyWithValue("fileExtension", ".html")
                .hasFieldOrProperty("content");
    }

    @AllureFeatures.Attachments
    @Test
    void shouldRenderResponseAttachment() {
        final HttpRequestAttachment data = randomHttpRequestAttachment();
        final DefaultAttachmentContent content = new FreemarkerAttachmentRenderer("http-response.ftl")
                .render(data);

        assertThat(content)
                .hasFieldOrPropertyWithValue("contentType", "text/html")
                .hasFieldOrPropertyWithValue("fileExtension", ".html")
                .hasFieldOrProperty("content");
    }
}