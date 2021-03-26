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
