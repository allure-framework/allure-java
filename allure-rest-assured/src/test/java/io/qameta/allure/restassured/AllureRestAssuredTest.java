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
package io.qameta.allure.restassured;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.common.collect.ImmutableList;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AttachmentArgumentProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

        return Stream.of(
                arguments(ImmutableList.of("Request", "HTTP/1.1 200 OK"), new AllureRestAssured()),
                arguments(ImmutableList.of("Allure Request", "Allure Response"), new AllureRestAssured().setRequestAttachmentName("Allure Request").setResponseAttachmentName("Allure Response")),
                arguments(ImmutableList.of("Request", "Allure Response"), new AllureRestAssured().setResponseAttachmentName("Allure Response")),
                arguments(ImmutableList.of("Allure Request", "HTTP/1.1 200 OK"), new AllureRestAssured().setRequestAttachmentName("Allure Request"))
        );
    }
}

class HiddenHeadersArgumentProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

        final String hiddenHeader = "Authorization";
        final String header = "Accept";
        final String headerValue = "value";

        final Map<String, String> headers = Map.of(hiddenHeader, headerValue, header, headerValue);
        final List<String> expectedHeaders = List.of(hiddenHeader + ": [ BLACKLISTED ]", header + ": " + headerValue);

        return Stream.of(
                arguments(headers, hiddenHeader, expectedHeaders, new AllureRestAssured()),
                arguments(headers, hiddenHeader.toUpperCase(), expectedHeaders, new AllureRestAssured())
        );
    }
}

/**
 * @author charlie (Dmitry Baev).
 */
class AllureRestAssuredTest {

    @ParameterizedTest
    @ArgumentsSource(AttachmentArgumentProvider.class)
    void shouldCreateAttachment(final List<String> attachmentNames, final AllureRestAssured filter) {
        RestAssured.replaceFiltersWith(filter);
        final AllureResults results = execute();

        assertThat(results.getTestResults()
                .stream()
                .map(TestResult::getAttachments)
                .flatMap(Collection::stream)
                .map(Attachment::getName))
                .containsExactlyElementsOf(attachmentNames);
    }

    @Test
    void shouldProperlySetAttachmentNameForSingleFilterInstance() {
        final AllureRestAssured filter = new AllureRestAssured();

        final ResponseDefinitionBuilder responseBuilderOne = WireMock.aResponse()
                .withStatus(200)
                .withBody("some body");

        final ResponseDefinitionBuilder responseBuilderTwo = WireMock.aResponse()
                .withStatus(400)
                .withBody("some other body");

        RestAssured.replaceFiltersWith(filter);
        final AllureResults resultsOne = executeWithStub(responseBuilderOne);

        RestAssured.replaceFiltersWith(filter);
        final AllureResults resultsTwo = executeWithStub(responseBuilderTwo);

        assertThat(resultsOne.getTestResults()
                .stream()
                .map(TestResult::getAttachments)
                .flatMap(Collection::stream)
                .map(Attachment::getName))
                .hasSize(2)
                .anyMatch(res -> res.equals("HTTP/1.1 200 OK"));

        assertThat(resultsTwo.getTestResults()
                .stream()
                .map(TestResult::getAttachments)
                .flatMap(Collection::stream)
                .map(Attachment::getName))
                .hasSize(2)
                .anyMatch(res -> res.equals("HTTP/1.1 400 Bad Request"));
    }

    @ParameterizedTest
    @ArgumentsSource(AttachmentArgumentProvider.class)
    void shouldCatchAttachmentBody(final List<String> attachmentNames, final AllureRestAssured filter) {
        RestAssured.replaceFiltersWith(filter);
        final AllureResults results = execute();

        List<Attachment> actualAttachments = results.getTestResults().stream()
                .map(TestResult::getAttachments)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        assertThat(actualAttachments)
                .map(Attachment::getName)
                .containsExactlyElementsOf(attachmentNames)
                .doesNotContainNull();

        assertThat(actualAttachments)
                .map(Attachment::getSource)
                .containsExactlyInAnyOrderElementsOf(results.getAttachments().keySet())
                .doesNotContainNull();
    }

    @ParameterizedTest
    @ArgumentsSource(HiddenHeadersArgumentProvider.class)
    void shouldHideHeadersInAttachments(final Map<String, String> headers,
                                        final String hiddenHeader,
                                        final List<String> expectedValues,
                                        final AllureRestAssured filter) {

        final ResponseDefinitionBuilder responseBuilder = WireMock.aResponse().withStatus(200);
        headers.forEach(responseBuilder::withHeader);

        RestAssured.config = new RestAssuredConfig().logConfig(LogConfig.logConfig().blacklistHeaders(List.of(hiddenHeader)));
        RestAssured.replaceFiltersWith(filter);

        final AllureResults results = executeWithStub(responseBuilder, RestAssured.with().headers(headers));

        assertThat(results.getAttachments().values())
                .hasSize(2)
                .map(at -> new String(at, StandardCharsets.UTF_8))
                .allSatisfy(at -> expectedValues.forEach(ev -> assertThat(at).contains(ev)));
    }

    protected final AllureResults execute() {
        return executeWithStub(WireMock.aResponse().withBody("some body"));
    }

    protected final AllureResults executeWithStub(ResponseDefinitionBuilder responseBuilder) {
        return executeWithStub(responseBuilder, RestAssured.given());
    }

    protected final AllureResults executeWithStub(ResponseDefinitionBuilder responseBuilder, RequestSpecification spec) {
        final WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        final int statusCode = responseBuilder.build().getStatus();

        return runWithinTestContext(() -> {
            server.start();
            WireMock.configureFor(server.port());

            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/hello?Allure=Form")).willReturn(responseBuilder));
            try {
                spec.contentType(ContentType.URLENC)
                    .formParams("Allure", "Form")
                    .get(server.url("/hello")).then().statusCode(statusCode);
            } finally {
                server.stop();
                RestAssured.replaceFiltersWith(ImmutableList.of());
                RestAssured.config = new RestAssuredConfig();
            }
        });
    }
}
