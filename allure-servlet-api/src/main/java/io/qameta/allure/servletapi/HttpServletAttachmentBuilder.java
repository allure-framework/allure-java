/*
 *  Copyright 2016-2024 Qameta Software Inc
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

import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import static io.qameta.allure.attachment.http.HttpRequestAttachment.Builder.create;
import static io.qameta.allure.attachment.http.HttpResponseAttachment.Builder.create;

/**
 * @author charlie (Dmitry Baev).
 */
public final class HttpServletAttachmentBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServletAttachmentBuilder.class);

    private HttpServletAttachmentBuilder() {
        throw new IllegalStateException();
    }

    public static HttpRequestAttachment buildRequest(final HttpServletRequest request) {
        final HttpRequestAttachment.Builder requestBuilder = create("Request", request.getRequestURI());
        Collections.list(request.getHeaderNames())
                .forEach(name -> {
                    final String value = request.getHeader(name);
                    requestBuilder.setHeader(name, value);
                });

        Stream.of(request.getCookies())
                .forEach(cookie -> requestBuilder.setCookie(cookie.getName(), cookie.getValue()));
        requestBuilder.setBody(getBody(request));
        return requestBuilder.build();
    }

    public static HttpResponseAttachment buildResponse(final HttpServletResponse response) {
        final HttpResponseAttachment.Builder responseBuilder = create("Response");
        response.getHeaderNames()
                .forEach(name -> response.getHeaders(name)
                        .forEach(value -> responseBuilder.setHeader(name, value)));
        return responseBuilder.build();
    }

    public static String getBody(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            readBody(sb, reader);
        } catch (IOException e) {
            LOGGER.warn("Could not read request body", e);
        }
        return sb.toString();
    }

    public static void readBody(final StringBuilder sb,
                                final BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
    }
}
