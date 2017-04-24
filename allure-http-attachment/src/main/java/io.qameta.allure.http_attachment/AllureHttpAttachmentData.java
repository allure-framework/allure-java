package io.qameta.allure.http_attachment;

import java.util.Map;

/**
 * Class with data for http attachment.
 */
public class AllureHttpAttachmentData {

    private String requestMethod;
    private String requestUrl;
    private Map<String, String> queryParams;
    private Map<String, String> requestHeaders;
    private Map<String, String> requestCookies;
    private String requestBody;
    private String responseStatus;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private String curl;

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(final String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(final String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(final Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(final Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Map<String, String> getRequestCookies() {
        return requestCookies;
    }

    public void setRequestCookies(final Map<String, String> requestCookies) {
        this.requestCookies = requestCookies;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(final String requestBody) {
        this.requestBody = requestBody;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(final String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(final Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(final String responseBody) {
        this.responseBody = responseBody;
    }

    public String getCurl() {
        return curl;
    }

    public void setCurl(final String curl) {
        this.curl = curl;
    }

    public void addRequestHeaders(final String name, final String value) {
        requestHeaders.put(name, value);
    }
}
