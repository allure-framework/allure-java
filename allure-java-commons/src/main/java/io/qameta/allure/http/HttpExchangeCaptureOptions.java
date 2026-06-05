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

import java.util.Set;

final class HttpExchangeCaptureOptions {

    private final Set<String> redactedHeaders;
    private final Set<String> redactedCookies;
    private final Set<String> redactedQueryParameters;
    private final Set<String> redactedFormParameters;
    private final long maxBodySize;

    HttpExchangeCaptureOptions(final Set<String> redactedHeaders,
                               final Set<String> redactedCookies,
                               final Set<String> redactedQueryParameters,
                               final Set<String> redactedFormParameters,
                               final long maxBodySize) {
        this.redactedHeaders = Set.copyOf(redactedHeaders);
        this.redactedCookies = Set.copyOf(redactedCookies);
        this.redactedQueryParameters = Set.copyOf(redactedQueryParameters);
        this.redactedFormParameters = Set.copyOf(redactedFormParameters);
        this.maxBodySize = maxBodySize;
    }

    boolean isHeaderRedacted(final String name) {
        return HttpExchange.contains(redactedHeaders, name);
    }

    boolean isCookieRedacted(final String name) {
        return HttpExchange.contains(redactedCookies, name);
    }

    boolean isQueryParameterRedacted(final String name) {
        return HttpExchange.contains(redactedQueryParameters, name);
    }

    boolean isFormParameterRedacted(final String name) {
        return HttpExchange.contains(redactedFormParameters, name);
    }

    long getMaxBodySize() {
        return maxBodySize;
    }
}
