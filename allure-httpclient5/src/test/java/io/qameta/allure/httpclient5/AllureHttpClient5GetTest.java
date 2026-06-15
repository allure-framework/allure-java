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
package io.qameta.allure.httpclient5;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.test.AllureResults;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.qameta.allure.Allure.step;
import static io.qameta.allure.httpclient5.HttpExchangeTestSupport.attachmentContent;
import static io.qameta.allure.httpclient5.HttpExchangeTestSupport.executeWithAllure;
import static io.qameta.allure.httpclient5.HttpExchangeTestSupport.httpExchangeAttachment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
class AllureHttpClient5GetTest {

    private static final String BODY_STRING = "Hello world!";
    private static final String HELLO_RESOURCE_PATH = "/hello";
    private static final String HELLO_GET_RETURN_BODY = "http://localhost:%d/hello";
    private static final String HELLO_GET_201_NO_BODY = "http://localhost:%d/empty";

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        configureFor(server.port());

        stubFor(
                get(HELLO_RESOURCE_PATH).willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(BODY_STRING)
                )
        );
        stubFor(
                get("/empty").willReturn(
                        aResponse()
                                .withStatus(200)
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
    void smokeGetShouldNotThrowThenReturnCorrectResponseMessage() {
        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addRequestInterceptorFirst(new AllureHttpClient5Request())
                .addResponseInterceptorLast(new AllureHttpClient5Response());

        step("Execute GET request through Apache HttpClient 5 interceptors", () -> assertDoesNotThrow(() -> {
            try (CloseableHttpClient httpClient = builder.build()) {
                final HttpGet httpGet = new HttpGet(String.format(HELLO_GET_RETURN_BODY, server.port()));
                httpClient.execute(httpGet, response -> {
                    assertThat(EntityUtils.toString(response.getEntity())).isEqualTo(BODY_STRING);
                    return response;
                });
            }
        }));
    }

    @Test
    void shouldCreateGetHttpExchangeAttachment() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpClientBuilder builder = HttpClientBuilder.create()
                    .addRequestInterceptorFirst(new AllureHttpClient5Request())
                    .addResponseInterceptorLast(new AllureHttpClient5Response());

            try (CloseableHttpClient httpClient = builder.build()) {
                final HttpGet httpGet = new HttpGet(String.format(HELLO_GET_RETURN_BODY, server.port()));
                httpClient.execute(httpGet, response -> {
                    assertThat(EntityUtils.toString(response.getEntity())).isEqualTo(BODY_STRING);
                    return response;
                });
            }
        });

        final Attachment attachment = httpExchangeAttachment(results);
        final String exchange = attachmentContent(results, attachment);

        assertThat(attachment.getName()).isEqualTo("HTTP exchange");
        assertThat(attachment.getType()).isEqualTo(HttpExchange.CONTENT_TYPE);
        assertThat(attachment.getSource()).endsWith(HttpExchange.FILE_EXTENSION);

        assertThat(exchange)
                .contains("\"schemaVersion\":1")
                .contains("\"method\":\"GET\"")
                .contains("\"url\":\"" + HELLO_RESOURCE_PATH + "\"")
                .contains("\"status\":200")
                .contains("\"value\":\"" + BODY_STRING + "\"");
    }

    @Test
    void shouldCreateGetExchangeAttachmentWithEmptyBody() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpClientBuilder builder = HttpClientBuilder.create()
                    .addRequestInterceptorFirst(new AllureHttpClient5Request())
                    .addResponseInterceptorLast(new AllureHttpClient5Response());

            try (CloseableHttpClient httpClient = builder.build()) {
                final HttpGet httpGet = new HttpGet(String.format(HELLO_GET_201_NO_BODY, server.port()));
                httpClient.execute(httpGet, response -> {
                    assertThat(EntityUtils.toString(response.getEntity())).isEqualTo("");
                    return response;
                });
            }
        });

        assertThat(attachmentContent(results, httpExchangeAttachment(results)))
                .contains("\"status\":200")
                .contains("\"value\":\"No body present\"");
    }
}
