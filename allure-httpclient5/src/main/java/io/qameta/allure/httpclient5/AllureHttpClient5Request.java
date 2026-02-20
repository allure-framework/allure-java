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
package io.qameta.allure.httpclient5;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.util.stream.Stream;

import static io.qameta.allure.attachment.http.HttpRequestAttachment.Builder.create;

/**
 * @author a-simeshin (Simeshin Artem)
 */
public class AllureHttpClient5Request implements HttpRequestInterceptor {

    private final AttachmentRenderer<AttachmentData> renderer;
    private final AttachmentProcessor<AttachmentData> processor;

    public AllureHttpClient5Request() {
        this("http-request.ftl");
    }

    public AllureHttpClient5Request(final String templateName) {
        this(new FreemarkerAttachmentRenderer(templateName), new DefaultAttachmentProcessor());
    }

    public AllureHttpClient5Request(final AttachmentRenderer<AttachmentData> renderer,
                                    final AttachmentProcessor<AttachmentData> processor) {
        this.renderer = renderer;
        this.processor = processor;
    }

    /**
     * Processes the HTTP request and adds an attachment to the Allure Attachment processor.
     *
     * @param request the HTTP request
     * @param entity  the entity details
     * @param context the HTTP context
     */
    @Override
    public void process(final HttpRequest request,
                        final EntityDetails entity,
                        final HttpContext context) {
        final String attachmentName = getAttachmentName(request);
        final HttpRequestAttachment.Builder builder = create(attachmentName, request.getRequestUri());
        builder.setMethod(request.getMethod());

        Stream.of(request.getHeaders()).forEach(header -> builder.setHeader(header.getName(), header.getValue()));

        if (entity instanceof HttpEntity && ((HttpEntity) entity).isRepeatable() && entity.getContentLength() != 0) {
            builder.setBody(AllureHttpEntityUtils.getBody((HttpEntity) entity));
        }

        processor.addAttachment(builder.build(), renderer);
    }

    private String getAttachmentName(final HttpRequest request) {
        return String.format("Request_%s_%s", request.getMethod(), request.getRequestUri());
    }

}
