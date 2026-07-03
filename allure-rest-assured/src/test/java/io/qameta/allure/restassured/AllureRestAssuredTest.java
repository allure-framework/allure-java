/*
 *  Copyright 2016-2026 Qameta Software Inc
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
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.test.AllureResults;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.qameta.allure.Allure.step;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import io.qameta.allure.test.IsolatedLifecycle;

@IsolatedLifecycle
class AttachmentArgumentProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
        return Stream.of(
                arguments(ImmutableList.of("HTTP exchange"), new AllureRestAssured()),
                arguments(
                        ImmutableList.of("Allure Response"), new AllureRestAssured()
                                .setRequestAttachmentName("Allure Request")
                                .setResponseAttachmentName("Allure Response")
                ),
                arguments(
                        ImmutableList.of("Allure Response"), new AllureRestAssured()
                                .setResponseAttachmentName("Allure Response")
                ),
                arguments(
                        ImmutableList.of("Allure Request"), new AllureRestAssured()
                                .setRequestAttachmentName("Allure Request")
                )
        );
    }
}

@IsolatedLifecycle
class JsonPrettifyingArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
        return Stream.of(
                arguments(new AllureRestAssured(), "\"name\""),
                arguments(new AllureRestAssured().setMaxAllowedPrettifyLength(5), "\\\"name\\\":\\\"12345\\\"")
        );
    }
}

@IsolatedLifecycle
class HiddenHeadersArgumentProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
        final String hiddenHeader = "Authorization";
        final String header = "Accept";
        final String headerValue = "value";

        final Map<String, String> headers = Map.of(hiddenHeader, headerValue, header, headerValue);
        final List<String> expectedHeaders = List.of(
                "\"name\":\"" + hiddenHeader + "\",\"value\":\"" + HttpExchange.REDACTED_VALUE + "\"",
                "\"name\":\"" + header + "\",\"value\":\"" + headerValue + "\""
        );

        return Stream.of(
                arguments(headers, hiddenHeader, expectedHeaders, new AllureRestAssured()),
                arguments(headers, hiddenHeader.toUpperCase(), expectedHeaders, new AllureRestAssured())
        );
    }
}
@IsolatedLifecycle
class AllureRestAssuredTest {

    @ParameterizedTest
    @ArgumentsSource(AttachmentArgumentProvider.class)
    void shouldCreateAttachment(final List<String> attachmentNames, final AllureRestAssured filter) {
        final AllureResults results = executeWithStub(
                server -> WireMock.stubFor(
                        WireMock.get(WireMock.urlEqualTo("/hello?Allure=Form"))
                                .willReturn(WireMock.aResponse().withStatus(200).withBody("some body"))
                ),
                server -> RestAssured.given()
                        .contentType(ContentType.URLENC)
                        .formParams("Allure", "Form")
                        .get(server.url("/hello")).then().statusCode(200),
                filter
        );

        assertThat(allAttachmentNames(results))
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

        // Reuse the same filter instance for both requests to verify names are not cached.
        final AllureResults resultsOne = executeWithStub(
                server -> WireMock.stubFor(
                        WireMock.get(WireMock.urlEqualTo("/hello?Allure=Form"))
                                .willReturn(responseBuilderOne)
                ),
                server -> RestAssured.given()
                        .contentType(ContentType.URLENC)
                        .formParams("Allure", "Form")
                        .get(server.url("/hello")).then().statusCode(200),
                filter
        );

        final AllureResults resultsTwo = executeWithStub(
                server -> WireMock.stubFor(
                        WireMock.get(WireMock.urlEqualTo("/hello?Allure=Form"))
                                .willReturn(responseBuilderTwo)
                ),
                server -> RestAssured.given()
                        .contentType(ContentType.URLENC)
                        .formParams("Allure", "Form")
                        .get(server.url("/hello")).then().statusCode(400),
                filter
        );

        assertThat(allAttachmentNames(resultsOne))
                .containsExactly("HTTP exchange");

        assertThat(allAttachmentNames(resultsTwo))
                .containsExactly("HTTP exchange");

        assertThat(resultsOne.getAttachmentContentAsString(httpExchangeAttachment(resultsOne)))
                .contains("\"status\":200");
        assertThat(resultsTwo.getAttachmentContentAsString(httpExchangeAttachment(resultsTwo)))
                .contains("\"status\":400");
    }

    @ParameterizedTest
    @ArgumentsSource(AttachmentArgumentProvider.class)
    void shouldCatchAttachmentBody(final List<String> attachmentNames, final AllureRestAssured filter) {
        final AllureResults results = executeWithStub(
                server -> WireMock.stubFor(
                        WireMock.get(WireMock.urlEqualTo("/hello?Allure=Form"))
                                .willReturn(WireMock.aResponse().withStatus(200).withBody("some body"))
                ),
                server -> RestAssured.given()
                        .contentType(ContentType.URLENC)
                        .formParams("Allure", "Form")
                        .get(server.url("/hello")).then().statusCode(200),
                filter
        );

        final List<Attachment> actualAttachments = results.getAttachmentsRecursively();

        assertThat(actualAttachments)
                .map(Attachment::getName)
                .containsExactlyElementsOf(attachmentNames)
                .doesNotContainNull();

        assertThat(actualAttachments)
                .map(Attachment::getSource)
                .containsExactlyInAnyOrderElementsOf(results.getAttachments().keySet())
                .doesNotContainNull();

        assertThat(actualAttachments)
                .map(Attachment::getType)
                .containsExactly(HttpExchange.CONTENT_TYPE);
    }

    @ParameterizedTest
    @ArgumentsSource(HiddenHeadersArgumentProvider.class)
    void shouldHideHeadersInAttachments(final Map<String, String> headers,
                                        final String hiddenHeader,
                                        final List<String> expectedValues,
                                        final AllureRestAssured filter) {
        final ResponseDefinitionBuilder responseBuilder = WireMock.aResponse().withStatus(200);
        headers.forEach(responseBuilder::withHeader);

        RestAssured.config = new RestAssuredConfig().logConfig(
                LogConfig.logConfig().blacklistHeaders(List.of(hiddenHeader))
        );

        final AllureResults results = executeWithStub(
                server -> WireMock.stubFor(
                        WireMock.get(WireMock.urlEqualTo("/hello?Allure=Form"))
                                .willReturn(responseBuilder)
                ),
                server -> RestAssured.with()
                        .headers(headers)
                        .contentType(ContentType.URLENC)
                        .formParams("Allure", "Form")
                        .get(server.url("/hello")).then().statusCode(200),
                filter
        );

        assertThat(results.getAttachments().values())
                .hasSize(1)
                .map(at -> new String(at, StandardCharsets.UTF_8))
                .allSatisfy(at -> expectedValues.forEach(ev -> assertThat(at).contains(ev)));
    }

    @ParameterizedTest
    @ArgumentsSource(JsonPrettifyingArgumentsProvider.class)
    void responseJsonPrettified(final AllureRestAssured filter, final String formattedBody) {
        final ResponseDefinitionBuilder responseBuilder = WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"name":"12345","value":"abcdef"}
                        """)
                .withStatus(200);

        final AllureResults results = executeWithStub(
                server -> WireMock.stubFor(
                        WireMock.get(WireMock.urlEqualTo("/hello?Allure=Form"))
                                .willReturn(responseBuilder)
                ),
                server -> RestAssured.with()
                        .contentType(ContentType.URLENC)
                        .formParams("Allure", "Form")
                        .get(server.url("/hello")).then().statusCode(200),
                filter
        );

        assertThat(results.getAttachmentContentAsString(httpExchangeAttachment(results)))
                .contains(formattedBody);
    }

    @Test
    void shouldRenderListValuedFormParams() {
        final ResponseDefinitionBuilder responseBuilder = WireMock.aResponse()
                .withStatus(200)
                .withBody("some body");

        final AllureResults results = executeWithStub(
                server -> WireMock.stubFor(
                        WireMock.post(WireMock.urlPathEqualTo("/hello"))
                                .willReturn(responseBuilder)
                ),
                server -> RestAssured.given()
                        .contentType(ContentType.URLENC)
                        .formParam("data", List.of("a", "b"))
                        .post(server.url("/hello")).then().statusCode(200)
        );

        assertThat(allAttachmentNames(results))
                .containsExactly("HTTP exchange");

        assertThat(results.getAttachmentContentAsString(httpExchangeAttachment(results)))
                .contains("\"form\"")
                .contains("\"name\":\"data\"")
                .contains("\"value\":\"[a, b]\"");
    }

    @Test
    void shouldNotFailForNullValuedFormParamsMap() {
        final ResponseDefinitionBuilder responseBuilder = WireMock.aResponse()
                .withStatus(200)
                .withBody("some body");

        final Map<String, Object> formParams = new LinkedHashMap<>();
        formParams.put("param1", "value1");
        formParams.put("param2", null);

        final AllureResults results = executeWithStub(
                server -> WireMock.stubFor(
                        WireMock.post(WireMock.urlPathEqualTo("/hello"))
                                .willReturn(responseBuilder)
                ),
                server -> RestAssured.given()
                        .contentType(ContentType.URLENC)
                        .formParams(formParams)
                        .post(server.url("/hello")).then().statusCode(200)
        );

        assertThat(allAttachmentNames(results))
                .containsExactly("HTTP exchange");

        assertThat(results.getAttachmentContentAsString(httpExchangeAttachment(results)))
                .contains("\"name\":\"param1\",\"value\":\"value1\"")
                .contains("\"name\":\"param2\",\"value\":\"null\"");
    }

    @Test
    void shouldApplyHttpExchangeBuilderOptions() {
        final AllureRestAssured filter = step(
                "Configure REST Assured exchange builder redaction and response truncation",
                () -> new AllureRestAssured().configureHttpExchange(
                        builder -> builder
                                .redactCookie("sid")
                                .redactQueryParameter("token")
                                .redactFormParameter("secret")
                                .setMaxBodySize(4)
                )
        );

        final AllureResults results = step(
                "Capture REST Assured exchange using builder options",
                () -> executeWithStub(
                        server -> WireMock.stubFor(
                                WireMock.post(WireMock.urlPathEqualTo("/hello"))
                                        .willReturn(
                                                WireMock.aResponse()
                                                        .withStatus(200)
                                                        .withBody("response body")
                                        )
                        ),
                        server -> RestAssured.given()
                                .contentType(ContentType.URLENC)
                                .cookie("sid", "cookie-secret")
                                .queryParam("token", "query-secret")
                                .formParam("secret", "form-secret")
                                .post(server.url("/hello")).then().statusCode(200),
                        filter
                )
        );

        step(
                "Verify REST Assured exchange uses shared redaction and truncation", () -> assertThat(
                        results.getAttachmentContentAsString(httpExchangeAttachment(results))
                )
                        .contains("\"name\":\"sid\",\"value\":\"" + HttpExchange.REDACTED_VALUE + "\"")
                        .contains("\"name\":\"token\",\"value\":\"" + HttpExchange.REDACTED_VALUE + "\"")
                        .contains("\"name\":\"secret\",\"value\":\"" + HttpExchange.REDACTED_VALUE + "\"")
                        .contains("\"value\":\"resp\"")
                        .contains("\"size\":13")
                        .contains("\"truncated\":true")
        );
    }

    protected final AllureResults executeWithStub(final Consumer<WireMockServer> stubSetup,
                                                  final Consumer<WireMockServer> requestExecutor) {
        return executeWithStub(stubSetup, requestExecutor, new AllureRestAssured());
    }

    protected final AllureResults executeWithStub(final Consumer<WireMockServer> stubSetup,
                                                  final Consumer<WireMockServer> requestExecutor,
                                                  final AllureRestAssured filter) {
        final WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());

        return step("Execute REST Assured request with WireMock stub and collect Allure results", () -> runWithinTestContext(() -> {
            server.start();
            WireMock.configureFor(server.port());
            RestAssured.replaceFiltersWith(filter);

            stubSetup.accept(server);
            try {
                requestExecutor.accept(server);
            } finally {
                server.stop();
                RestAssured.replaceFiltersWith(ImmutableList.of());
                RestAssured.config = new RestAssuredConfig();
            }
        }));
    }

    private static Attachment httpExchangeAttachment(final AllureResults results) {
        final List<Attachment> attachments = results.getAttachmentsRecursively();

        assertThat(attachments).hasSize(1);
        return attachments.get(0);
    }

    private static List<String> allAttachmentNames(final AllureResults results) {
        return results.getAttachmentsRecursively().stream()
                .map(Attachment::getName)
                .toList();
    }
}
