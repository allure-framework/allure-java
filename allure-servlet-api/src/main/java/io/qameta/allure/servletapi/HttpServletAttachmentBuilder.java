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
package io.qameta.allure.servletapi;

import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * Supports Servlet API integration with Allure reporting.
 *
 * <p>Use this type through the module that owns it when translating framework execution,
 * result metadata, or attachments into Allure report data.</p>
 */
public final class HttpServletAttachmentBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServletAttachmentBuilder.class);

    private HttpServletAttachmentBuilder() {
        throw new IllegalStateException();
    }

    /**
     * Builds and returns the request.
     *
     * @param request the request to capture or convert
     * @return the request
     */
    public static HttpExchangeRequest buildRequest(final HttpServletRequest request) {
        final HttpExchangeRequest.Builder requestBuilder = HttpExchangeRequest
                .builder(request.getMethod(), request.getRequestURI());
        Collections.list(request.getHeaderNames())
                .forEach(name -> {
                    final String value = request.getHeader(name);
                    requestBuilder.addHeader(name, value);
                });

        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Arrays.stream(cookies)
                    .forEach(cookie -> requestBuilder.addCookie(cookie.getName(), cookie.getValue()));
        }
        requestBuilder.setBody(HttpExchangeBody.utf8(getBody(request)));
        return requestBuilder.build();
    }

    /**
     * Builds and returns the response.
     *
     * @param response the response to capture or convert
     * @return the response
     */
    public static HttpExchangeResponse buildResponse(final HttpServletResponse response) {
        final HttpExchangeResponse.Builder responseBuilder = HttpExchangeResponse.builder()
                .setStatus(response.getStatus());
        response.getHeaderNames()
                .forEach(
                        name -> response.getHeaders(name)
                                .forEach(value -> responseBuilder.addHeader(name, value))
                );
        return responseBuilder.build();
    }

    /**
     * Returns the body.
     *
     * @param request the request to capture or convert
     * @return the body
     */
    public static String getBody(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            readBody(sb, reader);
        } catch (IOException e) {
            LOGGER.warn("Could not read request body", e);
        }
        return sb.toString();
    }

    /**
     * Handles the read body callback.
     *
     * @param sb the buffer that receives the body content
     * @param reader the reader that provides body content
     * @throws IOException if the underlying framework operation fails
     */
    public static void readBody(final StringBuilder sb,
                                final BufferedReader reader)
            throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
    }
}
