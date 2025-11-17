/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.openfeign;

import com.github.tomakehurst.wiremock.WireMockServer;
import feign.Feign;
import feign.RequestLine;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllureResponseDecoderTests {

    static WireMockServer wireMockServer;

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/json"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"message\":\"Hello World\"}")));
    }

    @Test
    void jsonBodyTest() {
        AtomicReference<HelloWorldRecord> helloWorldRecord = new AtomicReference<>();

        AllureResults allureResults = runWithinTestContext(() -> {
            helloWorldRecord.set(Feign.builder()
                    .decoder(new AllureResponseDecoder(new GsonDecoder()))
                    .encoder(new GsonEncoder())
                    .target(HelloWorldFeignClient.class, wireMockServer.baseUrl())
                    .getJsonHelloWorld());
        });

        List<String> attachmentNames = allureResults.getTestResults().stream()
                .flatMap(testResult -> testResult.getAttachments().stream())
                .map(Attachment::getName).collect(Collectors.toList());

        assertAll(
                () -> assertEquals(new HelloWorldRecord("Hello World").getMessage(), helloWorldRecord.get().getMessage()),
                () -> assertTrue(attachmentNames.contains("Response"), "Cannot find attachment with name \"Response\""),
                () -> assertTrue(attachmentNames.contains("Request"), "Cannot find attachment with name \"Request\"")
        );
    }

    interface HelloWorldFeignClient {

        @RequestLine("GET /api/v1/json")
        HelloWorldRecord getJsonHelloWorld();

    }

    static class HelloWorldRecord {

        private String message;

        public HelloWorldRecord() {
        }

        public HelloWorldRecord(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

}
