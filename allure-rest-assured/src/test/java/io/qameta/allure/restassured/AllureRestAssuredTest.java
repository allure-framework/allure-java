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
package io.qameta.allure.restassured;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.common.collect.ImmutableList;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.restassured.RestAssured;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unchecked")
class AllureRestAssuredTest {

    static Stream<String> attachmentNameProvider() {
        return Stream.of("Request", "HTTP/1.1 200 OK");
    }

    @ParameterizedTest
    @MethodSource(value = "attachmentNameProvider")
    void shouldCreateAttachment(final String attachmentName) {
        final AllureResults results = execute();

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getAttachments)
                .flatExtracting(Attachment::getName)
                .contains(attachmentName);
    }

    @ParameterizedTest
    @MethodSource(value = "attachmentNameProvider")
    void shouldCatchAttachmentBody(final String attachmentName) {
        final AllureResults results = execute();

        final Attachment found = results.getTestResults().stream()
                .map(TestResult::getAttachments)
                .flatMap(Collection::stream)
                .filter(attachment -> Objects.equals(attachmentName, attachment.getName()))
                .findAny()
                .orElseThrow(() -> new RuntimeException("attachment not found"));

        assertThat(results.getAttachments())
                .containsKeys(found.getSource());
    }

    protected final AllureResults execute() {
        RestAssured.replaceFiltersWith(new AllureRestAssured());
        final WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());

        return runWithinTestContext(() -> {
            server.start();
            WireMock.configureFor(server.port());

            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/hello")).willReturn(WireMock.aResponse().withBody("some body")));
            try {
                RestAssured.when().get(server.url("/hello")).then().statusCode(200);
            } finally {
                server.stop();
                RestAssured.replaceFiltersWith(ImmutableList.of());
            }
        });
    }

}
