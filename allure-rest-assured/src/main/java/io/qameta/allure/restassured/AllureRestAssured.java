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
package io.qameta.allure.restassured;

import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.internal.NameAndValue;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.qameta.allure.attachment.http.HttpRequestAttachment.Builder.create;
import static io.qameta.allure.attachment.http.HttpResponseAttachment.Builder.create;
import static java.util.Optional.ofNullable;

/**
 * Allure logger filter for Rest-assured.
 */
public class AllureRestAssured implements OrderedFilter {

    private String requestTemplatePath = "http-request.ftl";
    private String responseTemplatePath = "http-response.ftl";
    private String requestAttachmentName = "Request";
    private String responseAttachmentName;

    public AllureRestAssured setRequestTemplate(final String templatePath) {
        this.requestTemplatePath = templatePath;
        return this;
    }

    public AllureRestAssured setResponseTemplate(final String templatePath) {
        this.responseTemplatePath = templatePath;
        return this;
    }

    public AllureRestAssured setRequestAttachmentName(final String requestAttachmentName) {
        this.requestAttachmentName = requestAttachmentName;
        return this;
    }

    public AllureRestAssured setResponseAttachmentName(final String responseAttachmentName) {
        this.responseAttachmentName = responseAttachmentName;
        return this;
    }

    /**
     * @deprecated use {@link #setRequestTemplate(String)} instead.
     * Scheduled for removal in 3.0 release.
     */
    @Deprecated
    public AllureRestAssured withRequestTemplate(final String templatePath) {
        return setRequestTemplate(templatePath);
    }

    /**
     * @deprecated use {@link #setResponseTemplate(String)} instead.
     * Scheduled for removal in 3.0 release.
     */
    @Deprecated
    public AllureRestAssured withResponseTemplate(final String templatePath) {
        return setResponseTemplate(templatePath);
    }

    @Override
    public Response filter(final FilterableRequestSpecification requestSpec,
                           final FilterableResponseSpecification responseSpec,
                           final FilterContext filterContext) {
        final Prettifier prettifier = new Prettifier();
        final String url = requestSpec.getURI();
        final HttpRequestAttachment.Builder requestAttachmentBuilder = create(requestAttachmentName, url)
                .setMethod(requestSpec.getMethod())
                .setHeaders(toMapConverter(requestSpec.getHeaders()))
                .setCookies(toMapConverter(requestSpec.getCookies()));

        if (Objects.nonNull(requestSpec.getBody())) {
            requestAttachmentBuilder.setBody(prettifier.getPrettifiedBodyIfPossible(requestSpec));
        }

        if (Objects.nonNull(requestSpec.getFormParams())) {
            requestAttachmentBuilder.setFormParams(requestSpec.getFormParams());
        }

        final HttpRequestAttachment requestAttachment = requestAttachmentBuilder.build();

        new DefaultAttachmentProcessor().addAttachment(
                requestAttachment,
                new FreemarkerAttachmentRenderer(requestTemplatePath)
        );

        final Response response = filterContext.next(requestSpec, responseSpec);

        final String attachmentName = ofNullable(responseAttachmentName)
                .orElse(response.getStatusLine());

        final HttpResponseAttachment responseAttachment = create(attachmentName)
                .setResponseCode(response.getStatusCode())
                .setHeaders(toMapConverter(response.getHeaders()))
                .setBody(prettifier.getPrettifiedBodyIfPossible(response, response.getBody()))
                .build();

        new DefaultAttachmentProcessor().addAttachment(
                responseAttachment,
                new FreemarkerAttachmentRenderer(responseTemplatePath)
        );

        return response;
    }

    private static Map<String, String> toMapConverter(final Iterable<? extends NameAndValue> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach(h -> result.put(h.getName(), h.getValue()));
        return result;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
