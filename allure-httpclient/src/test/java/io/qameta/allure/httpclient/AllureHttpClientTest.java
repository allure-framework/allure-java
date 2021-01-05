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
import org.mockito.ArgumentCaptor;

import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
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
class AllureHttpClientTest {

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

        stubFor(get(urlEqualTo("/empty"))
                .willReturn(aResponse()
                        .withStatus(304)));

        stubFor(delete(urlEqualTo("/hello"))
                .willReturn(noContent()));
    }

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCreateRequestAttachment() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addInterceptorLast(new AllureHttpClientRequest(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpGet httpGet = new HttpGet(String.format("http://localhost:%d/hello", server.port()));
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                response.getStatusLine().getStatusCode();
                assertThat(EntityUtils.toString(response.getEntity()))
                        .isEqualTo(BODY_STRING);
            }
        }

        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);
        verify(processor, times(1))
                .addAttachment(captor.capture(), eq(renderer));

        assertThat(captor.getAllValues())
                .hasSize(1)
                .extracting("url")
                .containsExactly("/hello");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCreateResponseAttachment() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addInterceptorLast(new AllureHttpClientResponse(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpGet httpGet = new HttpGet(String.format("http://localhost:%d/hello", server.port()));
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                response.getStatusLine().getStatusCode();
                assertThat(EntityUtils.toString(response.getEntity()))
                        .isEqualTo(BODY_STRING);
            }
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
    void shouldCreateResponseAttachmentWithEmptyBody() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addInterceptorLast(new AllureHttpClientResponse(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpGet httpGet = new HttpGet(String.format("http://localhost:%d/empty", server.port()));
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                assertThat(response.getEntity())
                        .isEqualTo(null);
            }
        }

        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);
        verify(processor, times(1))
                .addAttachment(captor.capture(), eq(renderer));

        assertThat(captor.getAllValues())
                .hasSize(1)
                .extracting("body")
                .containsExactly("No body present");
    }

    @Test
    void shouldCreateRequestAttachmentWithEmptyBodyWhenNoContentIsReturned() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                                                           .addInterceptorLast(new AllureHttpClientRequest(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpDelete httpDelete = new HttpDelete(String.format("http://localhost:%d/hello", server.port()));
            try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
                assertThat(response.getEntity())
                        .isEqualTo(null);
            }
        }

        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);
        verify(processor, times(1))
                .addAttachment(captor.capture(), eq(renderer));

        assertThat(captor.getAllValues())
                .hasSize(1)
                .extracting("body")
                .containsNull();
    }

    @Test
    void shouldNotConsumeBody() throws Exception {
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentProcessor<AttachmentData> processor = mock(AttachmentProcessor.class);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                                                           .addInterceptorLast(new AllureHttpClientResponse(renderer, processor));

        try (CloseableHttpClient httpClient = builder.build()) {
          final HttpGet httpGet = new HttpGet(String.format("http://localhost:%d/hello", server.port()));
          try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
              response.getStatusLine().getStatusCode();
              BufferedHttpEntity ent = new BufferedHttpEntity(response.getEntity());
              assertThat(EntityUtils.toString(ent))
                      .isEqualTo(BODY_STRING);
          }
        }
    }
}
