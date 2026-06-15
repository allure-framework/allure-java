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
package io.qameta.allure.okhttp3;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.test.AllureResults;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
class AllureOkHttp3Test {

    private static final String BODY_STRING = "Hello world!";

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        configureFor(server.port());

        stubFor(
                get(urlEqualTo("/hello"))
                        .willReturn(
                                aResponse()
                                        .withBody(BODY_STRING)
                        )
        );
    }

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop();
        }
    }

    @Test
    void shouldCreateHttpExchangeAttachment() {
        final Request request = Allure.step(
                "Prepare an OkHttp request", () -> new Request.Builder()
                        .url(server.url("hello"))
                        .build()
        );

        final AllureResults results = Allure.step(
                "Execute the request through the Allure interceptor",
                () -> execute(request, checkBody(BODY_STRING))
        );

        Allure.step("Verify the generated HTTP exchange attachment", () -> {
            final Attachment attachment = httpExchangeAttachment(results);
            assertThat(attachment.getName()).isEqualTo("HTTP exchange");
            assertThat(attachment.getType()).isEqualTo(HttpExchange.CONTENT_TYPE);
            assertThat(attachment.getSource()).endsWith(HttpExchange.FILE_EXTENSION);
        });
    }

    @SafeVarargs
    protected final AllureResults execute(final Request request, final Consumer<Response>... matchers) {
        final OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AllureOkHttp3())
                .build();

        return runWithinTestContext(() -> {
            try {
                final Response response = client.newCall(request).execute();
                Stream.of(matchers).forEach(matcher -> matcher.accept(response));
            } catch (IOException e) {
                throw new RuntimeException("Could not execute request " + request, e);
            }
        });
    }

    protected Consumer<Response> checkBody(final String expectedBody) {
        return response -> {
            try {
                final ResponseBody body = response.body();
                if (Objects.isNull(body)) {
                    fail("empty response body");
                }
                assertThat(body.string()).isEqualTo(expectedBody);
            } catch (IOException e) {
                fail("could not read response body");
            }
        };
    }

    private static Attachment httpExchangeAttachment(final AllureResults results) {
        final List<Attachment> attachments = results.getAttachmentsRecursively();

        assertThat(attachments).hasSize(1);
        return attachments.get(0);
    }
}
