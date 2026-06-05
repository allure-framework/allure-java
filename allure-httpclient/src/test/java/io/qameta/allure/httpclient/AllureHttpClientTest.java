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
package io.qameta.allure.httpclient;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

class AllureHttpClientTest {

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

        stubFor(
                get(urlEqualTo("/empty"))
                        .willReturn(
                                aResponse()
                                        .withStatus(304)
                        )
        );

        stubFor(
                delete(urlEqualTo("/hello"))
                        .willReturn(noContent())
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
        final AllureResults results = executeWithAllure(() -> {
            final HttpClientBuilder builder = HttpClientBuilder.create()
                    .addInterceptorLast(new AllureHttpClientRequest())
                    .addInterceptorLast(new AllureHttpClientResponse());

            try (CloseableHttpClient httpClient = builder.build()) {
                final HttpGet httpGet = new HttpGet(String.format("http://localhost:%d/hello", server.port()));
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    response.getStatusLine().getStatusCode();
                    assertThat(EntityUtils.toString(response.getEntity()))
                            .isEqualTo(BODY_STRING);
                }
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
                .contains("\"url\":\"/hello\"")
                .contains("\"status\":200")
                .contains("\"value\":\"Hello world!\"");
    }

    @Test
    void shouldCreateExchangeAttachmentWithEmptyBody() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpClientBuilder builder = HttpClientBuilder.create()
                    .addInterceptorLast(new AllureHttpClientRequest())
                    .addInterceptorLast(new AllureHttpClientResponse());

            try (CloseableHttpClient httpClient = builder.build()) {
                final HttpGet httpGet = new HttpGet(String.format("http://localhost:%d/empty", server.port()));
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    assertThat(response.getEntity())
                            .isEqualTo(null);
                }
            }
        });

        assertThat(attachmentContent(results, httpExchangeAttachment(results)))
                .contains("\"status\":304")
                .contains("\"value\":\"No body present\"");
    }

    @Test
    void shouldCreateExchangeAttachmentWithEmptyRequestBodyWhenNoContentIsReturned() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpClientBuilder builder = HttpClientBuilder.create()
                    .addInterceptorLast(new AllureHttpClientRequest())
                    .addInterceptorLast(new AllureHttpClientResponse());

            try (CloseableHttpClient httpClient = builder.build()) {
                final HttpDelete httpDelete = new HttpDelete(String.format("http://localhost:%d/hello", server.port()));
                try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
                    assertThat(response.getEntity())
                            .isEqualTo(null);
                }
            }
        });

        assertThat(attachmentContent(results, httpExchangeAttachment(results)))
                .contains("\"method\":\"DELETE\"")
                .contains("\"status\":204")
                .doesNotContain("\"request\":{\"body\"");
    }

    @Test
    void shouldNotConsumeBody() {
        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addInterceptorLast(new AllureHttpClientResponse());

        executeWithAllure(() -> {
            try (CloseableHttpClient httpClient = builder.build()) {
                final HttpGet httpGet = new HttpGet(String.format("http://localhost:%d/hello", server.port()));
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    response.getStatusLine().getStatusCode();
                    BufferedHttpEntity ent = new BufferedHttpEntity(response.getEntity());
                    assertThat(EntityUtils.toString(ent))
                            .isEqualTo(BODY_STRING);
                }
            }
        });
    }

    private static AllureResults executeWithAllure(final ThrowingRunnable runnable) {
        return runWithinTestContext(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }

    private static Attachment httpExchangeAttachment(final AllureResults results) {
        final List<Attachment> attachments = attachments(results);

        assertThat(attachments).hasSize(1);
        return attachments.get(0);
    }

    private static List<Attachment> attachments(final AllureResults results) {
        return results.getTestResults().stream()
                .flatMap(AllureHttpClientTest::attachments)
                .toList();
    }

    private static Stream<Attachment> attachments(final TestResult result) {
        return Stream.concat(
                result.getAttachments().stream(),
                result.getSteps().stream().flatMap(AllureHttpClientTest::attachments)
        );
    }

    private static Stream<Attachment> attachments(final StepResult step) {
        return Stream.concat(
                step.getAttachments().stream(),
                step.getSteps().stream().flatMap(AllureHttpClientTest::attachments)
        );
    }

    private static String attachmentContent(final AllureResults results, final Attachment attachment) {
        return new String(results.getAttachments().get(attachment.getSource()), StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
