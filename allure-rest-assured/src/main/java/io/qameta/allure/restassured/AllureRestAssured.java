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
package io.qameta.allure.restassured;

import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import io.qameta.allure.util.ObjectUtils;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.internal.NameAndValue;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static io.qameta.allure.attachment.http.HttpRequestAttachment.Builder.create;
import static java.util.Optional.ofNullable;

/**
 * Allure logger filter for Rest-assured.
 */
public class AllureRestAssured implements OrderedFilter {

    private static final String HIDDEN_PLACEHOLDER = "[ BLACKLISTED ]";

    private int maxAllowedPrettifyLength = 1_048_576;

    private String requestTemplatePath = "http-request.ftl";
    private String responseTemplatePath = "http-response.ftl";
    private String requestAttachmentName = "Request";
    private String responseAttachmentName;

    /**
     * Sets the max allowed prettify length.
     *
     * @param maxAllowedPrettifyLength the max allowed prettify length
     * @return this instance for method chaining
     */
    public AllureRestAssured setMaxAllowedPrettifyLength(final int maxAllowedPrettifyLength) {
        this.maxAllowedPrettifyLength = maxAllowedPrettifyLength;
        return this;
    }

    /**
     * Sets the request template.
     *
     * @param templatePath the classpath path to the FreeMarker template
     * @return this instance for method chaining
     */
    public AllureRestAssured setRequestTemplate(final String templatePath) {
        this.requestTemplatePath = templatePath;
        return this;
    }

    /**
     * Sets the response template.
     *
     * @param templatePath the classpath path to the FreeMarker template
     * @return this instance for method chaining
     */
    public AllureRestAssured setResponseTemplate(final String templatePath) {
        this.responseTemplatePath = templatePath;
        return this;
    }

    /**
     * Sets the request attachment name.
     *
     * @param requestAttachmentName the request attachment name
     * @return this instance for method chaining
     */
    public AllureRestAssured setRequestAttachmentName(final String requestAttachmentName) {
        this.requestAttachmentName = requestAttachmentName;
        return this;
    }

    /**
     * Sets the response attachment name.
     *
     * @param responseAttachmentName the response attachment name
     * @return this instance for method chaining
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Response filter(final FilterableRequestSpecification requestSpec,
                           final FilterableResponseSpecification responseSpec,
                           final FilterContext filterContext) {
        final Prettifier prettifier = new Prettifier();
        final String url = requestSpec.getURI();

        final Set<String> hiddenHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        hiddenHeaders.addAll(Objects.requireNonNull(requestSpec.getConfig().getLogConfig().blacklistedHeaders()));

        final HttpRequestAttachment.Builder requestAttachmentBuilder = create(requestAttachmentName, url)
                .setMethod(requestSpec.getMethod())
                .setHeaders(toMapConverter(requestSpec.getHeaders(), hiddenHeaders))
                .setCookies(toMapConverter(requestSpec.getCookies(), new HashSet<>()));

        if (Objects.nonNull(requestSpec.getBody())) {
            requestAttachmentBuilder.setBody(prettifier.getPrettifiedBodyIfPossible(requestSpec));
        }

        if (Objects.nonNull(requestSpec.getFormParams())) {
            requestAttachmentBuilder.setFormParams(toStringMapConverter(requestSpec.getFormParams()));
        }

        final HttpRequestAttachment requestAttachment = requestAttachmentBuilder.build();

        new DefaultAttachmentProcessor().addAttachment(
                requestAttachment,
                new FreemarkerAttachmentRenderer(requestTemplatePath)
        );

        final Response response = filterContext.next(requestSpec, responseSpec);

        final String attachmentName = ofNullable(responseAttachmentName)
                .orElse(response.getStatusLine());

        final String responseAsString = response.getBody().asString();
        final String body = responseAsString.length() > maxAllowedPrettifyLength
                ? responseAsString
                : prettifier.getPrettifiedBodyIfPossible(response, response.getBody());

        final HttpResponseAttachment responseAttachment = HttpResponseAttachment.Builder.create(attachmentName)
                .setResponseCode(response.getStatusCode())
                .setHeaders(toMapConverter(response.getHeaders(), hiddenHeaders))
                .setBody(body)
                .build();

        new DefaultAttachmentProcessor().addAttachment(
                responseAttachment,
                new FreemarkerAttachmentRenderer(responseTemplatePath)
        );

        return response;
    }

    private static Map<String, String> toMapConverter(final Iterable<? extends NameAndValue> items,
                                                      final Set<String> toHide) {
        final Map<String, String> result = new HashMap<>();
        items.forEach(h -> result.put(h.getName(), toHide.contains(h.getName()) ? HIDDEN_PLACEHOLDER : h.getValue()));
        return result;
    }

    private static Map<String, String> toStringMapConverter(final Map<String, ?> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach((key, value) -> result.put(key, ObjectUtils.toString(value)));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
