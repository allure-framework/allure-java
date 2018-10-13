package io.qameta.allure.jaxrs;

import io.qameta.allure.attachment.*;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JAX-RS compatible filter.
 */
public class AllureJaxRs implements ClientRequestFilter, ClientResponseFilter {

    private final AttachmentRenderer<AttachmentData> requestRenderer;
    private final AttachmentRenderer<AttachmentData> responseRenderer;
    private final AttachmentProcessor<AttachmentData> processor;

    public AllureJaxRs() {
        this(new FreemarkerAttachmentRenderer("http-request.ftl"),
             new FreemarkerAttachmentRenderer("http-response.ftl"),
                new DefaultAttachmentProcessor()
        );
    }

    public AllureJaxRs(final AttachmentRenderer<AttachmentData> requestRenderer,
                                   final AttachmentRenderer<AttachmentData> responseRenderer,
                                   final AttachmentProcessor<AttachmentData> processor) {
        this.requestRenderer = requestRenderer;
        this.responseRenderer = responseRenderer;
        this.processor = processor;
    }

    public void filter(final ClientRequestContext requestContext) throws IOException {

        final String requestUrl = requestContext.getUri().toString();
        final Object requestBody = requestContext.getEntity();

        final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment.Builder
                .create("Request", requestUrl).withMethod(requestContext.getMethod())
                .withHeaders(topMapConverter(requestContext.getHeaders()));

        if (Objects.nonNull(requestBody)) {
            requestAttachmentBuilder.withBody(requestBody.toString());
        }

        final HttpRequestAttachment responseAttachment = requestAttachmentBuilder.build();
        processor.addAttachment(responseAttachment, requestRenderer);
    }

    public void filter(final ClientRequestContext requestContext,
                       final ClientResponseContext responseContext) throws IOException {

        final HttpResponseAttachment.Builder responseAttachmentBuilder = HttpResponseAttachment.Builder
                .create("Response").withResponseCode(responseContext.getStatus())
                .withHeaders(topMapConverter(requestContext.getHeaders()));

        if (Objects.nonNull(responseContext.getEntityStream())) {
            responseAttachmentBuilder.withBody(getBody(responseContext));
        }

        final HttpResponseAttachment responseAttachment = responseAttachmentBuilder.build();
        processor.addAttachment(responseAttachment, responseRenderer);
    }

    private static Map<String, String> topMapConverter(final MultivaluedMap<String, Object> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }

    private String getBody(final ClientResponseContext responseContext) throws IOException {
        final InputStream stream = responseContext.getEntityStream();
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[1024];

            int length = stream.read(buffer);
            while (length != -1) {
                result.write(buffer, 0, length);
                length = stream.read(buffer);
            }
            responseContext.setEntityStream(new ByteArrayInputStream(result.toByteArray()));
            return result.toString(StandardCharsets.UTF_8.toString());
        }
    }
}
