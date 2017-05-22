package io.qameta.allure.httpclient;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * @author charlie (Dmitry Baev).
 */
public class HttpClientAttachmentTest {

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        configureFor(server.port());
    }

    @Test
    void httpClientAttachmentTest() throws IOException {
        stubFor(get(urlEqualTo("/hello"))
                .willReturn(aResponse()
                        .withBody("Hello world!")));

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addInterceptorFirst(new AllureHttpClientRequest())
                .addInterceptorLast(new AllureHttpClientResponse());
        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpGet httpGet = new HttpGet(String.format("http://localhost:%d/hello", server.port()));
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                response.getStatusLine().getStatusCode();
            }
        }
    }

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop();
        }
    }
}
