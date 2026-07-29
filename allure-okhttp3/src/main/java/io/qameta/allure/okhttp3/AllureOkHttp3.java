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
package io.qameta.allure.okhttp3;

import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeError;
import io.qameta.allure.http.HttpExchangeNameValue;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Allure interceptor logger for OkHttp.
 */
public class AllureOkHttp3 implements Interceptor {

    private static final String ATTACHMENT_NAME = "HTTP exchange";

    private Consumer<HttpExchange.Builder> exchangeCustomizer = builder -> {
    };

    /**
     * Sets shared HTTP exchange builder customizer.
     *
     * @param exchangeCustomizer the exchange builder customizer
     * @return this instance for method chaining
     */
    public AllureOkHttp3 configureHttpExchange(final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.exchangeCustomizer = Objects.requireNonNull(exchangeCustomizer);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response intercept(final Chain chain) throws IOException {
        // enrichment-only integration: pass the call through untouched when no executable is
        // running — no warnings, no request/response body buffering
        if (Allure.getLifecycle().getCurrentExecutableKey().isEmpty()) {
            return chain.proceed(chain.request());
        }
        final long start = System.currentTimeMillis();
        final Request request = chain.request();
        final HttpExchangeRequest.Builder requestBuilder = HttpExchangeRequest
                .builder(request.method(), request.url().toString())
                .addHeaders(toNameValues(request.headers().toMultimap()));

        final RequestBody requestBody = request.body();
        if (Objects.nonNull(requestBody)) {
            requestBuilder.setBody(body(requestBody.contentType(), readRequestBody(requestBody)));
        }

        try {
            final Response response = chain.proceed(request);
            final HttpExchangeResponse.Builder responseBuilder = HttpExchangeResponse.builder()
                    .setStatus(response.code())
                    .setStatusText(response.message())
                    .addHeaders(toNameValues(response.headers().toMultimap()));

            final Response.Builder okHttpResponseBuilder = response.newBuilder();
            final ResponseBody responseBody = response.body();

            if (Objects.nonNull(responseBody)) {
                final byte[] bytes = responseBody.bytes();
                responseBuilder.setBody(body(responseBody.contentType(), new String(bytes, StandardCharsets.UTF_8)));
                okHttpResponseBuilder.body(ResponseBody.create(responseBody.contentType(), bytes));
            }

            Allure.addHttpExchange(
                    ATTACHMENT_NAME,
                    exchangeBuilder(requestBuilder.build())
                            .setResponse(responseBuilder.build())
                            .setStart(start)
                            .setStop(System.currentTimeMillis())
                            .build()
            );
            return okHttpResponseBuilder.build();
        } catch (IOException e) {
            Allure.addHttpExchange(
                    ATTACHMENT_NAME,
                    exchangeBuilder(requestBuilder.build())
                            .setError(new HttpExchangeError(e.getClass().getName(), e.getMessage(), null))
                            .setStart(start)
                            .setStop(System.currentTimeMillis())
                            .build()
            );
            throw e;
        }
    }

    private HttpExchange.Builder exchangeBuilder(final HttpExchangeRequest request) {
        final HttpExchange.Builder builder = HttpExchange.builder(request);
        exchangeCustomizer.accept(builder);
        return builder;
    }

    private static List<HttpExchangeNameValue> toNameValues(final Map<String, List<String>> items) {
        return items.entrySet().stream()
                .map(item -> new HttpExchangeNameValue(item.getKey(), String.join("; ", item.getValue())))
                .toList();
    }

    private static HttpExchangeBody body(final MediaType mediaType, final String value) {
        return new HttpExchangeBody(
                mediaType == null ? null : mediaType.toString(),
                "utf8",
                value,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static String readRequestBody(final RequestBody requestBody) throws IOException {
        try (Buffer buffer = new Buffer()) {
            requestBody.writeTo(buffer);
            return buffer.readString(StandardCharsets.UTF_8);
        }
    }
}
