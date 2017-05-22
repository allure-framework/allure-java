package io.qameta.allure.httpclient;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
                        final HttpContext context) throws HttpException, IOException {

        final HttpResponseAttachment.Builder builder = create("Response")
                .withResponseCode(response.getStatusLine().getStatusCode());

        Stream.of(response.getAllHeaders())
                .forEach(header -> builder.withHeader(header.getName(), header.getValue()));

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        response.getEntity().writeTo(os);

        final String body = new String(os.toByteArray(), StandardCharsets.UTF_8);
        builder.withBody(body);

        final HttpResponseAttachment responseAttachment = builder.build();
        processor.addAttachment(responseAttachment, renderer);
    }
}
