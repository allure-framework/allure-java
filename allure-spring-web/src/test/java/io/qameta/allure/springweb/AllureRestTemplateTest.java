package io.qameta.allure.springweb;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * * Created by v.o.pavlov on 06.02.2019.
 */
@SuppressWarnings("unchecked")
public class AllureRestTemplateTest {

    static Stream<String> attachmentNameProvider() {
        return Stream.of("Request", "Response");
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
        RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        restTemplate.setInterceptors(Collections.singletonList(new AllureRestTemplate()));

        final WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());

        return runWithinTestContext(() -> {
            server.start();
            WireMock.configureFor(server.port());
            WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/hello")).willReturn(WireMock.aResponse().withBody("some body")));
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<JsonNode> entity = new HttpEntity<>(headers);
                ResponseEntity<String> result = restTemplate.exchange(server.url("/hello"), HttpMethod.GET, entity, String.class);
                Assertions.assertEquals(result.getStatusCode(), HttpStatus.OK);
            } finally {
                server.stop();
            }
        });
    }
}
