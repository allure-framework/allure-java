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
package io.qameta.allure.httpclient;

import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeRequest;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

/**
 * Captures Apache HttpClient 4 requests for Allure HTTP exchange attachments.
 *
 * <p>Register this interceptor together with {@link AllureHttpClientResponse} to write a single exchange attachment.</p>
 */
public class AllureHttpClientRequest implements HttpRequestInterceptor {

    static final String REQUEST_CONTEXT_KEY = AllureHttpClientRequest.class.getName() + ".request";
    static final String START_CONTEXT_KEY = AllureHttpClientRequest.class.getName() + ".start";

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final HttpRequest request,
                        final HttpContext context)
            throws IOException {

        final HttpExchangeRequest.Builder builder = HttpExchangeRequest
                .builder(request.getRequestLine().getMethod(), request.getRequestLine().getUri());

        Stream.of(request.getAllHeaders())
                .forEach(header -> builder.addHeader(header.getName(), header.getValue()));

        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();

            if (entity != null) {
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                entity.writeTo(os);

                final String body = new String(os.toByteArray(), StandardCharsets.UTF_8);
                builder.setBody(body(entity.getContentType(), body));
            }
        }
        if (context != null) {
            context.setAttribute(REQUEST_CONTEXT_KEY, builder.build());
            context.setAttribute(START_CONTEXT_KEY, System.currentTimeMillis());
        }
    }

    private static HttpExchangeBody body(final Header contentType, final String value) {
        return new HttpExchangeBody(
                contentType == null ? null : contentType.getValue(),
                "utf8",
                value,
                null,
                null,
                null,
                null,
                null
        );
    }
}
