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
package io.qameta.allure.springweb;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static io.qameta.allure.Allure.step;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import io.qameta.allure.test.IsolatedLifecycle;
@SuppressWarnings("unchecked")
@IsolatedLifecycle
public class AllureRestTemplateTest {

    static Stream<SpringClientType> clientTypeProvider() {
        return Stream.of(SpringClientType.values());
    }

    @ParameterizedTest
    @MethodSource("clientTypeProvider")
    void shouldCreateAttachment(final SpringClientType clientType) {
        final AllureResults results = execute(clientType).getAllureResults();
        assertThat(results.getAttachmentsRecursively())
                .extracting(Attachment::getName)
                .containsExactly("HTTP exchange");
    }

    @ParameterizedTest
    @MethodSource("clientTypeProvider")
    void shouldCatchAttachmentBody(final SpringClientType clientType) {
        final AllureResults results = execute(clientType).getAllureResults();

        final Attachment attachment = findAttachment(results);
        assertThat(attachment.getType()).isEqualTo(HttpExchange.CONTENT_TYPE);
        assertThat(attachment.getSource()).endsWith(HttpExchange.FILE_EXTENSION);
        assertThat(results.getAttachments()).containsKeys(attachment.getSource());
    }

    @ParameterizedTest
    @MethodSource("clientTypeProvider")
    void shouldAllowResponseBodyConsumptionAfterInterception(final SpringClientType clientType) {
        final ExecutionResult executionResult = execute(clientType);
        assertThat(executionResult.getResponse().getBody()).isEqualTo("some body");
    }

    protected final ExecutionResult execute(final SpringClientType clientType) {
        final WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        final AtomicReference<ResponseEntity<String>> response = new AtomicReference<>();

        final AllureResults results = step("Execute Spring HTTP client request and collect Allure results", () -> runWithinTestContext(() -> {
            server.start();
            WireMock.configureFor(server.port());
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/hello")).willReturn(WireMock.aResponse().withBody("some body")));
            try {
                final ResponseEntity<String> result = clientType.execute(server.url("/hello"));
                response.set(result);
                assertThat(result.getStatusCode())
                        .isEqualTo(HttpStatus.OK);
                assertThat(result.getBody())
                        .isEqualTo("some body");
            } finally {
                server.stop();
            }
        }));

        return new ExecutionResult(results, response.get());
    }

    private static Attachment findAttachment(final AllureResults results) {
        final List<Attachment> attachments = results.getAttachmentsRecursively();

        assertThat(attachments)
                .extracting(Attachment::getName)
                .containsExactly("HTTP exchange");
        return attachments.get(0);
    }

    private static BufferingClientHttpRequestFactory createBufferingRequestFactory() {
        return new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
    }

    private enum SpringClientType {
        REST_TEMPLATE {
            @Override
            ResponseEntity<String> execute(final String url) {
                final RestTemplate restTemplate = new RestTemplate(createBufferingRequestFactory());
                restTemplate.setInterceptors(Collections.singletonList(new AllureRestTemplate()));

                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                final HttpEntity<JsonNode> entity = new HttpEntity<>(headers);
                return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            }
        },
        REST_CLIENT {
            @Override
            ResponseEntity<String> execute(final String url) {
                final RestClient restClient = RestClient.builder()
                        .requestFactory(createBufferingRequestFactory())
                        .requestInterceptor(new AllureRestTemplate())
                        .build();
                return restClient.get()
                        .uri(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(String.class);
            }
        };

        abstract ResponseEntity<String> execute(String url);
    }

    private static final class ExecutionResult {

        private final AllureResults allureResults;
        private final ResponseEntity<String> response;

        private ExecutionResult(final AllureResults allureResults, final ResponseEntity<String> response) {
            this.allureResults = allureResults;
            this.response = response;
        }

        AllureResults getAllureResults() {
            return allureResults;
        }

        ResponseEntity<String> getResponse() {
            return response;
        }
    }
}
