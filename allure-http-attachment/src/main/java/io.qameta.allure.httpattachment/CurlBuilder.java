package io.qameta.allure.httpattachment;

import java.util.Map;

/**
 * Curl generation builder.
 */
public class CurlBuilder {

    private static final String END = "' ";
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder curl;

    public CurlBuilder(final String method, final String url) {
        this.curl = new StringBuilder("curl -v ").append("-X ").append(method).append(" '")
                .append(url).append(END);
    }

    public CurlBuilder header(final String key, final String value) {
        curl.append("-H '");
        curl.append(String.format("%s: %s", key, value));
        curl.append(END);
        return this;
    }

    public CurlBuilder header(final Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return this;
        }
        headers.forEach(this::header);
        return this;
    }

    public CurlBuilder cookie(final String key, final String value) {
        curl.append("-b '");
        curl.append(String.format("%s=%s", key, value));
        curl.append(END);
        return this;
    }

    public CurlBuilder cookie(final Map<String, String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return this;
        }
        cookies.forEach(this::cookie);
        return this;
    }

    public CurlBuilder body(final String body) {
        if (body == null || body.isEmpty()) {
            return this;
        }
        curl.append("-d '");
        curl.append(body);
        curl.append(END);
        return this;
    }

    public String toString() {
        return curl.toString();
    }

}
