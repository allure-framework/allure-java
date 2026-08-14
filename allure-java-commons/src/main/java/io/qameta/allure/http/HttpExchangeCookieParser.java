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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class HttpExchangeCookieParser {

    private static final String COOKIE_HEADER = "cookie";
    private static final String SET_COOKIE_HEADER = "set-cookie";
    private static final String COOKIE_SEPARATOR = ";";
    private static final String TOKEN_SEPARATORS = "()<>@,;:\\\"/[]?={}";

    private HttpExchangeCookieParser() {
        throw new IllegalStateException("Utility class");
    }

    static boolean isCookieHeader(final String name) {
        return COOKIE_HEADER.equalsIgnoreCase(name);
    }

    static boolean isSetCookieHeader(final String name) {
        return SET_COOKIE_HEADER.equalsIgnoreCase(name);
    }

    static Optional<List<HttpExchangeCookie>> parseCookieHeader(final String value) {
        final List<HttpExchangeCookie> result = new ArrayList<>();
        for (String part : value.split(COOKIE_SEPARATOR, -1)) {
            final Optional<HttpExchangeCookie> cookie = parseCookiePair(part);
            if (cookie.isEmpty()) {
                return Optional.empty();
            }
            result.add(cookie.orElseThrow());
        }
        return result.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(result));
    }

    static Optional<HttpExchangeCookie> parseSetCookieHeader(final String value) {
        final String[] parts = value.split(COOKIE_SEPARATOR, -1);
        final Optional<HttpExchangeCookie> cookie = parseCookiePair(parts[0]);
        if (cookie.isEmpty()) {
            return Optional.empty();
        }

        final CookieAttributes attributes = new CookieAttributes();
        for (int index = 1; index < parts.length; index++) {
            if (!attributes.add(parts[index])) {
                return Optional.empty();
            }
        }

        return Optional.of(attributes.toCookie(cookie.orElseThrow()));
    }

    private static Optional<HttpExchangeCookie> parseCookiePair(final String value) {
        final int separator = value.indexOf('=');
        if (separator <= 0) {
            return Optional.empty();
        }
        final String name = value.substring(0, separator).trim();
        final String cookieValue = value.substring(separator + 1).trim();
        if (!isToken(name) || !isCookieValue(cookieValue)) {
            return Optional.empty();
        }
        return Optional.of(new HttpExchangeCookie(name, cookieValue));
    }

    private static boolean isToken(final String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            if (character <= ' ' || character >= '\u007f' || TOKEN_SEPARATORS.indexOf(character) >= 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCookieValue(final String value) {
        final boolean quoted = value.length() >= 2 && value.charAt(0) == '"'
                && value.charAt(value.length() - 1) == '"';
        final int start = quoted ? 1 : 0;
        final int end = quoted ? value.length() - 1 : value.length();
        for (int index = start; index < end; index++) {
            if (!isCookieOctet(value.charAt(index))) {
                return false;
            }
        }
        return quoted || value.indexOf('"') < 0;
    }

    private static boolean isCookieOctet(final char character) {
        return character == '!'
                || character >= '#' && character <= '+'
                || character >= '-' && character <= ':'
                || character >= '<' && character <= '['
                || character >= ']' && character <= '~';
    }

    private static final class CookieAttributes {
        private final Set<String> seen = new HashSet<>();
        private String path;
        private String domain;
        private String expires;
        private Boolean httpOnly;
        private Boolean secure;
        private String sameSite;

        boolean add(final String rawAttribute) {
            final String attribute = rawAttribute.trim();
            final int separator = attribute.indexOf('=');
            final String name = (separator < 0 ? attribute : attribute.substring(0, separator)).trim();
            if (!isToken(name)) {
                return false;
            }

            final String normalizedName = name.toLowerCase(Locale.ROOT);
            if (!seen.add(normalizedName)) {
                return false;
            }

            final String value = separator < 0 ? null : attribute.substring(separator + 1).trim();
            return switch (normalizedName) {
                case "path" -> setPath(value);
                case "domain" -> setDomain(value);
                case "expires" -> setExpires(value);
                case "httponly" -> setHttpOnly(value);
                case "secure" -> setSecure(value);
                case "samesite" -> setSameSite(value);
                default -> false;
            };
        }

        HttpExchangeCookie toCookie(final HttpExchangeCookie cookie) {
            return new HttpExchangeCookie(
                    cookie.name(),
                    cookie.value(),
                    path,
                    domain,
                    expires,
                    httpOnly,
                    secure,
                    sameSite
            );
        }

        private boolean setPath(final String value) {
            path = value;
            return value != null;
        }

        private boolean setDomain(final String value) {
            domain = value;
            return value != null;
        }

        private boolean setExpires(final String value) {
            expires = value;
            return value != null;
        }

        private boolean setHttpOnly(final String value) {
            httpOnly = value == null ? true : null;
            return value == null;
        }

        private boolean setSecure(final String value) {
            secure = value == null ? true : null;
            return value == null;
        }

        private boolean setSameSite(final String value) {
            sameSite = value;
            return value != null;
        }
    }
}
