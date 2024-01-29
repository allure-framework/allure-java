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
package io.qameta.allure.httpclient5;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.io.entity.BufferedHttpEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.util.stream.Stream;

import static io.qameta.allure.attachment.http.HttpResponseAttachment.Builder.create;

/**
 * @author a-simeshin (Simeshin Artem)
 */
@SuppressWarnings({
        "checkstyle:ParameterAssignment",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.AvoidReassigningParameters"})
public class AllureHttpClient5Response implements HttpResponseInterceptor {
    private final AttachmentRenderer<AttachmentData> renderer;
    private final AttachmentProcessor<AttachmentData> processor;
    private static final String NO_BODY = "No body present";

    public AllureHttpClient5Response() {
        this("http-response.ftl");
    }

    public AllureHttpClient5Response(final String templateName) {
        this(new FreemarkerAttachmentRenderer(templateName), new DefaultAttachmentProcessor());
    }

    public AllureHttpClient5Response(final AttachmentRenderer<AttachmentData> renderer,
                                     final AttachmentProcessor<AttachmentData> processor) {
        this.renderer = renderer;
        this.processor = processor;
    }

    /**
     * Processes the HTTP response and adds an attachment to the Allure Attachment processor.
     *
     * @param response the HTTP response
     * @param entity   the entity details, may be null for no response body responses
     * @param context  the HTTP context
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void process(HttpResponse response, EntityDetails entity, HttpContext context) throws IOException {
        final HttpResponseAttachment.Builder builder = create("Response");
        builder.setResponseCode(response.getCode());

        Stream.of(response.getHeaders()).forEach(header -> builder.setHeader(header.getName(), header.getValue()));

        final HttpEntity originalHttpEntity = (HttpEntity) entity;
        if (originalHttpEntity != null && !originalHttpEntity.isRepeatable()) {
            // Looks like a bug or completely new logic. It's not enough to replace chaining EntityDetails entity.
            // To read the response body twice, It needs to put in the context also
            entity = new BufferedHttpEntity(originalHttpEntity);
            final BasicClassicHttpResponse responseEntity =
                    (BasicClassicHttpResponse) context.getAttribute("http.response");
            responseEntity.setEntity((HttpEntity) entity);

            final String responseBody = AllureHttpEntityUtils.getBody((HttpEntity) entity);
            if (responseBody == null || responseBody.isEmpty()) {
                builder.setBody(NO_BODY);
            } else {
                builder.setBody(responseBody);
            }
        } else {
            builder.setBody(NO_BODY);
        }

        processor.addAttachment(builder.build(), renderer);
    }

}
