package io.qameta.allure.attachment.http;

import io.qameta.allure.attachment.AttachmentData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author charlie (Dmitry Baev).
 */
public class HttpRequestAttachment implements AttachmentData {

    private final String name;

    private final String url;

    private final String method;

    private final String body;

    private final String curl;

    private final Map<String, String> headers;

    private final Map<String, String> cookies;

    public HttpRequestAttachment(final String name, final String url, final String method,
                                 final String body, final String curl, final Map<String, String> headers,
                                 final Map<String, String> cookies) {
        this.name = name;
        this.url = url;
        this.method = method;
        this.body = body;
        this.curl = curl;
        this.headers = headers;
        this.cookies = cookies;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public String getCurl() {
        return curl;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Builder for HttpRequestAttachment.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static final class Builder {

        private final String name;

        private final String url;

        private String method;

        private String body;

        private final Map<String, String> headers = new HashMap<>();

        private final Map<String, String> cookies = new HashMap<>();

        private Builder(final String name, final String url) {
            Objects.requireNonNull(name, "Name must not be null value");
            Objects.requireNonNull(url, "Url must not be null value");
            this.name = name;
            this.url = url;
        }

        public static Builder create(final String attachmentName, final String url) {
            return new Builder(attachmentName, url);
        }

        public Builder setMethod(final String method) {
            Objects.requireNonNull(method, "Method must not be null value");
            this.method = method;
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
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withMethod(final String method) {
            return setMethod(method);
        }

        /**
         * Use setter method instead.
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withHeader(final String name, final String value) {
            return setHeader(name, value);
        }

        /**
         * Use setter method instead.
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withHeaders(final Map<String, String> headers) {
            return setHeaders(headers);
        }

        /**
         * Use setter method instead.
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withCookie(final String name, final String value) {
            return setCookie(name, value);
        }

        /**
         * Use setter method instead.
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withCookies(final Map<String, String> cookies) {
            return setCookies(cookies);
        }

        /**
         * Use setter method instead.
         * @deprecated scheduled for removal in 3.0 release
         */
        @Deprecated
        public Builder withBody(final String body) {
            return setBody(body);
        }

        public HttpRequestAttachment build() {
            return new HttpRequestAttachment(name, url, method, body, getCurl(), headers, cookies);
        }

        private String getCurl() {
            final StringBuilder builder = new StringBuilder("curl -v");
            if (Objects.nonNull(method)) {
                builder.append(" -X ").append(method);
            }
            builder.append(" '").append(url).append('\'');
            headers.forEach((key, value) -> appendHeader(builder, key, value));
            cookies.forEach((key, value) -> appendCookie(builder, key, value));

            if (Objects.nonNull(body)) {
                builder.append(" -d '").append(body).append('\'');
            }
            return builder.toString();
        }

        private static void appendHeader(final StringBuilder builder, final String key, final String value) {
            builder.append(" -H '")
                    .append(key)
                    .append(": ")
                    .append(value)
                    .append('\'');
        }

        private static void appendCookie(final StringBuilder builder, final String key, final String value) {
            builder.append(" -b '")
                    .append(key)
                    .append('=')
                    .append(value)
                    .append('\'');
        }
    }
}
