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
package io.qameta.allure.jaxrs;

import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeNameValue;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Jakarta RESTful Web Services / JAX-RS compatible filter.
 */
public class AllureJaxRs implements ClientRequestFilter, ClientResponseFilter {

    private static final String ATTACHMENT_NAME = "HTTP exchange";
    private static final String REQUEST_PROPERTY = AllureJaxRs.class.getName() + ".request";
    private static final String START_PROPERTY = AllureJaxRs.class.getName() + ".start";

    private Consumer<HttpExchange.Builder> exchangeCustomizer = builder -> {
    };

    /**
     * Sets shared HTTP exchange builder customizer.
     *
     * @param exchangeCustomizer the exchange builder customizer
     * @return this instance for method chaining
     */
    public AllureJaxRs configureHttpExchange(final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.exchangeCustomizer = Objects.requireNonNull(exchangeCustomizer);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filter(final ClientRequestContext requestContext) {

        final String requestUrl = requestContext.getUri().toString();
        final Object requestBody = requestContext.getEntity();

        final HttpExchangeRequest.Builder requestBuilder = HttpExchangeRequest
                .builder(requestContext.getMethod(), requestUrl)
                .addHeaders(toNameValues(requestContext.getHeaders()));

        if (Objects.nonNull(requestBody)) {
            requestBuilder.setBody(HttpExchangeBody.utf8(requestBody.toString()));
        }

        requestContext.setProperty(REQUEST_PROPERTY, requestBuilder.build());
        requestContext.setProperty(START_PROPERTY, System.currentTimeMillis());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filter(final ClientRequestContext requestContext,
                       final ClientResponseContext responseContext)
            throws IOException {

        final HttpExchangeResponse.Builder responseBuilder = HttpExchangeResponse.builder()
                .setStatus(responseContext.getStatus())
                .setStatusText(responseContext.getStatusInfo().getReasonPhrase())
                .addHeaders(toNameValues(responseContext.getHeaders()));

        if (Objects.nonNull(responseContext.getEntityStream())) {
            responseBuilder.setBody(HttpExchangeBody.utf8(getBody(responseContext)));
        }

        final Object request = requestContext.getProperty(REQUEST_PROPERTY);
        final Object start = requestContext.getProperty(START_PROPERTY);
        Allure.addHttpExchange(
                ATTACHMENT_NAME,
                exchangeBuilder(
                        request instanceof HttpExchangeRequest captured
                                ? captured
                                : HttpExchangeRequest.builder(
                                        requestContext.getMethod(),
                                        requestContext.getUri().toString()
                                ).build()
                )
                        .setResponse(responseBuilder.build())
                        .setStart(start instanceof Long ? (Long) start : null)
                        .setStop(System.currentTimeMillis())
                        .build()
        );
    }

    private HttpExchange.Builder exchangeBuilder(final HttpExchangeRequest request) {
        final HttpExchange.Builder builder = HttpExchange.builder(request);
        exchangeCustomizer.accept(builder);
        return builder;
    }

    private static List<HttpExchangeNameValue> toNameValues(final MultivaluedMap<String, ?> map) {
        return map.entrySet().stream()
                .map(entry -> new HttpExchangeNameValue(entry.getKey(), entry.getValue().toString()))
                .toList();
    }

    private String getBody(final ClientResponseContext responseContext) throws IOException {
        try (InputStream stream = responseContext.getEntityStream()) {
            final byte[] body = stream.readAllBytes();
            responseContext.setEntityStream(new ByteArrayInputStream(body));
            return new String(body, StandardCharsets.UTF_8);
        }
    }
}
