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
import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.jaxrs.AllureJaxRs;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

@IsolatedLifecycle
class AllureJaxRsTest {

    private static final String URL = "http://localhost/hello";
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
        final Client client = Allure.step(
                "Create a Jakarta REST client with the Allure filter", () -> ClientBuilder.newBuilder().build()
                        .register(new AllureJaxRs())
        );

        final URI uri = Allure.step(
                "Build a target URI for the WireMock endpoint", () -> UriBuilder.fromUri(URL)
                        .port(server.port())
                        .build()
        );

        final AllureResults results = Allure.step("Execute the request through the Jakarta REST client", () -> runWithinTestContext(() -> {
            Response response = null;
            try {
                response = client.target(uri).request().get();
                assertThat(response.readEntity(String.class))
                        .isEqualTo(BODY_STRING);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }
        ));

        Allure.step("Verify the generated HTTP exchange attachment", () -> {
            final Attachment attachment = httpExchangeAttachment(results);
            final String exchange = results.getAttachmentContentAsString(attachment);

            assertThat(attachment.getName()).isEqualTo("HTTP exchange");
            assertThat(attachment.getType()).isEqualTo(HttpExchange.CONTENT_TYPE);
            assertThat(attachment.getSource()).endsWith(HttpExchange.FILE_EXTENSION);

            assertThat(exchange)
                    .contains("\"method\":\"GET\"")
                    .contains("\"url\":\"" + uri + "\"")
                    .contains("\"status\":200")
                    .contains("\"value\":\"" + BODY_STRING + "\"");
        });
    }

    private static Attachment httpExchangeAttachment(final AllureResults results) {
        final List<Attachment> attachments = results.getAttachmentsRecursively();

        assertThat(attachments).hasSize(1);
        return attachments.get(0);
    }
}
