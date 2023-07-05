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
package io.qameta.allure.httpclient5;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
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

        stubFor(get(HELLO_RESOURCE_PATH).willReturn(
                aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(BODY_STRING)
        ));
        stubFor(get("/empty").willReturn(
                aResponse()
                        .withStatus(200))
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
                .addRequestInterceptorLast(new AllureHttpClient5Request())
                .addResponseInterceptorFirst(new AllureHttpClient5Response());

        assertDoesNotThrow(() -> {
            try (CloseableHttpClient httpClient = builder.build()) {
                final HttpGet httpGet = new HttpGet(String.format(HELLO_GET_RETURN_BODY, server.port()));
                httpClient.execute(httpGet, response -> {
                    assertThat(EntityUtils.toString(response.getEntity())).isEqualTo(BODY_STRING);
                    return response;
                });
            }
        });
    }

    @Test
    void shouldCreateGetRequestAttachment() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addRequestInterceptorLast(new AllureHttpClient5Request(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpGet httpGet = new HttpGet(String.format(HELLO_GET_RETURN_BODY, server.port()));
            httpClient.execute(httpGet, response -> {
                assertThat(EntityUtils.toString(response.getEntity())).isEqualTo(BODY_STRING);
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
    void shouldCreateGetResponseAttachment() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addResponseInterceptorLast(new AllureHttpClient5Response(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpGet httpGet = new HttpGet(String.format(HELLO_GET_RETURN_BODY, server.port()));
            httpClient.execute(httpGet, response -> {
                assertThat(EntityUtils.toString(response.getEntity())).isEqualTo(BODY_STRING);
                return response;
            });
        }

        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);
        verify(processor, times(1))
                .addAttachment(captor.capture(), eq(renderer));

        assertThat(captor.getAllValues())
                .hasSize(1)
                .extracting("responseCode")
                .containsExactly(200);
    }

    @Test
    void shouldCreateGetResponseAttachmentWithEmptyBody() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addResponseInterceptorLast(new AllureHttpClient5Response(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpGet httpGet = new HttpGet(String.format(HELLO_GET_201_NO_BODY, server.port()));
            httpClient.execute(httpGet, response -> {
                assertThat(EntityUtils.toString(response.getEntity())).isEqualTo("");
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
