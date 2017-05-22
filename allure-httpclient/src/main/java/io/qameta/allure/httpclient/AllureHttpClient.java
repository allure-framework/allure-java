package io.qameta.allure.httpclient;

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
public class AllureHttpClient implements HttpRequestInterceptor {

    @Override
    public void process(final HttpRequest request,
                        final HttpContext context) throws HttpException, IOException {
        final HttpRequestAttachment.Builder builder = create("Request", request.getRequestLine().getUri())
                .method(request.getRequestLine().getMethod());

        Stream.of(request.getAllHeaders())
                .forEach(header -> builder.header(header.getName(), header.getValue()));

        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();

            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            entity.writeTo(os);

            final String body = new String(os.toByteArray(), StandardCharsets.UTF_8);
            builder.body(body);
        }

        final HttpRequestAttachment requestAttachment = builder.build();
        new DefaultAttachmentProcessor().addAttachment(
                requestAttachment,
                new FreemarkerAttachmentRenderer("http-request.ftl")
        );
    }
}
