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

import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeNameValue;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import io.qameta.allure.util.ObjectUtils;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.internal.NameAndValue;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * Allure logger filter for Rest-assured.
 */
public class AllureRestAssured implements OrderedFilter {

    private static final String ENCODING_UTF8 = "utf8";

    private int maxAllowedPrettifyLength = 1_048_576;

    private String attachmentName = "HTTP exchange";

    private Consumer<HttpExchange.Builder> exchangeCustomizer = builder -> {
    };

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
     * Sets the HTTP exchange attachment name.
     *
     * @param attachmentName the attachment name
     * @return this instance for method chaining
     */
    public AllureRestAssured setAttachmentName(final String attachmentName) {
        this.attachmentName = attachmentName;
        return this;
    }

    /**
     * Sets shared HTTP exchange builder customizer.
     *
     * @param exchangeCustomizer the exchange builder customizer
     * @return this instance for method chaining
     */
    public AllureRestAssured configureHttpExchange(final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.exchangeCustomizer = Objects.requireNonNull(exchangeCustomizer);
        return this;
    }

    /**
     * Sets the HTTP exchange attachment name.
     *
     * @param requestAttachmentName the attachment name
     * @return this instance for method chaining
     */
    public AllureRestAssured setRequestAttachmentName(final String requestAttachmentName) {
        return setAttachmentName(requestAttachmentName);
    }

    /**
     * Sets the HTTP exchange attachment name.
     *
     * @param responseAttachmentName the attachment name
     * @return this instance for method chaining
     */
    public AllureRestAssured setResponseAttachmentName(final String responseAttachmentName) {
        return setAttachmentName(responseAttachmentName);
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
        final long start = System.currentTimeMillis();

        final Set<String> hiddenHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        hiddenHeaders.addAll(Objects.requireNonNull(requestSpec.getConfig().getLogConfig().blacklistedHeaders()));

        final HttpExchangeRequest.Builder requestBuilder = HttpExchangeRequest.builder(requestSpec.getMethod(), url)
                .addHeaders(toNameValues(requestSpec.getHeaders()));

        toNameValues(requestSpec.getCookies())
                .forEach(cookie -> requestBuilder.addCookie(cookie.name(), cookie.value()));

        toStringNameValues(requestSpec.getQueryParams())
                .forEach(query -> requestBuilder.addQuery(query.name(), query.value()));

        String requestBody = null;
        if (Objects.nonNull(requestSpec.getBody())) {
            requestBody = prettifier.getPrettifiedBodyIfPossible(requestSpec);
        }

        List<HttpExchangeNameValue> formParams = null;
        if (Objects.nonNull(requestSpec.getFormParams())) {
            formParams = toStringNameValues(requestSpec.getFormParams());
        }

        if (requestBody != null || formParams != null) {
            requestBuilder.setBody(
                    new HttpExchangeBody(
                            requestSpec.getContentType(),
                            requestBody == null ? null : ENCODING_UTF8,
                            requestBody,
                            null,
                            null,
                            formParams,
                            null,
                            null
                    )
            );
        }

        final Response response = filterContext.next(requestSpec, responseSpec);

        final String responseAsString = response.getBody().asString();
        final String body = responseAsString.length() > maxAllowedPrettifyLength
                ? responseAsString
                : prettifier.getPrettifiedBodyIfPossible(response, response.getBody());

        final HttpExchangeResponse responseExchange = HttpExchangeResponse.builder()
                .setStatus(response.getStatusCode())
                .setStatusText(response.getStatusLine())
                .addHeaders(toNameValues(response.getHeaders()))
                .setBody(
                        new HttpExchangeBody(
                                response.getContentType(),
                                ENCODING_UTF8,
                                body,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
                .build();

        Allure.addHttpExchange(
                attachmentName,
                exchangeBuilder(requestBuilder.build())
                        .redactHeaders(hiddenHeaders)
                        .setResponse(responseExchange)
                        .setStart(start)
                        .setStop(System.currentTimeMillis())
                        .build()
        );

        return response;
    }

    private HttpExchange.Builder exchangeBuilder(final HttpExchangeRequest request) {
        final HttpExchange.Builder builder = HttpExchange.builder(request);
        exchangeCustomizer.accept(builder);
        return builder;
    }

    private static List<HttpExchangeNameValue> toNameValues(final Iterable<? extends NameAndValue> items) {
        return StreamSupport.stream(items.spliterator(), false)
                .map(item -> new HttpExchangeNameValue(item.getName(), item.getValue()))
                .toList();
    }

    private static List<HttpExchangeNameValue> toStringNameValues(final Map<String, ?> items) {
        return items.entrySet().stream()
                .map(item -> new HttpExchangeNameValue(item.getKey(), toStringValue(item.getValue())))
                .toList();
    }

    private static String toStringValue(final Object value) {
        if (Objects.nonNull(value) && "io.restassured.internal.NoParameterValue".equals(value.getClass().getName())) {
            return "null";
        }
        return ObjectUtils.toString(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
