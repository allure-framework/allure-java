package io.qameta.allure.httpclient;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.AttachmentRenderer;

import io.qameta.allure.jaxrs.AllureJaxRs;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
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
public class AllureJaxRsTest {

    private static final String URL = "http://localhost/hello";
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
            response.close();
        }

        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);
        verify(processor, times(1))
                .addAttachment(captor.capture(), eq(requestRenderer));

        assertThat(captor.getAllValues())
                .hasSize(1)
                .extracting("url")
                .containsExactly(uri.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateResponseAttachment() throws Exception {
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
            response.close();
        }


        final ArgumentCaptor<AttachmentData> captor = ArgumentCaptor.forClass(AttachmentData.class);
        verify(processor, times(1))
                .addAttachment(captor.capture(), eq(responseRenderer));

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