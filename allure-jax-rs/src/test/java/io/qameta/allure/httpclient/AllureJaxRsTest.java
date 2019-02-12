/*
 *  Copyright 2019 Qameta Software OÜ
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
import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import io.qameta.allure.jaxrs.AllureJaxRs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureJaxRsTest {

    private static final String URL = "http://localhost/hello";
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

    @SuppressWarnings("unchecked")
    @Test
    void shouldCreateRequestAttachment() {
        final AttachmentRenderer<AttachmentData> requestRenderer = mock(AttachmentRenderer.class);
        final AttachmentRenderer<AttachmentData> responseRenderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        Client client = ClientBuilder.newBuilder().build()
                .register(new AllureJaxRs(requestRenderer, responseRenderer, processor));

        URI uri = UriBuilder.fromUri(URL)
                .port(server.port())
                .build();

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

        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);
        verify(processor, times(1))
                .addAttachment(captor.capture(), eq(requestRenderer));

        final List<AttachmentData> allValues = captor.getAllValues();

        assertThat(allValues)
                .hasSize(1)
                .extracting("url")
                .containsExactly(uri.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCreateResponseAttachment() {
        final AttachmentRenderer<AttachmentData> requestRenderer = mock(AttachmentRenderer.class);
        final AttachmentRenderer<AttachmentData> responseRenderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        Client client = ClientBuilder.newBuilder().build()
                .register(new AllureJaxRs(requestRenderer, responseRenderer, processor));

        URI uri = UriBuilder.fromUri(URL)
                .port(server.port())
                .build();

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


        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);
        verify(processor, times(1))
                .addAttachment(captor.capture(), eq(responseRenderer));

        final List<AttachmentData> allValues = captor.getAllValues();

        assertThat(allValues)
                .hasSize(1)
                .extracting("responseCode")
                .containsExactly(200);
    }
}
