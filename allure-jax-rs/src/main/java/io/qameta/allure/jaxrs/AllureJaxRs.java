/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.jaxrs;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
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

    @SuppressWarnings("unused")
    public AllureJaxRs() {
        this(
                new FreemarkerAttachmentRenderer("http-request.ftl"),
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

    @Override
    public void filter(final ClientRequestContext requestContext) {

        final String requestUrl = requestContext.getUri().toString();
        final Object requestBody = requestContext.getEntity();

        final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment.Builder
                .create("Request", requestUrl)
                .setMethod(requestContext.getMethod())
                .setHeaders(toMapConverter(requestContext.getHeaders()));

        if (Objects.nonNull(requestBody)) {
            requestAttachmentBuilder.setBody(requestBody.toString());
        }

        final HttpRequestAttachment responseAttachment = requestAttachmentBuilder.build();
        processor.addAttachment(responseAttachment, requestRenderer);
    }

    @Override
    public void filter(final ClientRequestContext requestContext,
                       final ClientResponseContext responseContext) throws IOException {

        final HttpResponseAttachment.Builder responseAttachmentBuilder = HttpResponseAttachment.Builder
                .create("Response")
                .setResponseCode(responseContext.getStatus())
                .setHeaders(toMapConverter(requestContext.getHeaders()));

        if (Objects.nonNull(responseContext.getEntityStream())) {
            responseAttachmentBuilder.setBody(getBody(responseContext));
        }

        final HttpResponseAttachment responseAttachment = responseAttachmentBuilder.build();
        processor.addAttachment(responseAttachment, responseRenderer);
    }

    private static Map<String, String> toMapConverter(final MultivaluedMap<String, Object> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }

    private String getBody(final ClientResponseContext responseContext) throws IOException {
        try (InputStream stream = responseContext.getEntityStream();
             ByteArrayOutputStream result = new ByteArrayOutputStream()) {
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
