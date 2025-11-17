/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.openfeign;

import feign.Request;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author sbushmelev (Sergei Bushmelev)
 */
public class AllureResponseDecoder implements Decoder {

    private final Decoder decoder;

    /**
     * Creates a new AllureResponseDecoder wrapping the specified decoder.
     *
     * @param decoder the underlying decoder to delegate actual decoding to
     */
    public AllureResponseDecoder(final Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Object decode(final Response response, final Type type) throws IOException {
        final Request request = response.request();

        final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment
                .Builder.create("Request", request.url())
                .setMethod(request.httpMethod().name())
                .setHeaders(headers(request.headers()));

        if (Objects.nonNull(request.body())) {
            final Charset charset = request.charset() == null ? StandardCharsets.UTF_8 : request.charset();
            requestAttachmentBuilder.setBody(new String(request.body(), charset));
        }

        new DefaultAttachmentProcessor().addAttachment(
                requestAttachmentBuilder.build(),
                new FreemarkerAttachmentRenderer("http-request.ftl")
        );

        final HttpResponseAttachment.Builder responseAttachmentBuilder = HttpResponseAttachment
                .Builder.create("Response")
                .setResponseCode(response.status())
                .setHeaders(headers(response.headers()));

        final Response.Builder builder = response.toBuilder();

        if (Objects.nonNull(response.body())) {
            try (InputStream bodyStream = response.body().asInputStream()) {
                final byte[] body = readAllBytes(bodyStream);
                final Charset charset = response.charset() == null ? StandardCharsets.UTF_8 : response.charset();
                responseAttachmentBuilder.setBody(new String(body, charset));
                builder.body(body);
            } catch (IOException e) {
                throw new DecodeException(response.status(), "Failed to read response body", request, e);
            }
        }

        new DefaultAttachmentProcessor().addAttachment(
                responseAttachmentBuilder.build(),
                new FreemarkerAttachmentRenderer("http-response.ftl")
        );

        return decoder.decode(builder.build(), type);
    }

    private byte[] readAllBytes(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int byteRead;
        while ((byteRead = inputStream.read()) != -1) {
            buffer.write(byteRead);
        }
        return buffer.toByteArray();
    }

    private Map<String, String> headers(final Map<String, Collection<String>> headers) {
        if (headers == null) {
            return new HashMap<>();
        } else {
            return headers.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> "Set-Cookie".equalsIgnoreCase(entry.getKey())
                                    ? String.join("\n", entry.getValue())
                                    : String.join(", ", entry.getValue())
                    ));
        }
    }

}
