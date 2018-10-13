package io.qameta.allure.jaxrs;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
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

    private String requestTemplatePath = "http-request.ftl";
    private String responseTemplatePath = "http-response.ftl";

    public AllureJaxRs withRequestTemplate(final String templatePath) {
        this.requestTemplatePath = templatePath;
        return this;
    }

    public AllureJaxRs withResponseTemplate(final String templatePath) {
        this.responseTemplatePath = templatePath;
        return this;
    }

    public void filter(final ClientRequestContext requestContext) throws IOException {
        final AttachmentProcessor<AttachmentData> processor = new DefaultAttachmentProcessor();

        final String requestUrl = requestContext.getUri().toString();
        final Object requestBody = requestContext.getEntity();

        final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment.Builder
                .create("Request", requestUrl).withMethod(requestContext.getMethod())
                .withHeaders(topMapConverter(requestContext.getHeaders()));

        if (Objects.nonNull(requestBody)) {
            requestAttachmentBuilder.withBody(requestBody.toString());
        }

        final HttpRequestAttachment responseAttachment = requestAttachmentBuilder.build();
        processor.addAttachment(responseAttachment, new FreemarkerAttachmentRenderer(requestTemplatePath));
    }

    public void filter(final ClientRequestContext requestContext,
                       final ClientResponseContext responseContext) throws IOException {
        final AttachmentProcessor<AttachmentData> processor = new DefaultAttachmentProcessor();

        final InputStream response = responseContext.getEntityStream();

        final HttpResponseAttachment.Builder responseAttachmentBuilder = HttpResponseAttachment.Builder
                .create("Response").withResponseCode(responseContext.getStatus())
                .withHeaders(topMapConverter(requestContext.getHeaders()));

        if (Objects.nonNull(response)) {
            responseAttachmentBuilder.withBody(getBody(response));
        }

        final HttpResponseAttachment responseAttachment = responseAttachmentBuilder.build();
        processor.addAttachment(responseAttachment, new FreemarkerAttachmentRenderer(responseTemplatePath));
    }

    private static Map<String, String> topMapConverter(final MultivaluedMap<String, Object> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }

    private String getBody(final InputStream stream) throws IOException {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[1024];

            int length = stream.read(buffer);
            while (length != -1) {
                result.write(buffer, 0, length);
                length = stream.read(buffer);
            }
            return result.toString(StandardCharsets.UTF_8.toString());
        }
    }
}
