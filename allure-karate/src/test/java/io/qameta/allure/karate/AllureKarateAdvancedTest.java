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

import io.karatelabs.common.Json;
import io.karatelabs.core.ScenarioResult;
import io.qameta.allure.Description;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.junitplatform.AllureJunitPlatform;
import io.qameta.allure.karate.features.KarateJunit6Tests;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static io.qameta.allure.model.Status.BROKEN;
import static io.qameta.allure.model.Status.PASSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals", "PMD.JUnitTestContainsTooManyAsserts"})
class AllureKarateAdvancedTest extends TestRunner {

    /**
     * Protects Karate's privacy contract: a suppressed scenario keeps only its outcome and safe identity.
     */
    @Test
    @Description
    void shouldSuppressReportDisabledScenarioDetails() {
        final AllureResults results = runApi("classpath:testdata/report-disabled.feature");
        final TestResult testResult = results.getTestResultByName("Suppressed failure");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Suppressed failure", BROKEN));
        assertThat(testResult.getStatus()).isEqualTo(BROKEN);
        assertThat(testResult.getDescription()).isNull();
        assertThat(testResult.getSteps()).isEmpty();
        assertThat(testResult.getParameters()).isEmpty();
        assertThat(testResult.getStatusDetails())
                .extracting(details -> details.getMessage(), details -> details.getTrace())
                .containsExactly(ScenarioResult.SUPPRESSED_FAILURE_MESSAGE, null);
        assertThat(results.getAttachments()).isEmpty();
    }

    /**
     * Ensures an explicitly suppressed callee cannot leak its failure through a visible caller's result or call step.
     */
    @Test
    @Description
    void shouldRedactReportDisabledCalledFailureFromCaller() {
        final AllureResults results = run("classpath:testdata/report-disabled-caller.feature");
        final TestResult caller = results.getTestResultByName("Caller of report-disabled feature");
        final StepResult call = directStep(
                caller,
                "call read('classpath:testdata/called-report-disabled.feature')"
        );

        assertThat(caller.getStatus()).isEqualTo(BROKEN);
        assertThat(caller.getStatusDetails())
                .extracting(details -> details.getMessage(), details -> details.getTrace())
                .containsExactly(ScenarioResult.SUPPRESSED_FAILURE_MESSAGE, null);
        assertThat(call.getStatus()).isEqualTo(BROKEN);
        assertThat(call.getStatusDetails())
                .extracting(details -> details.getMessage(), details -> details.getTrace())
                .containsExactly(ScenarioResult.SUPPRESSED_FAILURE_MESSAGE, null);
        assertThat(call.getSteps()).isEmpty();
        assertThat(results.getAttachments()).isEmpty();
    }

    /**
     * Ensures Allure's rich HTTP artifact applies Karate's complete configured mask before serialization.
     */
    @Test
    @AllureFeatures.Attachments
    @Description
    void shouldApplyConfiguredKarateMaskToHttpExchange() {
        final AllureResults results = runApi("classpath:testdata/http-masking.feature");
        final Attachment attachment = findStep(
                results.getTestResultByName("Configured HTTP mask"),
                "method post"
        ).getAttachments().get(0);

        final String exchange = results.getAttachmentContentAsString(attachment);
        assertThat(exchange)
                .contains("\"name\":\"X-Api-Key\",\"value\":\"[MASKED]\"")
                .contains("MASKED-NAME")
                .contains("[MASKED]")
                .doesNotContain("private-api-key")
                .doesNotContain("private-body-value")
                .doesNotContain("Soul");
    }

    /**
     * Protects the result-count fix without sacrificing evidence produced by called and nested features.
     */
    @Test
    @AllureFeatures.Attachments
    @Description
    void shouldKeepCalledFeatureEvidenceUnderCallerResult() {
        final AllureResults results = runApi("classpath:testdata/call-callonce.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Main Scenario with a call", PASSED));

        final TestResult caller = results.getTestResults().get(0);
        final StepResult call = directStep(caller, "call read('classpath:testdata/call-target.feature')");
        final StepResult calledScenario = directStep(call, "Called feature: Called scenario");
        assertThat(call.getStatus()).isEqualTo(PASSED);
        assertThat(calledScenario.getSteps())
                .extracting(StepResult::getName)
                .containsExactly(
                        "eval",
                        "url karate.properties['mock.server.url']",
                        "path '/called'",
                        "method get",
                        "status 200",
                        "call read('classpath:testdata/nested-call-target.feature')"
                );
        assertAttachment(results, findStep(calledScenario, "eval"), "called.txt", "called evidence");
        assertThat(
                results.getAttachmentContentAsString(
                        findStep(calledScenario, "method get").getAttachments().get(0)
                )
        ).contains("/called");

        final StepResult nestedCall = directStep(
                calledScenario,
                "call read('classpath:testdata/nested-call-target.feature')"
        );
        final StepResult nestedScenario = directStep(nestedCall, "Nested called feature: Nested called scenario");
        assertAttachment(results, findStep(nestedScenario, "eval"), "nested.txt", "nested evidence");

        final StepResult callonce = directStep(
                caller,
                "callonce read('classpath:testdata/callonce-target.feature')"
        );
        final StepResult callonceScenario = directStep(callonce, "Callonce feature: Callonce scenario");
        assertAttachment(results, findStep(callonceScenario, "eval"), "callonce.txt", "callonce evidence");
    }

    /**
     * Exercises the listener with four concurrently running scenarios and unique evidence in each result.
     */
    @Test
    @AllureFeatures.Attachments
    @Description
    void shouldKeepParallelScenarioEvidenceIsolated() {
        final AllureResults results = runApi(4, "classpath:testdata/parallel-evidence.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Parallel one", PASSED),
                        tuple("Parallel two", PASSED),
                        tuple("Parallel three", PASSED),
                        tuple("Parallel four", PASSED)
                );
        assertParallelEvidence(results, "Parallel one", "one");
        assertParallelEvidence(results, "Parallel two", "two");
        assertParallelEvidence(results, "Parallel three", "three");
        assertParallelEvidence(results, "Parallel four", "four");
    }

    /**
     * Protects Karate 2's multi-asset embed contract and Allure's rich image-diff rendering.
     */
    @Test
    @AllureFeatures.Attachments
    @Description
    void shouldPreserveMultipartEmbeds() {
        final AllureResults results = run("classpath:testdata/multipart-attachments.feature");
        final List<Attachment> attachments = findStep(
                results.getTestResultByName("Rich multipart attachments"),
                "eval"
        ).getAttachments();

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactly(
                        tuple("visual comparison", "application/vnd.allure.image.diff"),
                        tuple("visual comparison metadata", "application/json"),
                        tuple("multi evidence [request]", "text/plain"),
                        tuple("multi evidence [reference]", "text/uri-list"),
                        tuple("multi evidence metadata", "application/json")
                );

        final Map<String, Object> imageDiff = Json.of(
                Json.parseStrict(results.getAttachmentContentAsString(attachments.get(0)))
        ).asMap();
        assertThat(imageDiff)
                .containsEntry("expected", "data:image/png;base64,YmFzZWxpbmUtYnl0ZXM=")
                .containsEntry("actual", "data:image/png;base64,Y3VycmVudC1ieXRlcw==")
                .containsEntry("diff", "data:image/png;base64,ZGlmZi1ieXRlcw==");
        assertThat(results.getAttachmentContentAsString(attachments.get(1))).contains("\"threshold\":0.1");
        assertThat(results.getAttachmentContentAsString(attachments.get(2))).isEqualTo("request evidence");
        assertThat(results.getAttachmentContentAsString(attachments.get(3)))
                .isEqualTo("ext/image/reference.png\n");
        assertThat(results.getAttachmentContentAsString(attachments.get(4)))
                .contains("\"source\":\"karate extension\"");
    }

    /**
     * Cross-module smoke coverage with the actual Karate JUnit 6 implementation and both Allure listeners.
     */
    @Test
    @Description
    void shouldAvoidDuplicateResultsForRealKarateJunit6Launch() {
        final AllureResults results = RunUtils.runTests(lifecycle -> {
            final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(KarateJunit6Tests.class))
                    .build();
            final LauncherConfig config = LauncherConfig.builder()
                    .enableTestExecutionListenerAutoRegistration(false)
                    .enablePostDiscoveryFilterAutoRegistration(false)
                    .addTestExecutionListeners(new AllureJunitPlatform(lifecycle))
                    .build();
            final Launcher launcher = LauncherFactory.create(config);
            launcher.execute(request);
        });

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Karate JUnit 6 smoke scenario", PASSED),
                        tuple("ordinaryJupiterTest()", PASSED)
                );
    }

    private static void assertParallelEvidence(final AllureResults results,
                                               final String scenarioName,
                                               final String id) {
        final TestResult testResult = results.getTestResultByName(scenarioName);
        final StepResult embedStep = testResult.getSteps().stream()
                .filter(step -> step.getName().startsWith("eval karate.embed"))
                .findFirst()
                .orElseThrow();
        assertAttachment(results, embedStep, "parallel-" + id + ".txt", "payload-" + id);

        final StepResult method = findStep(testResult, "method get");
        assertThat(method.getAttachments())
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactly(tuple("HTTP exchange", HttpExchange.CONTENT_TYPE));
        assertThat(results.getAttachmentContentAsString(method.getAttachments().get(0)))
                .contains("/parallel/" + id);
    }

    private static void assertAttachment(final AllureResults results,
                                         final StepResult step,
                                         final String name,
                                         final String content) {
        assertThat(step.getAttachments())
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactly(tuple(name, "text/plain"));
        assertThat(results.getAttachmentContentAsString(step.getAttachments().get(0))).isEqualTo(content);
    }

    private static StepResult directStep(final TestResult result, final String name) {
        return result.getSteps().stream()
                .filter(step -> Objects.equals(step.getName(), name))
                .findFirst()
                .orElseThrow();
    }

    private static StepResult directStep(final StepResult result, final String name) {
        return result.getSteps().stream()
                .filter(step -> Objects.equals(step.getName(), name))
                .findFirst()
                .orElseThrow();
    }

    private static StepResult findStep(final TestResult result, final String name) {
        return result.getSteps().stream()
                .flatMap(AllureKarateAdvancedTest::flatten)
                .filter(step -> Objects.equals(step.getName(), name))
                .findFirst()
                .orElseThrow();
    }

    private static StepResult findStep(final StepResult result, final String name) {
        return flatten(result)
                .filter(step -> Objects.equals(step.getName(), name))
                .findFirst()
                .orElseThrow();
    }

    private static Stream<StepResult> flatten(final StepResult step) {
        return Stream.concat(Stream.of(step), step.getSteps().stream().flatMap(AllureKarateAdvancedTest::flatten));
    }
}
