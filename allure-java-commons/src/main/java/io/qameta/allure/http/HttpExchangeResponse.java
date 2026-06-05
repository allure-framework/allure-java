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

/**
 * HTTP response captured in an exchange attachment.
 *
 * @param status the response status code
 * @param statusText the response status text
 * @param httpVersion the HTTP version
 * @param cookies the response cookies
 * @param headers the response headers
 * @param body the response body
 * @param trailers the response trailers
 * @param informationalResponses the informational responses
 */
public record HttpExchangeResponse(Integer status, String statusText, String httpVersion,
        List<HttpExchangeCookie> cookies, List<HttpExchangeNameValue> headers,
        HttpExchangeBody body, List<HttpExchangeNameValue> trailers,
        List<HttpExchangeInformationalResponse> informationalResponses) {

    public HttpExchangeResponse {
        cookies = copy(cookies);
        headers = copy(headers);
        trailers = copy(trailers);
        informationalResponses = copy(informationalResponses);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static <T> List<T> copy(final List<T> values) {
        return values == null ? null : List.copyOf(values);
    }

    /**
     * Builder for {@link HttpExchangeResponse}.
     */
    public static final class Builder {
        private Integer status;
        private String statusText;
        private String httpVersion;
        private final List<HttpExchangeCookie> cookies = new ArrayList<>();
        private final List<HttpExchangeNameValue> headers = new ArrayList<>();
        private HttpExchangeBody body;
        private final List<HttpExchangeNameValue> trailers = new ArrayList<>();
        private final List<HttpExchangeInformationalResponse> informationalResponses = new ArrayList<>();

        public Builder setStatus(final Integer status) {
            this.status = status;
            return this;
        }

        public Builder setStatusText(final String statusText) {
            this.statusText = statusText;
            return this;
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

        public Builder setBody(final HttpExchangeBody body) {
            this.body = body;
            return this;
        }

        public Builder addTrailer(final String name, final String value) {
            trailers.add(new HttpExchangeNameValue(name, value));
            return this;
        }

        public Builder addInformationalResponse(final HttpExchangeInformationalResponse response) {
            informationalResponses.add(response);
            return this;
        }

        public HttpExchangeResponse build() {
            return new HttpExchangeResponse(
                    status, statusText, httpVersion, nullIfEmpty(cookies), nullIfEmpty(headers),
                    body, nullIfEmpty(trailers), nullIfEmpty(informationalResponses)
            );
        }

        private static <T> List<T> nullIfEmpty(final List<T> values) {
            return values.isEmpty() ? null : values;
        }
    }
}
