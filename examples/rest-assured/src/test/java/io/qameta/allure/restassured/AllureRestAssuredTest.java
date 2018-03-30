package io.qameta.allure.restassured;

import com.github.tomakehurst.wiremock.WireMockServer;
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
import static io.restassured.RestAssured.given;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureRestAssuredTest {

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        configureFor(server.port());
    }

    @Test
    void restAssuredAttachmentTest() throws IOException {
        stubFor(get(urlEqualTo("/hello"))
                .willReturn(aResponse()
                        .withBody("Hello world!")));

        given().filter(new AllureRestAssured()).get(String.format("http://localhost:%d/hello", server.port()));
    }

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop();
        }
    }
}
