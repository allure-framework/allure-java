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
package io.qameta.allure.httpclient5;

import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeRequest;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.util.stream.Stream;

/**
 * Captures Apache HttpClient 5 requests for Allure HTTP exchange attachments.
 *
 * <p>Register this interceptor together with {@link AllureHttpClient5Response} to write a single exchange attachment.</p>
 */
public class AllureHttpClient5Request implements HttpRequestInterceptor {

    static final String REQUEST_CONTEXT_KEY = AllureHttpClient5Request.class.getName() + ".request";
    static final String START_CONTEXT_KEY = AllureHttpClient5Request.class.getName() + ".start";

    /**
     * Processes the HTTP request and adds an attachment to the Allure Attachment processor.
     *
     * @param request the HTTP request
     * @param entity  the entity details
     * @param context the HTTP context
     */
    @Override
    public void process(final HttpRequest request,
                        final EntityDetails entity,
                        final HttpContext context) {
        // enrichment-only integration: silently skip when no executable is running,
        // so a disabled Allure reporter produces no warnings and no body copying
        if (Allure.getLifecycle().getCurrentExecutableKey().isEmpty()) {
            return;
        }
        final HttpExchangeRequest.Builder builder = HttpExchangeRequest
                .builder(request.getMethod(), request.getRequestUri());

        Stream.of(request.getHeaders()).forEach(header -> builder.addHeader(header.getName(), header.getValue()));

        if (entity instanceof HttpEntity && ((HttpEntity) entity).isRepeatable() && entity.getContentLength() != 0) {
            builder.setBody(body(((HttpEntity) entity).getContentType(), AllureHttpEntityUtils.getBody((HttpEntity) entity)));
        }

        if (context != null) {
            context.setAttribute(REQUEST_CONTEXT_KEY, builder.build());
            context.setAttribute(START_CONTEXT_KEY, System.currentTimeMillis());
        }
    }

    private static HttpExchangeBody body(final String contentType, final String value) {
        return new HttpExchangeBody(
                contentType,
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
