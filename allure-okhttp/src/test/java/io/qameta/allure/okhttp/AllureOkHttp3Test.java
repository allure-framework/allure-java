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
package io.qameta.allure.okhttp;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

/**
 * @author charlie (Dmitry Baev).
 */
class AllureOkHttp3Test {

    private static final String BODY_STRING = "Hello world!";

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        configureFor(server.port());

        stubFor(get(urlEqualTo("/hello"))
                .willReturn(aResponse()
                        .withBody(BODY_STRING)));
    }

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop();
        }
    }

    @Test
    void shouldCreateRequestAttachment() {
        final Request request = new Request.Builder()
                .url(server.url("hello"))
                .build();

        final AllureResults results = execute(request, checkBody(BODY_STRING));

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting(Attachment::getName)
                .contains("Request");
    }

    @Test
    void shouldCreateResponseAttachment() {
        final Request request = new Request.Builder()
                .url(server.url("hello"))
                .build();

        final AllureResults results = execute(request, checkBody(BODY_STRING));

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting(Attachment::getName)
                .contains("Response");
    }

    @SafeVarargs
    protected final AllureResults execute(final Request request, final Consumer<Response>... matchers) {
        final OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new AllureOkHttp());

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
}
