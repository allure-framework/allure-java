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

import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeNameValue;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Allure interceptor for Spring synchronous HTTP clients such as
 * {@code RestTemplate} and {@code RestClient}.
 * <p>
 * Since this interceptor reads the response body to create an attachment,
 * configure a buffering request factory when the caller also needs to consume
 * the response body after interception.
 */
public class AllureRestTemplate implements ClientHttpRequestInterceptor {

    private static final String ATTACHMENT_NAME = "HTTP exchange";

    private Consumer<HttpExchange.Builder> exchangeCustomizer = builder -> {
    };

    /**
     * Sets shared HTTP exchange builder customizer.
     *
     * @param exchangeCustomizer the exchange builder customizer
     * @return this instance for method chaining
     */
    public AllureRestTemplate configureHttpExchange(final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.exchangeCustomizer = Objects.requireNonNull(exchangeCustomizer);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public ClientHttpResponse intercept(@NonNull final HttpRequest request, final byte[] body,
                                        @NonNull final ClientHttpRequestExecution execution)
            throws IOException {
        // enrichment-only integration: pass the call through untouched when no executable is
        // running — no warnings, no response body copying
        if (Allure.getLifecycle().getCurrentExecutableKey().isEmpty()) {
            return execution.execute(request, body);
        }
        final long start = System.currentTimeMillis();

        final HttpExchangeRequest.Builder requestBuilder = HttpExchangeRequest
                .builder(request.getMethod().name(), request.getURI().toString())
                .addHeaders(toNameValues(request.getHeaders().headerSet()));
        if (body.length != 0) {
            requestBuilder.setBody(HttpExchangeBody.utf8(new String(body, StandardCharsets.UTF_8)));
        }

        final ClientHttpResponse clientHttpResponse = execution.execute(request, body);

        final HttpExchangeResponse response = HttpExchangeResponse.builder()
                .setStatus(clientHttpResponse.getStatusCode().value())
                .setStatusText(clientHttpResponse.getStatusText())
                .addHeaders(toNameValues(clientHttpResponse.getHeaders().headerSet()))
                .setBody(
                        HttpExchangeBody.utf8(
                                StreamUtils.copyToString(clientHttpResponse.getBody(), StandardCharsets.UTF_8)
                        )
                )
                .build();

        Allure.addHttpExchange(
                ATTACHMENT_NAME,
                exchangeBuilder(requestBuilder.build())
                        .setResponse(response)
                        .setStart(start)
                        .setStop(System.currentTimeMillis())
                        .build()
        );

        return clientHttpResponse;
    }

    private HttpExchange.Builder exchangeBuilder(final HttpExchangeRequest request) {
        final HttpExchange.Builder builder = HttpExchange.builder(request);
        exchangeCustomizer.accept(builder);
        return builder;
    }

    /**
     * Converts and returns the map converter.
     *
     * @param items the map entries to convert
     * @return the converted map converter
     */
    protected static List<HttpExchangeNameValue> toNameValues(final Collection<Map.Entry<String, List<String>>> items) {
        return items.stream()
                .map(item -> new HttpExchangeNameValue(item.getKey(), String.join("; ", item.getValue())))
                .toList();
    }
}
