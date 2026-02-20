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
import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author a-simeshin (Simeshin Artem).
 */
@SuppressWarnings({"unchecked", "PMD.JUnitTestContainsTooManyAsserts"})
class AllureHttpClient5DeleteTest {

    private static final String DELETE_URL = "http://localhost:%d/delete";
    private static final String HELLO_RESOURCE_PATH = "/delete";

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        configureFor(server.port());

        stubFor(delete(HELLO_RESOURCE_PATH).willReturn(
                aResponse()
                        .withStatus(204))
        );
    }

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop();
        }
    }

    @Test
    void smokeDeleteShouldNotThrowThenReturnCorrectCode() {
        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addRequestInterceptorFirst(new AllureHttpClient5Request())
                .addResponseInterceptorLast(new AllureHttpClient5Response());

        assertDoesNotThrow(() -> {
            try (CloseableHttpClient httpClient = builder.build()) {
                final HttpDelete httpDelete = new HttpDelete(String.format(DELETE_URL, server.port()));
                httpClient.execute(httpDelete, response -> {
                    assertThat(response.getCode()).isEqualTo(204);
                    return response;
                });
            }
        });
    }

    @Test
    void shouldCreateDeleteRequestAttachment() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addRequestInterceptorFirst(new AllureHttpClient5Request(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpDelete httpDelete = new HttpDelete(String.format(DELETE_URL, server.port()));
            httpClient.execute(httpDelete, response -> {
                assertThat(response.getCode()).isEqualTo(204);
                return response;
            });
        }

        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);

        verify(processor, times(1)).addAttachment(captor.capture(), eq(renderer));
        assertThat(captor.getAllValues())
                .hasSize(1)
                .extracting("url")
                .containsExactly(HELLO_RESOURCE_PATH);
    }
    @Test
    void shouldCreateDeleteResponseAttachmentWithEmptyBody() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addResponseInterceptorLast(new AllureHttpClient5Response(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpDelete httpDelete = new HttpDelete(String.format(DELETE_URL, server.port()));
            httpClient.execute(httpDelete, response -> {
                assertThat(response.getCode()).isEqualTo(204);
                return response;
            });
        }

        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);
        verify(processor, times(1))
                .addAttachment(captor.capture(), eq(renderer));

        assertThat(captor.getAllValues())
                .hasSize(1)
                .extracting("body")
                .containsExactly("No body present");
    }
}
