/*
 *  Copyright 2016-2026 Qameta Software Inc
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
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.stream.Stream;

import static io.qameta.allure.attachment.http.HttpResponseAttachment.Builder.create;

/**
 * Captures Apache HttpClient 4 responses as Allure attachments.
 *
 * <p>Register an instance as an {@link org.apache.http.HttpResponseInterceptor} on the client. The default constructor uses the standard response template and writer; the explicit constructor accepts custom rendering and processing components.</p>
 */
public class AllureHttpClientResponse implements HttpResponseInterceptor {

    private final AttachmentRenderer<AttachmentData> renderer;
    private final AttachmentProcessor<AttachmentData> processor;

    /**
     * Creates an Allure http client response with default configuration.
     */
    public AllureHttpClientResponse() {
        this(
                new FreemarkerAttachmentRenderer("http-response.ftl"),
                new DefaultAttachmentProcessor()
        );
    }

    /**
     * Creates an Allure http client response with the supplied values.
     *
     * @param renderer the renderer used to turn attachment data into content
     * @param processor the processor used to write rendered attachments
     */
    public AllureHttpClientResponse(final AttachmentRenderer<AttachmentData> renderer,
                                    final AttachmentProcessor<AttachmentData> processor) {
        this.renderer = renderer;
        this.processor = processor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final HttpResponse response,
                        final HttpContext context)
            throws IOException {

        final HttpResponseAttachment.Builder builder = create("Response")
                .setResponseCode(response.getStatusLine().getStatusCode());

        Stream.of(response.getAllHeaders())
                .forEach(header -> builder.setHeader(header.getName(), header.getValue()));

        if (response.getEntity() != null) {
            if (!response.getEntity().isRepeatable()) {
                final BufferedHttpEntity bufferedEntity = new BufferedHttpEntity(response.getEntity());
                response.setEntity(bufferedEntity);
            }

            builder.setBody(EntityUtils.toString(response.getEntity()));
        } else {
            builder.setBody("No body present");
        }

        final HttpResponseAttachment responseAttachment = builder.build();
        processor.addAttachment(responseAttachment, renderer);
    }
}
