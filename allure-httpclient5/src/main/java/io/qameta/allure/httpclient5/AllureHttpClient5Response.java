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

import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.io.entity.BufferedHttpEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Captures Apache HttpClient 5 responses as Allure HTTP exchange attachments.
 *
 * <p>Register this interceptor with {@link AllureHttpClient5Request} to include captured request metadata.</p>
 */
public class AllureHttpClient5Response implements HttpResponseInterceptor {
    private static final String ATTACHMENT_NAME = "HTTP exchange";
    private static final String NO_BODY = "No body present";

    private Consumer<HttpExchange.Builder> exchangeCustomizer = builder -> {
    };

    /**
     * Sets shared HTTP exchange builder customizer.
     *
     * @param exchangeCustomizer the exchange builder customizer
     * @return this instance for method chaining
     */
    public AllureHttpClient5Response configureHttpExchange(final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.exchangeCustomizer = Objects.requireNonNull(exchangeCustomizer);
        return this;
    }

    /**
     * Processes the HTTP response and adds an attachment to the Allure Attachment processor.
     *
     * @param response the HTTP response
     * @param entity   the entity details, may be null for no response body responses
     * @param context  the HTTP context
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("PMD.CloseResource")
    @Override
    public void process(final HttpResponse response,
                        final EntityDetails entity,
                        final HttpContext context)
            throws IOException {
        final HttpExchangeResponse.Builder builder = HttpExchangeResponse.builder()
                .setStatus(response.getCode())
                .setStatusText(response.getReasonPhrase());

        Stream.of(response.getHeaders()).forEach(header -> builder.addHeader(header.getName(), header.getValue()));

        final HttpEntity originalHttpEntity = (HttpEntity) entity;
        if (originalHttpEntity != null) {
            HttpEntity capturedEntity = originalHttpEntity;
            if (!originalHttpEntity.isRepeatable()) {
                capturedEntity = new BufferedHttpEntity(originalHttpEntity);
                final BasicClassicHttpResponse responseEntity = (BasicClassicHttpResponse) context
                        .getAttribute("http.response");
                responseEntity.setEntity(capturedEntity);
            }

            final String responseBody = AllureHttpEntityUtils.getBody(capturedEntity);
            if (responseBody == null || responseBody.isEmpty()) {
                builder.setBody(HttpExchangeBody.utf8(NO_BODY));
            } else {
                builder.setBody(body(capturedEntity.getContentType(), responseBody));
            }
        } else {
            builder.setBody(HttpExchangeBody.utf8(NO_BODY));
        }

        Allure.addHttpExchange(
                ATTACHMENT_NAME,
                exchangeBuilder(request(context))
                        .setResponse(builder.build())
                        .setStart(start(context))
                        .setStop(System.currentTimeMillis())
                        .build()
        );
    }

    private HttpExchange.Builder exchangeBuilder(final HttpExchangeRequest request) {
        final HttpExchange.Builder builder = HttpExchange.builder(request);
        exchangeCustomizer.accept(builder);
        return builder;
    }

    private static HttpExchangeRequest request(final HttpContext context) {
        final Object captured = context == null
                ? null
                : context.getAttribute(AllureHttpClient5Request.REQUEST_CONTEXT_KEY);
        if (captured instanceof HttpExchangeRequest request) {
            return request;
        }

        final Object value = context == null ? null : context.getAttribute("http.request");
        if (value instanceof HttpRequest request) {
            return HttpExchangeRequest.builder(request.getMethod(), request.getRequestUri()).build();
        }
        return HttpExchangeRequest.builder("GET", "unknown").build();
    }

    private static Long start(final HttpContext context) {
        final Object value = context == null ? null : context.getAttribute(AllureHttpClient5Request.START_CONTEXT_KEY);
        return value instanceof Long ? (Long) value : null;
    }

    private static HttpExchangeBody body(final String contentType, final String value) {
        return new HttpExchangeBody(
                contentType,
                "utf8",
                value,
                null,
                null,
                null,
                null,
                null
        );
    }

}
