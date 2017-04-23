package io.qameta.allure.curl;

import java.util.Map;

/**
 * Created by vicdev on 07.04.17.
 */
public class CurlBuilder {

    private StringBuilder curl;

    public CurlBuilder(String method, String url) {
        this.curl = new StringBuilder("curl -v ").append("-X ").append(method).append(" '")
                .append(url).append("' ");
    }

    public CurlBuilder header(String key, String value) {
        curl.append("-H '");
        curl.append(String.format("%s: %s", key, value));
        curl.append("' ");
        return this;
    }

    public CurlBuilder header(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return this;
        }
        headers.forEach(this::header);
        return this;
    }

    public CurlBuilder cookie(String key, String value) {
        curl.append("-b '");
        curl.append(String.format("%s=%s", key, value));
        curl.append("' ");
        return this;
    }

    public CurlBuilder cookie(Map<String, String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return this;
        }
        cookies.forEach(this::cookie);
        return this;
    }

    public CurlBuilder body(String body) {
        if (body == null || body.isEmpty()) {
            return this;
        } else {
            curl.append("-d '");
            curl.append(body);
            curl.append("' ");
            return this;
        }
    }

    public String toString() {
        return curl.toString();
    }

}
