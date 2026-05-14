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
package io.qameta.allure.seleniumbidi;

import org.openqa.selenium.bidi.network.BaseParameters;
import org.openqa.selenium.bidi.network.BeforeRequestSent;
import org.openqa.selenium.bidi.network.BytesValue;
import org.openqa.selenium.bidi.network.FetchError;
import org.openqa.selenium.bidi.network.FetchTimingInfo;
import org.openqa.selenium.bidi.network.Header;
import org.openqa.selenium.bidi.network.Initiator;
import org.openqa.selenium.bidi.network.RequestData;
import org.openqa.selenium.bidi.network.ResponseData;
import org.openqa.selenium.bidi.network.ResponseDetails;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
final class BiDiNetworkEvent {

    private final Map<String, Object> values;

    private BiDiNetworkEvent(final Map<String, Object> values) {
        this.values = values;
    }

    static BiDiNetworkEvent beforeRequestSent(final BeforeRequestSent event) {
        final Map<String, Object> values = base("beforeRequestSent", event);
        BiDiLogEvent.putIfNotNull(values, "initiator", initiator(event.getInitiator()));
        return new BiDiNetworkEvent(values);
    }

    static BiDiNetworkEvent fetchError(final FetchError event) {
        final Map<String, Object> values = base("fetchError", event);
        BiDiLogEvent.putIfNotNull(values, "errorText", event.getErrorText());
        return new BiDiNetworkEvent(values);
    }

    static BiDiNetworkEvent responseStarted(final ResponseDetails event) {
        return response("responseStarted", event);
    }

    static BiDiNetworkEvent responseCompleted(final ResponseDetails event) {
        return response("responseCompleted", event);
    }

    static BiDiNetworkEvent authRequired(final ResponseDetails event) {
        return response("authRequired", event);
    }

    static BiDiNetworkEvent of(final String event, final Map<String, Object> values) {
        final Map<String, Object> copy = new LinkedHashMap<>();
        copy.put(BiDiJsonKeys.EVENT, event);
        copy.putAll(values);
        return new BiDiNetworkEvent(copy);
    }

    Map<String, Object> toMap(final HeaderRedactor redactor) {
        return redactor.redact(values);
    }

    private static BiDiNetworkEvent response(final String name, final ResponseDetails event) {
        final Map<String, Object> values = base(name, event);
        BiDiLogEvent.putIfNotNull(values, "response", responseData(event.getResponseData()));
        return new BiDiNetworkEvent(values);
    }

    private static Map<String, Object> responseData(final ResponseData response) {
        if (response == null) {
            return null;
        }
        final Map<String, Object> values = new LinkedHashMap<>();
        BiDiLogEvent.putIfNotNull(values, BiDiJsonKeys.URL, response.getUrl());
        BiDiLogEvent.putIfNotNull(values, "protocol", response.getProtocol());
        values.put("status", response.getStatus());
        BiDiLogEvent.putIfNotNull(values, "statusText", response.getStatusText());
        values.put("fromCache", response.isFromCache());
        values.put(BiDiJsonKeys.HEADERS, headers(response.getHeaders()));
        BiDiLogEvent.putIfNotNull(values, "mimeType", response.getMimeType());
        values.put("bytesReceived", response.getBytesReceived());
        values.put(BiDiJsonKeys.HEADERS_SIZE, response.getHeadersSize());
        values.put("bodySize", response.getBodySize());
        response.getContent().ifPresent(content -> values.put("contentLength", content));
        response.getAuthChallenge().ifPresent(challenge -> {
            final Map<String, Object> authChallenge = new LinkedHashMap<>();
            BiDiLogEvent.putIfNotNull(authChallenge, "scheme", challenge.getScheme());
            BiDiLogEvent.putIfNotNull(authChallenge, "realm", challenge.getRealm());
            values.put("authChallenge", authChallenge);
        });
        return values;
    }

    private static Map<String, Object> base(final String event, final BaseParameters parameters) {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put(BiDiJsonKeys.EVENT, event);
        BiDiLogEvent.putIfNotNull(values, "browsingContextId", parameters.getBrowsingContextId());
        values.put("blocked", parameters.isBlocked());
        BiDiLogEvent.putIfNotNull(values, "navigationId", parameters.getNavigationId());
        values.put("redirectCount", parameters.getRedirectCount());
        BiDiLogEvent.putIfNotNull(values, "request", request(parameters.getRequest()));
        values.put("timestamp", parameters.getTimestamp());
        values.put("intercepts", parameters.getIntercepts());
        return values;
    }

    private static Map<String, Object> request(final RequestData request) {
        if (request == null) {
            return null;
        }
        final Map<String, Object> values = new LinkedHashMap<>();
        BiDiLogEvent.putIfNotNull(values, BiDiJsonKeys.REQUEST_ID, request.getRequestId());
        BiDiLogEvent.putIfNotNull(values, BiDiJsonKeys.URL, request.getUrl());
        BiDiLogEvent.putIfNotNull(values, "method", request.getMethod());
        values.put(BiDiJsonKeys.HEADERS, headers(request.getHeaders()));
        BiDiLogEvent.putIfNotNull(values, BiDiJsonKeys.HEADERS_SIZE, request.getHeadersSize());
        BiDiLogEvent.putIfNotNull(values, "timings", timings(request.getTimings()));
        return values;
    }

    private static List<Map<String, Object>> headers(final List<Header> headers) {
        final List<Map<String, Object>> result = new ArrayList<>();
        if (headers == null) {
            return result;
        }
        headers.forEach(header -> {
            final Map<String, Object> value = new LinkedHashMap<>();
            BiDiLogEvent.putIfNotNull(value, "name", header.getName());
            BiDiLogEvent.putIfNotNull(value, BiDiJsonKeys.VALUE, bytesValue(header.getValue()));
            result.add(value);
        });
        return result;
    }

    private static Map<String, Object> bytesValue(final BytesValue bytesValue) {
        if (bytesValue == null) {
            return null;
        }
        final Map<String, Object> value = new LinkedHashMap<>();
        BiDiLogEvent.putIfNotNull(value, BiDiJsonKeys.TYPE, bytesValue.getType());
        BiDiLogEvent.putIfNotNull(value, BiDiJsonKeys.VALUE, bytesValue.getValue());
        return value;
    }

    private static Map<String, Object> timings(final FetchTimingInfo timings) {
        if (timings == null) {
            return null;
        }
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("timeOrigin", timings.getTimeOrigin());
        values.put("requestTime", timings.getRequestTime());
        values.put("redirectStart", timings.getRedirectStart());
        values.put("redirectEnd", timings.getRedirectEnd());
        values.put("fetchStart", timings.getFetchStart());
        values.put("dnsStart", timings.getDnsStart());
        values.put("dnsEnd", timings.getDnsEnd());
        values.put("connectStart", timings.getConnectStart());
        values.put("connectEnd", timings.getConnectEnd());
        values.put("tlsStart", timings.getTlsStart());
        values.put("requestStart", timings.getRequestStart());
        values.put("responseStart", timings.getResponseStart());
        values.put("responseEnd", timings.getResponseEnd());
        return values;
    }

    private static Map<String, Object> initiator(final Initiator initiator) {
        if (initiator == null) {
            return null;
        }
        final Map<String, Object> values = new LinkedHashMap<>();
        BiDiLogEvent.putIfNotNull(values, BiDiJsonKeys.TYPE, initiator.getType());
        initiator.getColumnNumber().ifPresent(column -> values.put("columnNumber", column));
        initiator.getLineNumber().ifPresent(line -> values.put("lineNumber", line));
        initiator.getRequestId().ifPresent(requestId -> values.put(BiDiJsonKeys.REQUEST_ID, requestId));
        initiator.getStackTrace().ifPresent(
                stackTrace -> values.put(
                        "stackTrace",
                        BiDiLogEvent.stackTrace(stackTrace)
                )
        );
        return values;
    }
}
