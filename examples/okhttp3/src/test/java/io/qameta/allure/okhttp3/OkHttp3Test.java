package io.qameta.allure.okhttp3;

import com.github.tomakehurst.wiremock.WireMockServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
public class OkHttp3Test {

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        configureFor(server.port());
    }

    @Test
    void okHttp3ClientAttachmentTest() throws IOException {
        stubFor(get(urlEqualTo("/hello"))
                .willReturn(aResponse()
                        .withBody("Hello world!")));

        final OkHttpClient client = new OkHttpClient().newBuilder().addInterceptor(new AllureOkHttp3()).build();

        final Request request = new Request.Builder()
                .url(String.format("http://localhost:%d/hello", server.port()))
                .build();

        client.newCall(request).execute();
    }

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop();
        }
    }
}
