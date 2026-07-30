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
package io.qameta.allure.karate;

import io.karatelabs.core.Runner;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.FileSystemResultsWriter;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static io.qameta.allure.model.Status.BROKEN;
import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static io.qameta.allure.test.AllureTestCommonsUtils.expectedHistoryId;
import static io.qameta.allure.util.ResultsUtils.md5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
class AllureKarateTest extends TestRunner {

    @Test
    void shouldCreateNameAndFullName() {
        final AllureResults results = run("classpath:testdata/description-and-name.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getFullName)
                .containsExactlyInAnyOrder(
                        tuple(
                                "Some api* request # comment 1",
                                "testdata/description-and-name.feature:3"
                        ),
                        tuple(
                                "testdata/description-and-name.feature:8",
                                "testdata/description-and-name.feature:8"
                        )
                );
    }

    @Test
    void shouldCreateDescription() {
        final AllureResults results = run("classpath:testdata/description-and-name.feature");
        assertThat(results.getTestResults())
                .extracting(TestResult::getDescription, TestResult::getDescriptionHtml)
                .containsExactlyInAnyOrder(
                        tuple(
                                "Request '//user' & get 20* code, ...",
                                null
                        ),
                        tuple(
                                "",
                                null
                        )
                );
    }

    @Test
    void shouldCreateStartAndStopTimeslots() {
        final AllureResults results = runApi("classpath:testdata/api.feature");

        final TestResult tr1 = results.getTestResults().get(0);
        final TestResult tr2 = results.getTestResults().get(1);

        assertThat(tr2.getStop())
                .isGreaterThan(tr2.getStart())
                .isGreaterThan(tr1.getStop());
    }

    @Test
    void shouldCreateStatusAndStage() {
        final AllureResults results = run("classpath:testdata/api.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Simple post request")
                .extracting(TestResult::getStatus, TestResult::getStage)
                .containsExactlyInAnyOrder(
                        tuple(BROKEN, Stage.FINISHED)
                );
    }

    @Test
    void shouldNotCreateStatusDetailsIfTestPassed() {
        final AllureResults results = runApi("classpath:testdata/api.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Simple get request")
                .extracting(TestResult::getStatus, TestResult::getStatusDetails)
                .containsExactlyInAnyOrder(
                        tuple(PASSED, null)
                );
    }

    @Test
    void shouldCreateStatusDetailsIfTestFailed() {
        final AllureResults results = runApi("classpath:testdata/api.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Simple post request")
                .extracting(
                        TestResult::getStatus,
                        result -> result.getStatusDetails().getMessage(),
                        result -> result.getStatusDetails().getTrace().lines().findFirst().orElse("")
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                FAILED,
                                "expected status: 200, actual: 401",
                                "java.lang.AssertionError: expected status: 200, actual: 401"
                        )
                );
    }

    @Test
    void shouldCreateTestCaseIdAndName() {
        final AllureResults results = run("classpath:testdata/description-and-name.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getTestCaseId, TestResult::getTestCaseName)
                .containsExactlyInAnyOrder(
                        tuple(md5("testdata/description-and-name.feature:Some api* request # comment 1"), null),
                        tuple(md5("testdata/description-and-name.feature:8"), null)
                );
        assertThat(results.getTestResults())
                .allSatisfy(result -> {
                    assertThat(result.getParameters()).isEmpty();
                    assertThat(result.getHistoryId())
                            .isEqualTo(expectedHistoryId(result.getTestCaseId(), result.getParameters()));
                });
    }

    @Test
    void shouldCreateTestCaseIdAndNamesOfParametrizedTest() {
        final AllureResults results = runApi("classpath:testdata/parametrized-test.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getTestCaseId)
                .containsExactlyInAnyOrder(
                        tuple("/login should return 200", md5("testdata/parametrized-test.feature:/login should return 200")),
                        tuple("/user should return 301", md5("testdata/parametrized-test.feature:/user should return 301")),
                        tuple("/pages should return 404", md5("testdata/parametrized-test.feature:/pages should return 404"))
                );
    }

    @Test
    void shouldCreateParamsForParametrizedTest() {
        final AllureResults results = runApi("classpath:testdata/parametrized-test.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "/login should return 200")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("path", "login"),
                        tuple("status", "200")
                );
    }

    @Test
    void shouldCreateHistoryIdAndNamesOfParametrizedTest() {
        final AllureResults results = runApi("classpath:testdata/parametrized-test.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "/login should return 200",
                        "/user should return 301",
                        "/pages should return 404"
                );
        assertThat(results.getTestResults())
                .allSatisfy(
                        result -> assertThat(result.getHistoryId())
                                .isEqualTo(expectedHistoryId(result.getTestCaseId(), result.getParameters()))
                );
        assertThat(results.getTestResults())
                .extracting(TestResult::getHistoryId)
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldCalculateIdsFromFinalNativeAndRuntimeParameters() {
        final AllureResults results = run("classpath:testdata/runtime-api.feature");

        final TestResult testResult = results.getTestResults().get(0);
        assertThat(testResult.getParameters())
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded)
                .containsExactlyInAnyOrder(
                        tuple("native", "example", null),
                        tuple("runtime", "value", null),
                        tuple("excluded", "ignored", true)
                );
        assertThat(testResult.getTestCaseId())
                .isEqualTo(md5("testdata/runtime-api.feature:Runtime parameters for example"));
        assertThat(testResult.getHistoryId())
                .isEqualTo(expectedHistoryId(testResult.getTestCaseId(), testResult.getParameters()));
    }

    @Test
    void shouldCreateLabels() {
        final AllureResults results = run("classpath:testdata/tags.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Test with labels")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .containsExactlyInAnyOrder(
                        tuple("feature", "labels"),
                        tuple("epic", "epic1"),
                        tuple("story", "story1"),
                        tuple("tag", "some_tag")
                );
    }

    @Test
    void shouldCreateSpecialLabels() {
        final AllureResults results = run("classpath:testdata/tags.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Test with owner, id and layer")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .containsExactlyInAnyOrder(
                        tuple("feature", "labels"),
                        tuple("AS_ID", "141413"),
                        tuple("owner", "npolly"),
                        tuple("layer", "unit_tests"),
                        tuple("severity", "blocker")
                );
    }

    @Test
    void shouldNotCreateTagLabel() {
        final AllureResults results = run("classpath:testdata/tags.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Test without allure labels")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .containsExactly(
                        tuple("feature", "labels")
                );
    }

    @Test
    void shouldCreateLinks() {
        final AllureResults results = run("classpath:testdata/links.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Test with links")
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType)
                .containsExactlyInAnyOrder(
                        tuple("http://localhost:8080", "custom"),
                        tuple("http://localhost:8080", "tms"),
                        tuple("http://localhost:8080", "issue")
                );
    }

    @Test
    void shouldCreateApiTestSteps() {
        final AllureResults results = runApi("classpath:testdata/steps.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "f1 - s1")
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactlyInAnyOrder(
                        "print 'first feature:@smoke, first scenario'",
                        "url karate.properties['mock.server.url']",
                        "path '/login'",
                        "method get",
                        "status 200"
                );
    }

    @Test
    void shouldCreateResultWithEmptySteps() {
        final AllureResults results = runApi("classpath:testdata/steps.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "f1 - s2")
                .flatExtracting(TestResult::getSteps)
                .isEmpty();
    }

    @Test
    void shouldCreateStepsStatuses() {
        final AllureResults results = run("classpath:testdata/steps.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "f1 - s1")
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getStatus)
                .containsExactly(
                        PASSED,
                        PASSED,
                        PASSED,
                        BROKEN
                );
    }

    @AllureFeatures.Attachments
    @Test
    void shouldCreateAttachmentForFailedStep() {
        final AllureResults results = run("classpath:testdata/failed-attachment.feature");
        final TestResult testResult = results.getTestResultByName("Failed step attachment");

        assertThat(testResult.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(tuple("eval", BROKEN));

        final List<Attachment> attachments = testResult.getSteps().get(0).getAttachments();
        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactly(tuple("failure-context.txt", "text/plain"));

        final Attachment attachment = attachments.get(0);
        assertThat(attachment.getSource()).endsWith(".txt");
        assertThat(results.getAttachmentContentAsString(attachment)).isEqualTo("failure context");
    }

    @AllureFeatures.Attachments
    @Test
    void shouldCreateAttachments() {
        final AllureResults results = run("classpath:testdata/attachments.feature");
        final TestResult testResult = results.getTestResultByName("Named attachments");

        assertThat(testResult.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(tuple("eval", PASSED));

        final List<Attachment> attachments = testResult.getSteps().get(0).getAttachments();

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactly(
                        tuple("notes.txt", "text/plain"),
                        tuple("payload.json", "application/json")
                );

        assertThat(attachments)
                .extracting(Attachment::getSource)
                .allSatisfy(source -> assertThat(results.getAttachments()).containsKey(source));

        final List<String> attachmentContents = attachments.stream()
                .map(results::getAttachmentContentAsString)
                .toList();
        assertThat(attachmentContents)
                .containsExactly("plain attachment", "{\"status\":\"ok\"}");
    }

    @AllureFeatures.Attachments
    @Test
    void shouldCreateHttpRequestAndResponseAttachment() {
        final AllureResults results = runApi("classpath:testdata/http-attachments.feature");
        final TestResult testResult = results.getTestResultByName("HTTP request and response attachment");
        final StepResult methodStep = testResult.getSteps().stream()
                .filter(step -> "method post".equals(step.getName()))
                .findFirst()
                .orElseThrow();

        assertThat(methodStep.getAttachments())
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactly(tuple("HTTP exchange", HttpExchange.CONTENT_TYPE));

        final Attachment attachment = methodStep.getAttachments().get(0);
        assertThat(attachment.getSource()).endsWith(HttpExchange.FILE_EXTENSION);
        assertThat(results.getAttachments()).containsKey(attachment.getSource());

        final String exchange = results.getAttachmentContentAsString(attachment);
        assertThat(exchange)
                .contains("\"schemaVersion\":1")
                .contains("\"method\":\"POST\"")
                .contains("/users/login")
                .contains("\"name\":\"X-Request-Id\",\"value\":\"karate-http-attachment\"")
                .contains("\\\"username\\\":\\\"Soul\\\"")
                .contains("\"status\":200")
                .contains("\\\"message\\\":\\\"User logged in\\\"")
                .contains(HttpExchange.REDACTED_VALUE)
                .doesNotContain("Bearer secret");
    }

    @Test
    void buildTest() {
        final Path allureResults = temp.resolve("allure-results");

        Allure.step("Run Karate builder with Allure listener", () -> {
            Runner.builder()
                    .path("classpath:testdata/greeting.feature")
                    .listener(
                            new AllureKarate(
                                    new AllureLifecycle(
                                            new FileSystemResultsWriter(allureResults)
                                    )
                            )
                    )
                    .backupOutputDir(false)
                    .outputDir(temp.resolve("karate-reports"))
                    .outputJunitXml(false)
                    .outputCucumberJson(false)
                    .outputHtmlReport(false)
                    .parallel(1);
        });

        assertThat(allureResults)
                .isDirectoryContaining(path -> path.getFileName().toString().endsWith("-result.json"));
    }
}
