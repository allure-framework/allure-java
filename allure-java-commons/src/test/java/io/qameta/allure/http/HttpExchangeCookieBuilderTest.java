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

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class HttpExchangeCookieBuilderTest {

    @Test
    void requestBuilderConvertsCookieHeadersToStructuredCookies() {
        final HttpExchangeRequest request = step(
                "Build a request from individual and bulk headers",
                () -> HttpExchangeRequest.builder("GET", "https://example.test")
                        .addHeader("Cookie", "session=abc=123; theme=dark")
                        .addHeaders(
                                List.of(
                                        new HttpExchangeNameValue("X-Trace", "trace-1"),
                                        new HttpExchangeNameValue("cOoKiE", "locale=en-GB")
                                )
                        )
                        .build()
        );

        step("Verify Cookie headers are represented once as structured cookies", () -> {
            assertThat(request.headers())
                    .extracting(HttpExchangeNameValue::name, HttpExchangeNameValue::value)
                    .containsExactly(tuple("X-Trace", "trace-1"));
            assertThat(request.cookies())
                    .extracting(HttpExchangeCookie::name, HttpExchangeCookie::value)
                    .containsExactly(
                            tuple("session", "abc=123"),
                            tuple("theme", "dark"),
                            tuple("locale", "en-GB")
                    );
        });
    }

    @Test
    void responseBuilderConvertsSetCookieHeadersAndTrailersToStructuredCookies() {
        final HttpExchangeResponse response = step(
                "Build a response from headers and trailers",
                () -> HttpExchangeResponse.builder()
                        .addHeader(
                                "SET-COOKIE",
                                "session=response-secret; Path=/; Domain=example.test; "
                                        + "Expires=Wed, 21 Oct 2015 07:28:00 GMT; HttpOnly; Secure; SameSite=Lax"
                        )
                        .addHeaders(
                                List.of(
                                        new HttpExchangeNameValue("X-Trace", "trace-1"),
                                        new HttpExchangeNameValue("set-cookie", "theme=light")
                                )
                        )
                        .addTrailer("Set-Cookie", "trailer=value")
                        .build()
        );

        step("Verify Set-Cookie fields preserve supported attributes without raw duplicates", () -> {
            assertThat(response.headers())
                    .extracting(HttpExchangeNameValue::name, HttpExchangeNameValue::value)
                    .containsExactly(tuple("X-Trace", "trace-1"));
            assertThat(response.trailers()).isNull();
            assertThat(response.cookies())
                    .extracting(
                            HttpExchangeCookie::name,
                            HttpExchangeCookie::value,
                            HttpExchangeCookie::path,
                            HttpExchangeCookie::domain,
                            HttpExchangeCookie::expires,
                            HttpExchangeCookie::httpOnly,
                            HttpExchangeCookie::secure,
                            HttpExchangeCookie::sameSite
                    )
                    .containsExactly(
                            tuple(
                                    "session",
                                    "response-secret",
                                    "/",
                                    "example.test",
                                    "Wed, 21 Oct 2015 07:28:00 GMT",
                                    true,
                                    true,
                                    "Lax"
                            ),
                            tuple("theme", "light", null, null, null, null, null, null),
                            tuple("trailer", "value", null, null, null, null, null, null)
                    );
        });
    }

    @Test
    void buildersKeepRawCookieHeadersWhenConversionWouldLoseInformation() {
        final HttpExchangeRequest request = step(
                "Build a request with a malformed Cookie header",
                () -> HttpExchangeRequest.builder("GET", "https://example.test")
                        .addHeader("Cookie", "session=value; malformed")
                        .build()
        );
        final HttpExchangeResponse response = step(
                "Build a response with unsupported and malformed Set-Cookie fields",
                () -> HttpExchangeResponse.builder()
                        .addHeader("Set-Cookie", "session=value; Max-Age=60")
                        .addTrailer("Set-Cookie", "malformed")
                        .build()
        );

        step("Verify the original fields remain available for lossless capture", () -> {
            assertThat(request.cookies()).isNull();
            assertThat(request.headers())
                    .extracting(HttpExchangeNameValue::name, HttpExchangeNameValue::value)
                    .containsExactly(tuple("Cookie", "session=value; malformed"));
            assertThat(response.cookies()).isNull();
            assertThat(response.headers())
                    .extracting(HttpExchangeNameValue::name, HttpExchangeNameValue::value)
                    .containsExactly(tuple("Set-Cookie", "session=value; Max-Age=60"));
            assertThat(response.trailers())
                    .extracting(HttpExchangeNameValue::name, HttpExchangeNameValue::value)
                    .containsExactly(tuple("Set-Cookie", "malformed"));
        });
    }
}
