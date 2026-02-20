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
package io.qameta.allure.springweb;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allure interceptor for spring rest template.
 */
public class AllureRestTemplate implements ClientHttpRequestInterceptor {

    private String requestTemplatePath = "http-request.ftl";
    private String responseTemplatePath = "http-response.ftl";

    public String getRequestTemplatePath() {
        return requestTemplatePath;
    }

    public String getResponseTemplatePath() {
        return responseTemplatePath;
    }

    public AllureRestTemplate setRequestTemplate(final String templatePath) {
        this.requestTemplatePath = templatePath;
        return this;
    }

    public AllureRestTemplate setResponseTemplate(final String templatePath) {
        this.responseTemplatePath = templatePath;
        return this;
    }

    protected AttachmentRenderer<AttachmentData> getRequestRenderer() {
        return new FreemarkerAttachmentRenderer(getRequestTemplatePath());
    }

    protected AttachmentRenderer<AttachmentData> getResponseRenderer() {
        return new FreemarkerAttachmentRenderer(getResponseTemplatePath());
    }

    protected AttachmentProcessor<AttachmentData> getAttachmentProcessor() {
        return new DefaultAttachmentProcessor();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ClientHttpResponse intercept(@NonNull final HttpRequest request, final byte[] body,
                                        @NonNull final ClientHttpRequestExecution execution) throws IOException {
        final AttachmentProcessor<AttachmentData> processor = getAttachmentProcessor();

        final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment.Builder
                .create("Request", request.getURI().toString())
                .setMethod(request.getMethod().name())
                .setHeaders(toMapConverter(request.getHeaders()));
        if (body.length != 0) {
            requestAttachmentBuilder.setBody(new String(body, StandardCharsets.UTF_8));
        }

        final HttpRequestAttachment requestAttachment = requestAttachmentBuilder.build();
        processor.addAttachment(requestAttachment, getRequestRenderer());

        final ClientHttpResponse clientHttpResponse = execution.execute(request, body);

        final HttpResponseAttachment responseAttachment = HttpResponseAttachment.Builder
                .create("Response")
                .setResponseCode(clientHttpResponse.getStatusCode().value())
                .setHeaders(toMapConverter(clientHttpResponse.getHeaders()))
                .setBody(StreamUtils.copyToString(clientHttpResponse.getBody(), StandardCharsets.UTF_8))
                .build();
        processor.addAttachment(responseAttachment, getResponseRenderer());

        return clientHttpResponse;
    }

    protected static Map<String, String> toMapConverter(final Map<String, List<String>> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach((key, value) -> result.put(key, String.join("; ", value)));
        return result;
    }
}
