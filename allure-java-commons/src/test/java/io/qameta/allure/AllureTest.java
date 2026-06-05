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
package io.qameta.allure;

import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.ScopeResult;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.util.ObjectUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static io.qameta.allure.Allure.addAttachment;
import static io.qameta.allure.Allure.addByteAttachmentAsync;
import static io.qameta.allure.Allure.addHttpExchange;
import static io.qameta.allure.Allure.addStreamAttachmentAsync;
import static io.qameta.allure.Allure.attachment;
import static io.qameta.allure.Allure.description;
import static io.qameta.allure.Allure.descriptionHtml;
import static io.qameta.allure.Allure.epic;
import static io.qameta.allure.Allure.feature;
import static io.qameta.allure.Allure.getLifecycle;
import static io.qameta.allure.Allure.issue;
import static io.qameta.allure.Allure.label;
import static io.qameta.allure.Allure.link;
import static io.qameta.allure.Allure.parameter;
import static io.qameta.allure.Allure.step;
import static io.qameta.allure.Allure.tms;
import static io.qameta.allure.test.RunUtils.runTests;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static io.qameta.allure.test.TestData.randomName;
import static io.qameta.allure.test.ThreadLocalEnhancedRandom.current;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
@SuppressWarnings("unchecked")
class AllureTest {

    @Test
    void shouldAddSteps() {
        final AllureResults results = runWithinTestContext(
                () -> {
                    step("first", Status.PASSED);
                    step("second", Status.PASSED);
                    step("third", Status.FAILED);
                },
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("first", Status.PASSED),
                        tuple("second", Status.PASSED),
                        tuple("third", Status.FAILED)
                );
    }

    @Test
    void shouldCreateStepsFromLambdas() {
        final AllureResults results = runWithinTestContext(
                () -> {
                    step("first", () -> {
                    });
                    step("second", this::doSomething);
                    step("third", () -> fail("this step is failed"));
                },
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("first", Status.PASSED),
                        tuple("second", Status.PASSED),
                        tuple("third", Status.FAILED)
                );
    }

    void doSomething() {
    }

    @Test
    void shouldHideCheckedExceptions() {
        final AllureResults results = runWithinTestContext(
                () -> {
                    step("first", Status.PASSED);
                    step("second", () -> {
                        throw new Exception("something wrong");
                    });
                    step("third", Status.FAILED);
                },
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactly(Status.BROKEN);

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactly("something wrong");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("first", Status.PASSED),
                        tuple("second", Status.BROKEN)
                );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .filteredOn("name", "second")
                .extracting(StepResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactly("something wrong");
    }

    @Test
    void shouldAddLabels() {
        final Label first = current().nextObject(Label.class);
        final Label second = current().nextObject(Label.class);
        final Label third = current().nextObject(Label.class);

        final AllureResults results = runWithinTestContext(
                () -> getLifecycle().updateTestCase(testResult -> testResult.getLabels().addAll(asList(first, second, third))),
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .contains(third, first, second);
    }

    @Test
    void shouldAddParameter() {
        final Parameter first = current().nextObject(Parameter.class);
        final Parameter second = current().nextObject(Parameter.class);
        final Parameter third = current().nextObject(Parameter.class);

        final AllureResults results = runWithinTestContext(
                () -> {
                    parameter(first.getName(), first.getValue());
                    parameter(second.getName(), second.getValue());
                    parameter(third.getName(), third.getValue());
                },
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .contains(
                        tuple(first.getName(), first.getValue()),
                        tuple(second.getName(), second.getValue()),
                        tuple(third.getName(), third.getValue())
                );
    }

    @Test
    void shouldAddParameterWithModeAndExcluded() {
        final Parameter first = current().nextObject(Parameter.class);
        final Parameter second = current().nextObject(Parameter.class);
        final Parameter third = current().nextObject(Parameter.class);

        final AllureResults results = runWithinTestContext(
                () -> {
                    parameter(first.getName(), first.getValue(), first.getMode());
                    parameter(second.getName(), second.getValue(), second.getExcluded());
                    parameter(third.getName(), third.getValue(), third.getExcluded(), third.getMode());
                },
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded, Parameter::getMode)
                .contains(
                        tuple(first.getName(), first.getValue(), null, first.getMode()),
                        tuple(second.getName(), second.getValue(), second.getExcluded(), null),
                        tuple(third.getName(), third.getValue(), third.getExcluded(), third.getMode())
                );
    }

    @Test
    void shouldAddLinks() {
        final io.qameta.allure.model.Link first = current().nextObject(Link.class);
        final io.qameta.allure.model.Link second = current().nextObject(Link.class);
        final io.qameta.allure.model.Link third = current().nextObject(Link.class);

        final AllureResults results = runWithinTestContext(
                () -> {
                    link(first.getName(), first.getType(), first.getUrl());
                    link(second.getName(), second.getUrl());
                    link(third.getUrl());
                },
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType, Link::getUrl)
                .contains(
                        tuple(first.getName(), first.getType(), first.getUrl()),
                        tuple(second.getName(), null, second.getUrl()),
                        tuple(null, null, third.getUrl())
                );
    }

    @Test
    void shouldAddDescription() {
        final String description = randomName();

        final AllureResults results = runWithinTestContext(
                () -> description(description),
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getDescription)
                .containsExactly(description);
    }

    @Test
    void shouldAddDescriptionHtml() {
        final String descriptionHtml = randomName();

        final AllureResults results = runWithinTestContext(
                () -> descriptionHtml(descriptionHtml),
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getDescriptionHtml)
                .containsExactly(descriptionHtml);
    }

    @Test
    void shouldApplyRuntimeMetadataFromBeforeFixtureScope() {
        final String scopeUuid = randomName();
        final String fixtureUuid = randomName();
        final String testUuid = randomName();
        final String description = randomName();

        final AllureResults results = runTests(
                lifecycle -> {
                    lifecycle.startScope(new ScopeResult().setUuid(scopeUuid));
                    lifecycle.startBeforeFixture(scopeUuid, fixtureUuid, new FixtureResult().setName("before"));
                    label("layer", "api");
                    parameter("browser", "chrome");
                    link("docs", "https://example.com/docs");
                    description(description);
                    lifecycle.stopFixture(fixtureUuid);

                    lifecycle.scheduleTestCase(scopeUuid, new TestResult().setUuid(testUuid).setName("test"));
                    lifecycle.startTestCase(testUuid);
                    lifecycle.stopTestCase(testUuid);
                    lifecycle.writeTestCase(testUuid);
                },
                Allure::setLifecycle
        );

        final TestResult result = results.getTestResults().get(0);
        assertThat(result.getLabels())
                .extracting(Label::getName, Label::getValue)
                .containsExactly(tuple("layer", "api"));
        assertThat(result.getParameters())
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactly(tuple("browser", "chrome"));
        assertThat(result.getLinks())
                .extracting(Link::getName, Link::getUrl)
                .containsExactly(tuple("docs", "https://example.com/docs"));
        assertThat(result.getDescription())
                .isEqualTo(description);
    }

    @Test
    void shouldWrapRuntimeAttachmentsWithMetaSteps() {
        final AllureResults results = step(
                "Capture every high-level runtime attachment API inside a test context",
                () -> runWithinTestContext(
                        () -> {
                            step("step 1");
                            attachment("attach", "body");
                            addAttachment("typed", "application/json", "{}");
                            addAttachment("file", "text/csv", "a,b", ".csv");
                            addAttachment("stream", new ByteArrayInputStream(
                                    "stream".getBytes(StandardCharsets.UTF_8)
                            ));
                            addByteAttachmentAsync(
                                    "bytes async",
                                    "application/octet-stream",
                                    ".bin",
                                    () -> new byte[]{1}
                            )
                                    .join();
                            addStreamAttachmentAsync(
                                    "stream async",
                                    "text/plain",
                                    ".txt",
                                    () -> new ByteArrayInputStream("async".getBytes(StandardCharsets.UTF_8))
                            ).join();
                            step("step 2");
                        },
                        Allure::setLifecycle
                )
        );

        final TestResult testResult = results.getTestResults().get(0);

        step("Verify runtime attachments are written as ordered meta-steps", () -> {
            assertThat(testResult.getSteps())
                    .extracting(StepResult::getName, StepResult::getStatus)
                    .containsExactly(
                            tuple("step 1", Status.PASSED),
                            tuple("attach", Status.PASSED),
                            tuple("typed", Status.PASSED),
                            tuple("file", Status.PASSED),
                            tuple("stream", Status.PASSED),
                            tuple("bytes async", Status.PASSED),
                            tuple("stream async", Status.PASSED),
                            tuple("step 2", Status.PASSED)
                    );

            assertThat(testResult.getAttachments())
                    .isEmpty();

            assertThat(testResult.getSteps().subList(1, 7))
                    .allSatisfy(step -> assertThat(step.getAttachments()).hasSize(1));
            assertThat(testResult.getSteps().get(1).getAttachments())
                    .extracting(Attachment::getName, Attachment::getType)
                    .containsExactly(tuple("attach", "text/plain"));
        });
    }

    @Test
    void shouldWrapHttpExchangeAttachmentsWithMetaSteps() {
        final HttpExchange exchange = HttpExchange
                .builder(HttpExchangeRequest.builder("GET", "https://example.com/api").build())
                .build();

        final AllureResults results = step(
                "Capture HTTP exchange attachment inside a test context",
                () -> runWithinTestContext(
                        () -> {
                            step("step 1");
                            addHttpExchange("HTTP exchange", exchange);
                            step("step 2");
                        },
                        Allure::setLifecycle
                )
        );

        final TestResult testResult = results.getTestResults().get(0);
        final StepResult attachmentStep = testResult.getSteps().get(1);

        step("Verify HTTP exchange attachment is written as an ordered meta-step", () -> {
            assertThat(testResult.getSteps())
                    .extracting(StepResult::getName, StepResult::getStatus)
                    .containsExactly(
                            tuple("step 1", Status.PASSED),
                            tuple("HTTP exchange", Status.PASSED),
                            tuple("step 2", Status.PASSED)
                    );

            assertThat(attachmentStep.getAttachments())
                    .extracting(Attachment::getName, Attachment::getType)
                    .containsExactly(tuple("HTTP exchange", HttpExchange.CONTENT_TYPE));

            final Attachment attachment = attachmentStep.getAttachments().get(0);
            assertThat(attachment.getSource())
                    .endsWith(HttpExchange.FILE_EXTENSION);
            assertThat(results.getAttachments())
                    .containsKey(attachment.getSource());
        });
    }

    @Test
    void shouldSupportNewJavaApi() {
        final AllureResults results = runWithinTestContext(
                this::simpleTest,
                Allure::setLifecycle
        );

        assertThat(results.getTestResults())
                .hasSize(1);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("set up test database", Status.SKIPPED),
                        tuple("set up test create mocks", Status.PASSED),
                        tuple("authorization", Status.PASSED),
                        tuple("preparation checks", Status.PASSED),
                        tuple("dynamic name ABC", Status.PASSED),
                        tuple("get data", Status.PASSED)
                );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .filteredOn("name", "get data")
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("build client", Status.PASSED),
                        tuple("run request", Status.PASSED),
                        tuple("response", Status.PASSED)
                );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .filteredOn("name", "get data")
                .flatExtracting(StepResult::getSteps)
                .filteredOn("name", "run request")
                .flatExtracting(StepResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactly(
                        tuple("authorization", "Basic admin:admin"),
                        tuple("url", "https://example.com/getData"),
                        tuple("requestBody", "[1, 2, 3]")
                );
    }

    void simpleTest() {
        // Add test links
        link("testing", "https://example.com");
        issue("GH-123", "https://github.com/allure-framework/allure2/issues/123");
        tms("AS-182", "https://allureee.qameta.io/project/1/test-cases/182");

        // Add test labels
        epic("Allure Java API");
        feature("Dynamic API");
        label("component", "allure-java-commons");

        // Add parameters to test within test body
        String baseUrl = parameter("baseUrl", "https://example.com/getData");

        // Log-style steps
        step("set up test database", Status.SKIPPED);
        step("set up test create mocks"); // Status.PASSED by default

        // Add parameters to test inside steps as well
        String token = step("authorization", () -> {
            String login = parameter("login", "admin");
            String password = parameter("password", "admin");
            return getAuth(login, password);
        });

        // Add parameters to step using injected StepContext
        step("preparation checks", (step) -> {
            step.parameter("a", "a value");
            step.parameter("b", "b value");
        });

        // Nested step and dynamic step name
        step((step) -> {
            String a = step("child 1", () -> "A");
            String b = step("child b", () -> "B");
            String c = step("child b", () -> "C");

            step.name("dynamic name " + a + b + c);
        });

        // Create attachments as well as steps
        step("get data", () -> {
            step("build client");
            List<String> responseData = step("run request", (step) -> {
                step.parameter("authorization", token);
                step.parameter("url", baseUrl);
                int[] requestBody = step.parameter("requestBody", new int[]{1, 2, 3});

                return getData(baseUrl, token, requestBody);
            });
            attachment("response", ObjectUtils.toString(responseData));
        });
    }

    List<String> getData(final String url, final String token, final Object body) {
        return Collections.emptyList();
    }

    String getAuth(final String login, final String password) {
        return String.format("Basic %s:%s", login, password);
    }
}
