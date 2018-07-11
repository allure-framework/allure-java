package io.qameta.allure.httpclient;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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
public class AllureHttpClientTest {

    private static final String BODY_STRING = "Hello world!";

    private WireMockServer server;

    @Before
    public void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        configureFor(server.port());

        stubFor(get(urlEqualTo("/hello"))
                .willReturn(aResponse()
                        .withBody(BODY_STRING)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateRequestAttachment() throws Exception {
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
    public void shouldCreateResponseAttachment() throws Exception {
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

    @After
    public void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop();
        }
    }
}