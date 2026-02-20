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
package io.qameta.allure.attachment.http;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author charlie (Dmitry Baev).
 */
public class HttpResponseAttachment implements AttachmentData {

    private final String name;

    private final String url;

    private final String body;

    private final int responseCode;

    private final Map<String, String> headers;

    private final Map<String, String> cookies;

    public HttpResponseAttachment(final String name, final String url,
                                  final String body, final int responseCode,
                                  final Map<String, String> headers, final Map<String, String> cookies) {
        this.name = name;
        this.url = url;
        this.body = body;
        this.responseCode = responseCode;
        this.headers = headers;
        this.cookies = cookies;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getBody() {
        return body;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    @Override
    public String toString() {
        return "HttpResponseAttachment("
                + "\n\tname=" + this.name
                + ",\n\turl=" + this.url
                + ",\n\tbody=" + this.body
                + ",\n\tresponseCode=" + this.responseCode
                + ",\n\theaders=" + ObjectUtils.mapToString(this.headers)
                + ",\n\tcookies=" + ObjectUtils.mapToString(this.cookies)
                + "\n)";
    }

    /**
     * Builder for HttpRequestAttachment.
     */
    public static final class Builder {

        private final String name;

        private String url;

        private int responseCode;

        private String body;

        private final Map<String, String> headers = new HashMap<>();

        private final Map<String, String> cookies = new HashMap<>();

        private Builder(final String name) {
            Objects.requireNonNull(name, "Name must not be null value");
            this.name = name;
        }

        public static Builder create(final String attachmentName) {
            return new Builder(attachmentName);
        }

        public Builder setUrl(final String url) {
            Objects.requireNonNull(url, "Url must not be null value");
            this.url = url;
            return this;
        }

        public Builder setResponseCode(final int responseCode) {
            this.responseCode = responseCode;
            return this;
        }

        public Builder setHeader(final String name, final String value) {
            Objects.requireNonNull(name, "Header name must not be null value");
            Objects.requireNonNull(value, "Header value must not be null value");
            this.headers.put(name, value);
            return this;
        }

        public Builder setHeaders(final Map<String, String> headers) {
            Objects.requireNonNull(headers, "Headers must not be null value");
            this.headers.putAll(headers);
            return this;
        }

        public Builder setCookie(final String name, final String value) {
            Objects.requireNonNull(name, "Cookie name must not be null value");
            Objects.requireNonNull(value, "Cookie value must not be null value");
            this.cookies.put(name, value);
            return this;
        }

        public Builder setCookies(final Map<String, String> cookies) {
            Objects.requireNonNull(cookies, "Cookies must not be null value");
            this.cookies.putAll(cookies);
            return this;
        }

        public Builder setBody(final String body) {
            Objects.requireNonNull(body, "Body should not be null value");
            this.body = body;
            return this;
        }

        /**
         * Use setter method instead.
         *
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withUrl(final String url) {
            return setUrl(url);
        }

        /**
         * Use setter method instead.
         *
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withResponseCode(final int responseCode) {
            return setResponseCode(responseCode);
        }

        /**
         * Use setter method instead.
         *
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withHeader(final String name, final String value) {
            return setHeader(name, value);
        }

        /**
         * Use setter method instead.
         *
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withHeaders(final Map<String, String> headers) {
            return setHeaders(headers);
        }

        /**
         * Use setter method instead.
         *
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withCookie(final String name, final String value) {
            return setCookie(name, value);
        }

        /**
         * Use setter method instead.
         *
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withCookies(final Map<String, String> cookies) {
            return setCookies(cookies);
        }

        /**
         * Use setter method instead.
         *
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withBody(final String body) {
            return setBody(body);
        }

        public HttpResponseAttachment build() {
            return new HttpResponseAttachment(name, url, body, responseCode, headers, cookies);
        }
    }
}
