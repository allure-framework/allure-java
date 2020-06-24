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
package io.qameta.allure.httpclient;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
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

    private static String getAttachmentName(final HttpRequest request) {
        return String.format("Request_%s_%s", request.getRequestLine().getMethod(),
                             request.getRequestLine().getUri());
    }

    @Override
    public void process(final HttpRequest request,
                        final HttpContext context) throws IOException {

        final HttpRequestAttachment.Builder builder = create(getAttachmentName(request),
                                                             request.getRequestLine().getUri())
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
