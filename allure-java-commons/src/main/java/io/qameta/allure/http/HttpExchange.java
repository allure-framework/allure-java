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
package io.qameta.allure.http;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Allure HTTP exchange attachment payload.
 *
 * @param schemaVersion the HTTP exchange schema version
 * @param request the captured request
 * @param response the captured response
 * @param error the captured error
 * @param start the exchange start timestamp
 * @param stop the exchange stop timestamp
 */
public record HttpExchange(int schemaVersion, HttpExchangeRequest request, HttpExchangeResponse response,
        HttpExchangeError error, Long start, Long stop) {

    public static final int SCHEMA_VERSION = 1;
    public static final String CONTENT_TYPE = "application/vnd.allure.http+json";
    public static final String FILE_EXTENSION = ".httpexchange";
    public static final String REDACTED_VALUE = "__ALLURE_REDACTED__";
    public static final long DEFAULT_MAX_BODY_SIZE = 1_048_576L;

    private static final Set<String> DEFAULT_REDACTED_HEADERS = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie"
    );

    public HttpExchange {
        Objects.requireNonNull(request, "request must not be null");
    }

    public HttpExchange(final HttpExchangeRequest request, final HttpExchangeResponse response,
                        final HttpExchangeError error, final Long start, final Long stop) {
        this(SCHEMA_VERSION, request, response, error, start, stop);
    }

    /**
     * Creates an HTTP exchange builder.
     *
     * @return exchange builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an HTTP exchange builder initialized with a request.
     *
     * @param request the captured request
     * @return exchange builder
     */
    public static Builder builder(final HttpExchangeRequest request) {
        return builder().setRequest(request);
    }

    static boolean contains(final Set<String> names, final String name) {
        return name != null && names.contains(normalize(name));
    }

    static String normalize(final String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * Builder for {@link HttpExchange}.
     */
    @SuppressWarnings("PMD.TooManyMethods")
    public static final class Builder {
        private static final String CONFIGURE_MUST_NOT_BE_NULL = "configure must not be null";

        private final Set<String> redactedHeaders = new LinkedHashSet<>(DEFAULT_REDACTED_HEADERS);
        private final Set<String> redactedCookies = new LinkedHashSet<>();
        private final Set<String> redactedQueryParameters = new LinkedHashSet<>();
        private final Set<String> redactedFormParameters = new LinkedHashSet<>();
        private long maxBodySize = DEFAULT_MAX_BODY_SIZE;
        private HttpExchangeRequest request;
        private HttpExchangeResponse response;
        private HttpExchangeError error;
        private Long start;
        private Long stop;

        private Builder() {
        }

        /**
         * Sets a captured request that will be redacted and truncated when the exchange is built.
         *
         * @param request the captured request
         * @return this builder
         */
        public Builder request(final HttpExchangeRequest request) {
            return setRequest(request);
        }

        /**
         * Creates and sets a request with the supplied method and URL.
         *
         * @param method the request method
         * @param url the request URL
         * @return this builder
         */
        public Builder request(final String method, final String url) {
            return request(method, url, builder -> {
            });
        }

        /**
         * Creates, configures, and sets a request with the supplied method and URL.
         *
         * @param method the request method
         * @param url the request URL
         * @param configure the request builder customizer
         * @return this builder
         */
        public Builder request(final String method, final String url,
                               final Consumer<HttpExchangeRequest.Builder> configure) {
            final HttpExchangeRequest.Builder builder = HttpExchangeRequest.builder(method, url);
            Objects.requireNonNull(configure, CONFIGURE_MUST_NOT_BE_NULL).accept(builder);
            return request(builder.build());
        }

        /**
         * Sets a captured request that will be redacted and truncated when the exchange is built.
         *
         * @param request the captured request
         * @return this builder
         */
        public Builder setRequest(final HttpExchangeRequest request) {
            this.request = request;
            return this;
        }

        /**
         * Sets a captured response that will be redacted and truncated when the exchange is built.
         *
         * @param response the captured response
         * @return this builder
         */
        public Builder response(final HttpExchangeResponse response) {
            return setResponse(response);
        }

        /**
         * Creates, configures, and sets a response.
         *
         * @param configure the response builder customizer
         * @return this builder
         */
        public Builder response(final Consumer<HttpExchangeResponse.Builder> configure) {
            final HttpExchangeResponse.Builder builder = HttpExchangeResponse.builder();
            Objects.requireNonNull(configure, CONFIGURE_MUST_NOT_BE_NULL).accept(builder);
            return response(builder.build());
        }

        /**
         * Sets a captured response that will be redacted and truncated when the exchange is built.
         *
         * @param response the captured response
         * @return this builder
         */
        public Builder setResponse(final HttpExchangeResponse response) {
            this.response = response;
            return this;
        }

        public Builder error(final HttpExchangeError error) {
            return setError(error);
        }

        public Builder setError(final HttpExchangeError error) {
            this.error = error;
            return this;
        }

        public Builder start(final Long start) {
            return setStart(start);
        }

        public Builder setStart(final Long start) {
            this.start = start;
            return this;
        }

        public Builder stop(final Long stop) {
            return setStop(stop);
        }

        public Builder setStop(final Long stop) {
            this.stop = stop;
            return this;
        }

        public Builder redactHeader(final String name) {
            add(redactedHeaders, name);
            return this;
        }

        public Builder redactHeaders(final Collection<String> names) {
            addAll(redactedHeaders, names);
            return this;
        }

        public Builder clearRedactedHeaders() {
            redactedHeaders.clear();
            return this;
        }

        public Builder redactCookie(final String name) {
            add(redactedCookies, name);
            return this;
        }

        public Builder redactCookies(final Collection<String> names) {
            addAll(redactedCookies, names);
            return this;
        }

        public Builder redactQueryParameter(final String name) {
            add(redactedQueryParameters, name);
            return this;
        }

        public Builder redactQueryParameters(final Collection<String> names) {
            addAll(redactedQueryParameters, names);
            return this;
        }

        public Builder redactFormParameter(final String name) {
            add(redactedFormParameters, name);
            return this;
        }

        public Builder redactFormParameters(final Collection<String> names) {
            addAll(redactedFormParameters, names);
            return this;
        }

        public Builder setMaxBodySize(final long maxBodySize) {
            if (maxBodySize < 0) {
                throw new IllegalArgumentException("maxBodySize must be greater than or equal to 0");
            }
            this.maxBodySize = maxBodySize;
            return this;
        }

        public HttpExchange build() {
            return new HttpExchangeProcessor(captureOptions()).process(
                    new HttpExchange(request, response, error, start, stop)
            );
        }

        private HttpExchangeCaptureOptions captureOptions() {
            return new HttpExchangeCaptureOptions(
                    redactedHeaders,
                    redactedCookies,
                    redactedQueryParameters,
                    redactedFormParameters,
                    maxBodySize
            );
        }

        private static void addAll(final Set<String> destination, final Collection<String> names) {
            if (names != null) {
                names.forEach(name -> add(destination, name));
            }
        }

        private static void add(final Set<String> destination, final String name) {
            if (Objects.nonNull(name) && !name.isBlank()) {
                destination.add(normalize(name));
            }
        }
    }
}
