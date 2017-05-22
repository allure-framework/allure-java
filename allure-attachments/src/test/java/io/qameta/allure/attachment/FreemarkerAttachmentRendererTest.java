package io.qameta.allure.attachment;

import io.qameta.allure.attachment.http.HttpRequestAttachment;
import org.junit.Test;

import static io.qameta.allure.attachment.testdata.TestData.randomHttpRequestAttachment;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class FreemarkerAttachmentRendererTest {

    @Test
    public void shouldRenderRequestAttachment() throws Exception {
        final HttpRequestAttachment data = randomHttpRequestAttachment();
        final DefaultAttachmentContent content = new FreemarkerAttachmentRenderer("http-request.ftl")
                .render(data);

        assertThat(content)
                .hasFieldOrPropertyWithValue("contentType", "text/html")
                .hasFieldOrPropertyWithValue("fileExtension", ".html")
                .hasFieldOrProperty("content");
    }

    @Test
    public void shouldRenderResponseAttachment() throws Exception {
        final HttpRequestAttachment data = randomHttpRequestAttachment();
        final DefaultAttachmentContent content = new FreemarkerAttachmentRenderer("http-response.ftl")
                .render(data);

        assertThat(content)
                .hasFieldOrPropertyWithValue("contentType", "text/html")
                .hasFieldOrPropertyWithValue("fileExtension", ".html")
                .hasFieldOrProperty("content");
    }
}