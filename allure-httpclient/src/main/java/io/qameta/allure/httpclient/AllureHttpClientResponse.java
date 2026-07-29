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

import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Captures Apache HttpClient 4 responses as Allure HTTP exchange attachments.
 *
 * <p>Register this interceptor with {@link AllureHttpClientRequest} to include captured request metadata.</p>
 */
public class AllureHttpClientResponse implements HttpResponseInterceptor {

    private static final String ATTACHMENT_NAME = "HTTP exchange";

    private Consumer<HttpExchange.Builder> exchangeCustomizer = builder -> {
    };

    /**
     * Sets shared HTTP exchange builder customizer.
     *
     * @param exchangeCustomizer the exchange builder customizer
     * @return this instance for method chaining
     */
    public AllureHttpClientResponse configureHttpExchange(final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.exchangeCustomizer = Objects.requireNonNull(exchangeCustomizer);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final HttpResponse response,
                        final HttpContext context)
            throws IOException {
        // enrichment-only integration: silently skip when no executable is running,
        // so a disabled Allure reporter produces no warnings and no entity buffering
        if (Allure.getLifecycle().getCurrentExecutableKey().isEmpty()) {
            return;
        }

        final HttpExchangeResponse.Builder builder = HttpExchangeResponse.builder()
                .setStatus(response.getStatusLine().getStatusCode())
                .setStatusText(response.getStatusLine().getReasonPhrase());

        Stream.of(response.getAllHeaders())
                .forEach(header -> builder.addHeader(header.getName(), header.getValue()));

        if (response.getEntity() != null) {
            if (!response.getEntity().isRepeatable()) {
                final BufferedHttpEntity bufferedEntity = new BufferedHttpEntity(response.getEntity());
                response.setEntity(bufferedEntity);
            }

            builder.setBody(body(response.getEntity().getContentType(), EntityUtils.toString(response.getEntity())));
        } else {
            builder.setBody(HttpExchangeBody.utf8("No body present"));
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
                : context.getAttribute(AllureHttpClientRequest.REQUEST_CONTEXT_KEY);
        if (captured instanceof HttpExchangeRequest request) {
            return request;
        }

        final Object value = context == null ? null : context.getAttribute("http.request");
        if (value instanceof HttpRequest request) {
            return HttpExchangeRequest.builder(request.getRequestLine().getMethod(), request.getRequestLine().getUri())
                    .build();
        }
        return HttpExchangeRequest.builder("GET", "unknown").build();
    }

    private static Long start(final HttpContext context) {
        final Object value = context == null ? null : context.getAttribute(AllureHttpClientRequest.START_CONTEXT_KEY);
        return value instanceof Long ? (Long) value : null;
    }

    private static HttpExchangeBody body(final Header contentType, final String value) {
        return new HttpExchangeBody(
                contentType == null ? null : contentType.getValue(),
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
