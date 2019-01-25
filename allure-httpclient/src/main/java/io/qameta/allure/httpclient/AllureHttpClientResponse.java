package io.qameta.allure.httpclient;

import io.qameta.allure.attachment.*;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static io.qameta.allure.attachment.http.HttpResponseAttachment.Builder.create;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureHttpClientResponse implements HttpResponseInterceptor {

    private final AttachmentRenderer<AttachmentData> renderer;
    private final AttachmentProcessor<AttachmentData> processor;

    public AllureHttpClientResponse() {
        this(new FreemarkerAttachmentRenderer("http-response.ftl"),
                new DefaultAttachmentProcessor()
        );
    }

    public AllureHttpClientResponse(final AttachmentRenderer<AttachmentData> renderer,
                                    final AttachmentProcessor<AttachmentData> processor) {
        this.renderer = renderer;
        this.processor = processor;
    }

    @Override
    public void process(final HttpResponse response,
                        final HttpContext context) throws IOException {

        final HttpResponseAttachment.Builder builder = create("Response")
                .setResponseCode(response.getStatusLine().getStatusCode());

        Stream.of(response.getAllHeaders())
                .forEach(header -> builder.setHeader(header.getName(), header.getValue()));

        if (response.getEntity() != null) {
            final LoggableEntity loggableEntity = new LoggableEntity(response.getEntity());
            response.setEntity(loggableEntity);

            builder.setBody(loggableEntity.getBody());
        } else {
            builder.setBody("No body present");
        }

        final HttpResponseAttachment responseAttachment = builder.build();
        processor.addAttachment(responseAttachment, renderer);
    }

    /**
     * Required to allow consume httpEntity twice.
     */
    private static class LoggableEntity extends HttpEntityWrapper {

        private final byte[] rawContent;

        LoggableEntity(final HttpEntity wrappedEntity) throws IOException {
            super(wrappedEntity);
            rawContent = EntityUtils.toByteArray(wrappedEntity);
        }

        public String getBody() {
            return new String(rawContent, StandardCharsets.UTF_8);
        }

        @Override
        public InputStream getContent() {
            return new ByteArrayInputStream(rawContent);
        }
    }
}
