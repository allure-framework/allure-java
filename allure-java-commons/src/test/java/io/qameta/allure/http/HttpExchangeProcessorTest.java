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

import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.qameta.allure.Allure.attachment;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class HttpExchangeProcessorTest {

    @Test
    void shouldApplyBuilderOptionsWhenExchangeIsBuilt() {
        final HttpExchangeBody body = new HttpExchangeBody(
                "application/json",
                "utf8",
                "0123456789",
                null,
                null,
                List.of(
                        new HttpExchangeNameValue("password", "secret"),
                        new HttpExchangeNameValue("visible", "value")
                ),
                List.of(
                        new HttpExchangeBodyPart(
                                "payload",
                                null,
                                List.of(new HttpExchangeNameValue("Authorization", "Bearer token")),
                                "text/plain",
                                "utf8",
                                "abcdef",
                                null,
                                null
                        )
                ),
                null
        );

        final HttpExchange actual = step(
                "Build exchange with redaction options and five-byte body limit",
                () -> HttpExchange.builder()
                        .clearRedactedHeaders()
                        .redactHeader("Authorization")
                        .redactCookie("session")
                        .redactQueryParameter("token")
                        .redactFormParameter("password")
                        .setMaxBodySize(5)
                        .request("POST", "https://example.test/api", request -> request
                                .addHeader("Authorization", "Bearer token")
                                .addHeader("Accept", "application/json")
                                .addCookie("SESSION", "cookie-secret")
                                .addCookie("theme", "dark")
                                .addQuery("token", "query-secret")
                                .addQuery("page", "1")
                                .setBody(body))
                        .response(response -> response.setStatus(200))
                        .build()
        );

        step("Verify redacted values and truncation metadata", () -> {
            attachment("processed HTTP exchange", new String(
                    HttpExchangeSerializer.toJsonBytes(actual),
                    StandardCharsets.UTF_8
            ));

            assertThat(actual.request().headers())
                    .extracting(HttpExchangeNameValue::name, HttpExchangeNameValue::value)
                    .containsExactly(
                            tuple("Authorization", HttpExchange.REDACTED_VALUE),
                            tuple("Accept", "application/json")
                    );
            assertThat(actual.request().cookies())
                    .extracting(HttpExchangeCookie::name, HttpExchangeCookie::value)
                    .containsExactly(
                            tuple("SESSION", HttpExchange.REDACTED_VALUE),
                            tuple("theme", "dark")
                    );
            assertThat(actual.request().query())
                    .extracting(HttpExchangeNameValue::name, HttpExchangeNameValue::value)
                    .containsExactly(
                            tuple("token", HttpExchange.REDACTED_VALUE),
                            tuple("page", "1")
                    );
            assertThat(actual.request().body())
                    .hasFieldOrPropertyWithValue("value", "01234")
                    .hasFieldOrPropertyWithValue("size", 10L)
                    .hasFieldOrPropertyWithValue("truncated", true);
            assertThat(actual.request().body().form())
                    .extracting(HttpExchangeNameValue::name, HttpExchangeNameValue::value)
                    .containsExactly(
                            tuple("password", HttpExchange.REDACTED_VALUE),
                            tuple("visible", "value")
                    );

            final HttpExchangeBodyPart part = actual.request().body().parts().get(0);
            assertThat(part.headers())
                    .extracting(HttpExchangeNameValue::name, HttpExchangeNameValue::value)
                    .containsExactly(tuple("Authorization", HttpExchange.REDACTED_VALUE));
            assertThat(part)
                    .hasFieldOrPropertyWithValue("value", "abcde")
                    .hasFieldOrPropertyWithValue("size", 6L)
                    .hasFieldOrPropertyWithValue("truncated", true);
        });
    }

    @Test
    void shouldSerializeExchangeCreatedWithBuilderOptions() {
        final HttpExchange exchange = step(
                "Build exchange with redaction options and three-byte body limit",
                () -> HttpExchange.builder()
                        .clearRedactedHeaders()
                        .redactHeader("X-Secret")
                        .setMaxBodySize(3)
                        .request("POST", "https://example.test/api", request -> request
                                .addHeader("X-Secret", "secret")
                                .setBody(HttpExchangeBody.utf8("abcdef")))
                        .response(response -> response.setStatus(200))
                        .build()
        );

        final String json = step(
                "Serialize already captured exchange with commons Jackson mapper",
                () -> new String(
                        HttpExchangeSerializer.toJsonBytes(exchange),
                        StandardCharsets.UTF_8
                )
        );

        step("Verify serialized HTTP exchange contract", () -> {
            attachment("serialized HTTP exchange", json);

            assertThat(json)
                    .contains("\"schemaVersion\":1")
                    .contains("\"status\":200")
                    .contains("\"value\":\"" + HttpExchange.REDACTED_VALUE + "\"")
                    .contains("\"value\":\"abc\"")
                    .contains("\"size\":6")
                    .contains("\"truncated\":true");
        });
    }

    @Test
    void shouldSerializeRawExchangeWithoutApplyingBuilderOptions() {
        final HttpExchange exchange = step(
                "Create raw exchange without capture builder",
                () -> new HttpExchange(
                        HttpExchangeRequest.builder("POST", "https://example.test/api")
                                .addHeader("X-Secret", "secret")
                                .setBody(HttpExchangeBody.utf8("abcdef"))
                                .build(),
                        null,
                        null,
                        null,
                        null
                )
        );

        final String json = step(
                "Serialize raw exchange with commons Jackson mapper",
                () -> new String(HttpExchangeSerializer.toJsonBytes(exchange), StandardCharsets.UTF_8)
        );

        step("Verify serializer does not redact or truncate raw exchanges", () -> {
            attachment("raw serialized HTTP exchange", json);

            assertThat(json)
                    .contains("\"value\":\"secret\"")
                    .contains("\"value\":\"abcdef\"")
                    .doesNotContain(HttpExchange.REDACTED_VALUE)
                    .doesNotContain("\"truncated\":true");
        });
    }
}
