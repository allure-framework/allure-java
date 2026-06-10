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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

final class HttpExchangeProcessor {

    private static final String ENCODING_UTF8 = "utf8";

    private final HttpExchangeCaptureOptions options;

    HttpExchangeProcessor(final HttpExchangeCaptureOptions options) {
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Creates an UTF-8 body using this processor's truncation rules.
     *
     * @param contentType the body content type
     * @param value the body value
     * @return HTTP exchange body
     */
    HttpExchangeBody body(final String contentType, final String value) {
        if (value == null) {
            return null;
        }
        final TruncatedValue truncated = truncate(value);
        return new HttpExchangeBody(
                contentType,
                ENCODING_UTF8,
                truncated.value(),
                truncated.size(),
                truncated.truncated() ? true : null,
                null,
                null,
                null
        );
    }

    /**
     * Applies configured redaction and truncation to an exchange.
     *
     * @param exchange the exchange to process
     * @return processed exchange
     */
    HttpExchange process(final HttpExchange exchange) {
        if (exchange == null) {
            return null;
        }
        return new HttpExchange(
                exchange.schemaVersion(),
                request(exchange.request()),
                response(exchange.response()),
                exchange.error(),
                exchange.start(),
                exchange.stop()
        );
    }

    /**
     * Applies configured redaction and truncation to a request.
     *
     * @param request the request to process
     * @return processed request
     */
    HttpExchangeRequest process(final HttpExchangeRequest request) {
        return request(request);
    }

    /**
     * Applies configured redaction and truncation to a response.
     *
     * @param response the response to process
     * @return processed response
     */
    HttpExchangeResponse process(final HttpExchangeResponse response) {
        return response(response);
    }

    private HttpExchangeRequest request(final HttpExchangeRequest request) {
        if (request == null) {
            return null;
        }
        return new HttpExchangeRequest(
                request.method(),
                request.url(),
                request.httpVersion(),
                cookies(request.cookies()),
                nameValues(request.headers(), NameValueKind.HEADER),
                nameValues(request.query(), NameValueKind.QUERY),
                processBody(request.body()),
                nameValues(request.trailers(), NameValueKind.HEADER)
        );
    }

    private HttpExchangeResponse response(final HttpExchangeResponse response) {
        if (response == null) {
            return null;
        }
        return new HttpExchangeResponse(
                response.status(),
                response.statusText(),
                response.httpVersion(),
                cookies(response.cookies()),
                nameValues(response.headers(), NameValueKind.HEADER),
                processBody(response.body()),
                nameValues(response.trailers(), NameValueKind.HEADER),
                informationalResponses(response.informationalResponses())
        );
    }

    private List<HttpExchangeInformationalResponse> informationalResponses(
                                                                           final List<HttpExchangeInformationalResponse> responses) {
        return responses == null
                ? null
                : responses.stream()
                        .map(
                                response -> new HttpExchangeInformationalResponse(
                                        response.status(),
                                        response.statusText(),
                                        nameValues(response.headers(), NameValueKind.HEADER)
                                )
                        )
                        .toList();
    }

    private HttpExchangeBody processBody(final HttpExchangeBody body) {
        if (body == null) {
            return null;
        }
        final TruncatedValue value = truncate(body.value());
        return new HttpExchangeBody(
                body.contentType(),
                body.encoding(),
                value.value(),
                body.size() == null ? value.size() : body.size(),
                truncatedFlag(body.truncated(), value.truncated()),
                nameValues(body.form(), NameValueKind.FORM),
                parts(body.parts()),
                body.stream()
        );
    }

    private List<HttpExchangeBodyPart> parts(final List<HttpExchangeBodyPart> parts) {
        return parts == null
                ? null
                : parts.stream()
                        .map(this::part)
                        .toList();
    }

    private HttpExchangeBodyPart part(final HttpExchangeBodyPart part) {
        final TruncatedValue value = truncate(part.value());
        return new HttpExchangeBodyPart(
                part.name(),
                part.fileName(),
                nameValues(part.headers(), NameValueKind.HEADER),
                part.contentType(),
                part.encoding(),
                value.value(),
                part.size() == null ? value.size() : part.size(),
                truncatedFlag(part.truncated(), value.truncated())
        );
    }

    private List<HttpExchangeCookie> cookies(final List<HttpExchangeCookie> cookies) {
        return cookies == null
                ? null
                : cookies.stream()
                        .map(
                                cookie -> new HttpExchangeCookie(
                                        cookie.name(),
                                        options.isCookieRedacted(cookie.name()) ? HttpExchange.REDACTED_VALUE : cookie.value(),
                                        cookie.path(),
                                        cookie.domain(),
                                        cookie.expires(),
                                        cookie.httpOnly(),
                                        cookie.secure(),
                                        cookie.sameSite()
                                )
                        )
                        .toList();
    }

    private List<HttpExchangeNameValue> nameValues(final List<HttpExchangeNameValue> values,
                                                   final NameValueKind kind) {
        return values == null
                ? null
                : values.stream()
                        .map(
                                value -> new HttpExchangeNameValue(
                                        value.name(),
                                        shouldRedact(kind, value.name()) ? HttpExchange.REDACTED_VALUE : value.value()
                                )
                        )
                        .toList();
    }

    private boolean shouldRedact(final NameValueKind kind, final String name) {
        return switch (kind) {
            case HEADER -> options.isHeaderRedacted(name);
            case QUERY -> options.isQueryParameterRedacted(name);
            case FORM -> options.isFormParameterRedacted(name);
        };
    }

    private static Boolean truncatedFlag(final Boolean existing, final boolean truncated) {
        if (Boolean.TRUE.equals(existing) || truncated) {
            return true;
        }
        return existing;
    }

    private TruncatedValue truncate(final String value) {
        if (value == null) {
            return new TruncatedValue(null, null, false);
        }
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        final long maxBodySize = options.getMaxBodySize();
        if (bytes.length <= maxBodySize) {
            return new TruncatedValue(value, (long) bytes.length, false);
        }

        long size = 0;
        final StringBuilder result = new StringBuilder();
        int offset = 0;
        while (offset < value.length()) {
            final int codePoint = value.codePointAt(offset);
            final int characterSize = utf8Length(codePoint);
            if (size + characterSize > maxBodySize) {
                break;
            }
            result.appendCodePoint(codePoint);
            size += characterSize;
            offset += Character.charCount(codePoint);
        }
        return new TruncatedValue(result.toString(), (long) bytes.length, true);
    }

    private static int utf8Length(final int codePoint) {
        if (codePoint <= 0x7F) {
            return 1;
        }
        if (codePoint <= 0x7FF) {
            return 2;
        }
        if (codePoint <= 0xFFFF) {
            return 3;
        }
        return 4;
    }

    private enum NameValueKind {
        HEADER,
        QUERY,
        FORM
    }

    private record TruncatedValue(String value, Long size, boolean truncated) {
    }
}
