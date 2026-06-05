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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * HTTP request captured in an exchange attachment.
 *
 * @param method the HTTP method
 * @param url the request URL
 * @param httpVersion the HTTP version
 * @param cookies the request cookies
 * @param headers the request headers
 * @param query the request query values
 * @param body the request body
 * @param trailers the request trailers
 */
public record HttpExchangeRequest(String method, String url, String httpVersion,
        List<HttpExchangeCookie> cookies, List<HttpExchangeNameValue> headers,
        List<HttpExchangeNameValue> query, HttpExchangeBody body,
        List<HttpExchangeNameValue> trailers) {

    public HttpExchangeRequest {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(url, "url must not be null");
        cookies = copy(cookies);
        headers = copy(headers);
        query = copy(query);
        trailers = copy(trailers);
    }

    public static Builder builder(final String method, final String url) {
        return new Builder(method, url);
    }

    private static <T> List<T> copy(final List<T> values) {
        return values == null ? null : List.copyOf(values);
    }

    /**
     * Builder for {@link HttpExchangeRequest}.
     */
    public static final class Builder {
        private final String method;
        private final String url;
        private String httpVersion;
        private final List<HttpExchangeCookie> cookies = new ArrayList<>();
        private final List<HttpExchangeNameValue> headers = new ArrayList<>();
        private final List<HttpExchangeNameValue> query = new ArrayList<>();
        private HttpExchangeBody body;
        private final List<HttpExchangeNameValue> trailers = new ArrayList<>();

        private Builder(final String method, final String url) {
            this.method = method;
            this.url = url;
        }

        public Builder setHttpVersion(final String httpVersion) {
            this.httpVersion = httpVersion;
            return this;
        }

        public Builder addHeader(final String name, final String value) {
            headers.add(new HttpExchangeNameValue(name, value));
            return this;
        }

        public Builder addHeaders(final List<HttpExchangeNameValue> headers) {
            this.headers.addAll(headers);
            return this;
        }

        public Builder addCookie(final String name, final String value) {
            cookies.add(new HttpExchangeCookie(name, value));
            return this;
        }

        public Builder addCookies(final List<HttpExchangeCookie> cookies) {
            this.cookies.addAll(cookies);
            return this;
        }

        public Builder addQuery(final String name, final String value) {
            query.add(new HttpExchangeNameValue(name, value));
            return this;
        }

        public Builder setBody(final HttpExchangeBody body) {
            this.body = body;
            return this;
        }

        public Builder addTrailer(final String name, final String value) {
            trailers.add(new HttpExchangeNameValue(name, value));
            return this;
        }

        public HttpExchangeRequest build() {
            return new HttpExchangeRequest(
                    method, url, httpVersion, nullIfEmpty(cookies), nullIfEmpty(headers),
                    nullIfEmpty(query), body, nullIfEmpty(trailers)
            );
        }

        private static <T> List<T> nullIfEmpty(final List<T> values) {
            return values.isEmpty() ? null : values;
        }
    }
}
