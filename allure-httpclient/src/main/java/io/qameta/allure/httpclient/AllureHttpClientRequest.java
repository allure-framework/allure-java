package io.qameta.allure.httpclient;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static io.qameta.allure.attachment.http.HttpRequestAttachment.Builder.create;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureHttpClientRequest implements HttpRequestInterceptor {

    private final AttachmentRenderer<AttachmentData> renderer;
    private final AttachmentProcessor<AttachmentData> processor;

    public AllureHttpClientRequest() {
        this(new FreemarkerAttachmentRenderer("http-request.ftl"),
                new DefaultAttachmentProcessor()
        );
    }

    public AllureHttpClientRequest(final AttachmentRenderer<AttachmentData> renderer,
                                   final AttachmentProcessor<AttachmentData> processor) {
        this.renderer = renderer;
        this.processor = processor;
    }

    @Override
    public void process(final HttpRequest request,
                        final HttpContext context) throws HttpException, IOException {
        final HttpRequestAttachment.Builder builder = create("Request", request.getRequestLine().getUri())
                .setMethod(request.getRequestLine().getMethod());

        Stream.of(request.getAllHeaders())
                .forEach(header -> builder.setHeader(header.getName(), header.getValue()));

        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();

            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            entity.writeTo(os);

            final String body = new String(os.toByteArray(), StandardCharsets.UTF_8);
            builder.setBody(body);
        }

        final HttpRequestAttachment requestAttachment = builder.build();
        processor.addAttachment(requestAttachment, renderer);
    }
}
