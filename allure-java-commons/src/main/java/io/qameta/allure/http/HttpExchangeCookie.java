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

import java.util.Objects;

/**
 * HTTP cookie captured in an exchange attachment.
 *
 * @param name the cookie name
 * @param value the cookie value
 * @param path the cookie path
 * @param domain the cookie domain
 * @param expires the cookie expiration value
 * @param httpOnly true when the cookie is HTTP-only
 * @param secure true when the cookie is secure
 * @param sameSite the cookie SameSite value
 */
public record HttpExchangeCookie(String name, String value, String path, String domain, String expires,
        Boolean httpOnly, Boolean secure, String sameSite) {

    public HttpExchangeCookie {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }

    public HttpExchangeCookie(final String name, final String value) {
        this(name, value, null, null, null, null, null, null);
    }
}
