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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class HeaderRedactor {

    static final String REDACTED = "[REDACTED]";

    private static final Set<String> DEFAULT_HEADERS = new LinkedHashSet<>(Arrays.asList(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key"
    ));

    private final Set<String> headerNames;

    private HeaderRedactor(final Set<String> headerNames) {
        this.headerNames = headerNames;
    }

    static HeaderRedactor defaults() {
        return new HeaderRedactor(DEFAULT_HEADERS);
    }

    HeaderRedactor withAdditionalHeaders(final String... additionalHeaders) {
        final Set<String> headers = new LinkedHashSet<>(headerNames);
        if (additionalHeaders != null) {
            Arrays.stream(additionalHeaders)
                    .map(HeaderRedactor::normalize)
                    .filter(value -> !value.isEmpty())
                    .forEach(headers::add);
        }
        return new HeaderRedactor(headers);
    }

    Map<String, Object> redact(final Map<String, Object> values) {
        return redactMap(values);
    }

    private Map<String, Object> redactMap(final Map<String, Object> values) {
        final Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (BiDiJsonKeys.HEADERS.equals(key) && value instanceof List) {
                result.put(key, redactHeaders((List<?>) value));
            } else if (value instanceof Map) {
                result.put(key, redactMap(castMap(value)));
            } else if (value instanceof List) {
                result.put(key, redactList((List<?>) value));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    private List<Object> redactList(final List<?> values) {
        final List<Object> result = new ArrayList<>();
        values.forEach(value -> {
            if (value instanceof Map) {
                result.add(redactMap(castMap(value)));
            } else if (value instanceof List) {
                result.add(redactList((List<?>) value));
            } else {
                result.add(value);
            }
        });
        return result;
    }

    private List<Object> redactHeaders(final List<?> headers) {
        return headers.stream()
                .map(header -> header instanceof Map ? redactHeader(castMap(header)) : header)
                .collect(Collectors.toList());
    }

    private Map<String, Object> redactHeader(final Map<String, Object> header) {
        final Map<String, Object> result = new LinkedHashMap<>(header);
        final Object name = result.get("name");
        if (name instanceof String && headerNames.contains(normalize((String) name))) {
            result.put(BiDiJsonKeys.VALUE, redactedValue(result.get(BiDiJsonKeys.VALUE)));
        }
        return result;
    }

    private Object redactedValue(final Object value) {
        if (value instanceof Map) {
            final Map<String, Object> redacted = new LinkedHashMap<>(castMap(value));
            redacted.put(BiDiJsonKeys.VALUE, REDACTED);
            return redacted;
        }
        return REDACTED;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(final Object value) {
        return (Map<String, Object>) value;
    }

    private static String normalize(final String headerName) {
        return headerName == null ? "" : headerName.toLowerCase(Locale.ROOT);
    }
}
