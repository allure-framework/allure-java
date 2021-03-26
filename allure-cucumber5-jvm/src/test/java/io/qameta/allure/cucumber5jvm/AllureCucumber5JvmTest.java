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
package io.qameta.allure.cucumber5jvm;

import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.feature.FeatureWithLines;
import io.cucumber.core.resource.ClassLoaders;
import io.cucumber.core.runtime.FeaturePathFeatureSupplier;
import io.cucumber.core.runtime.Runtime;
import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.runtime.TimeServiceEventBus;
import io.github.glytching.junit.extension.system.SystemProperty;
import io.github.glytching.junit.extension.system.SystemPropertyExtension;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.PACKAGE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.SUITE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TEST_CLASS_LABEL_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unchecked")
class AllureCucumber5JvmTest {

    @AllureFeatures.Base
    @Test
    void shouldSetName() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Add a to b");
    }

    @AllureFeatures.PassedTests
    @Test
    void shouldSetStatus() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.PASSED);
    }

    @AllureFeatures.FailedTests
    @Test
    void shouldSetFailedStatus() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/failed.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED);
    }

    @AllureFeatures.FailedTests
    @Test
    void shouldSetStatusDetails() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/failed.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactlyInAnyOrder("\n"
                        + "Expecting:\n"
                        + " <15>\n"
                        + "to be equal to:\n"
                        + " <123>\n"
                        + "but was not.");
    }

    @AllureFeatures.BrokenTests
    @Test
    void shouldSetBrokenStatus() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/broken.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.BROKEN);
    }

    @AllureFeatures.Stages
    @Test
    void shouldSetStage() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStage)
                .containsExactlyInAnyOrder(Stage.FINISHED);
    }

    @AllureFeatures.Timings
    @Test
    void shouldSetStart() {
        final long before = Instant.now().toEpochMilli();
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.Timings
    @Test
    void shouldSetStop() {
        final long before = Instant.now().toEpochMilli();
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.FullName
    @Test
    void shouldSetFullName() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getFullName)
                .containsExactlyInAnyOrder("Simple feature: Add a to b");
    }

    @AllureFeatures.Descriptions
    @Test
    void shouldSetDescription() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/description.feature");

        final String expected = "This is description for current feature.\n"
                + "It should appear on each scenario in report";

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getDescription)
                .containsExactlyInAnyOrder(
                        expected,
                        expected
                );
    }

    @AllureFeatures.Descriptions
    @Test
    void shouldSetScenarioDescription() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/scenario_description.feature");

        final String expected = "This is description for current feature.\n"
                + "It should appear on each scenario in report";

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getDescription)
                .containsExactlyInAnyOrder(
                        expected + "\n    scenario description 1",
                        expected + "\n    scenario description 2"
                );
    }

    @AllureFeatures.Attachments
    @Test
    void shouldAddDataTableAttachment() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/datatable.feature");

        final List<Attachment> attachments = writer.getTestResults().stream()
                .map(TestResult::getSteps)
                .flatMap(Collection::stream)
                .map(StepResult::getAttachments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactlyInAnyOrder(
                        tuple("Data table", "text/tab-separated-values")
                );

        final Attachment dataTableAttachment = attachments.iterator().next();
        final Map<String, byte[]> attachmentFiles = writer.getAttachments();
        assertThat(attachmentFiles)
                .containsKeys(dataTableAttachment.getSource());

        final byte[] bytes = attachmentFiles.get(dataTableAttachment.getSource());
        final String attachmentContent = new String(bytes, StandardCharsets.UTF_8);

        assertThat(attachmentContent)
                .isEqualTo("name\tlogin\temail\n" +
                        "Viktor\tclicman\tclicman@ya.ru\n" +
                        "Viktor2\tclicman2\tclicman2@ya.ru\n"
                );

    }

    @AllureFeatures.Attachments
    @Test
    void shouldAddAttachments() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/attachments.feature");

        final List<Attachment> attachments = writer.getTestResults().stream()
                .map(TestResult::getSteps)
                .flatMap(Collection::stream)
                .map(StepResult::getAttachments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactlyInAnyOrder(
                        tuple("Text output", "text/plain"),
                        tuple("Screenshot", null)
                );

        final List<String> attachmentContents = writer.getAttachments().values().stream()
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .collect(Collectors.toList());

        assertThat(attachmentContents)
                .containsExactlyInAnyOrder("text attachment", "image attachment");
    }

    @AllureFeatures.Steps
    @Test
    void shouldAddBackgroundSteps() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/background.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "Given  cat is sad",
                        "And  cat is murmur",
                        "When  Pet the cat",
                        "Then  Cat is happy"
                );
    }

    @AllureFeatures.Parameters
    @Test
    void shouldAddParametersFromExamples() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/examples.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .hasSize(2);

        assertThat(testResults.get(0).getParameters())
                .hasSize(3);

        assertThat(testResults.get(1).getParameters())
                .hasSize(3);

        assertThat(testResults)
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("a", "1"), tuple("b", "3"), tuple("result", "4"),
                        tuple("a", "2"), tuple("b", "4"), tuple("result", "6")
                );

    }

    @AllureFeatures.Parameters
    @Test
    void shouldHandleMultipleExamplesPerOutline() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/multi-examples.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .hasSize(2);

        assertThat(testResults)
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("a", "1"), tuple("b", "3"), tuple("result", "4"),
                        tuple("a", "2"), tuple("b", "4"), tuple("result", "6")
                );
    }

    @AllureFeatures.Parameters
    @Test
    void shouldSupportTaggedExamplesBlocks() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/multi-examples.feature", "--tags", "@ExamplesTag2");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .hasSize(1);

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("tag", "ExamplesTag2")
                );

        assertThat(testResults)
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("a", "2"), tuple("b", "4"), tuple("result", "6")
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldAddTags() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("tag", "FeatureTag"),
                        tuple("tag", "good")
                );
    }

    @AllureFeatures.Links
    @ExtendWith(SystemPropertyExtension.class)
    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @Test
    void shouldAddLinks() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType, Link::getUrl)
                .contains(
                        tuple("OAT-4444", "tms", "https://example.org/tms/OAT-4444"),
                        tuple("BUG-22400", "issue", "https://example.org/issue/BUG-22400")
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldAddBddLabels() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("feature", "Test Simple Scenarios"),
                        tuple("story", "Add a to b")
                );
    }

    @AllureFeatures.Timeline
    @Test
    void shouldAddThreadHostLabels() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .contains("host", "thread");
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldAddCommonLabels() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(PACKAGE_LABEL_NAME, "src.test.resources.features.tags_feature.Test Simple Scenarios"),
                        tuple(SUITE_LABEL_NAME, "Test Simple Scenarios"),
                        tuple(TEST_CLASS_LABEL_NAME, "Add a to b")
                );
    }

    @AllureFeatures.Steps
    @Test
    void shouldProcessUndefinedSteps() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/undefined.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.SKIPPED);

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 5", Status.PASSED),
                        tuple("When  step is undefined", null),
                        tuple("Then  b is 10", Status.SKIPPED)
                );
    }

    @AllureFeatures.SkippedTests
    @AllureFeatures.Steps
    @Test
    void shouldProcessPendingExceptionsInSteps() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/pending.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.SKIPPED);

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 5", Status.PASSED),
                        tuple("When  step is yet to be implemented", Status.SKIPPED),
                        tuple("Then  b is 10", Status.SKIPPED)
                );
    }

    @AllureFeatures.Base
    @Test
    void shouldSupportDryRunForSimpleFeatures() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature", "--dry-run");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Add a to b", Status.SKIPPED)
                );

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 5", Status.SKIPPED),
                        tuple("And  b is 10", Status.SKIPPED),
                        tuple("When  I add a to b", Status.SKIPPED),
                        tuple("Then  result is 15", Status.SKIPPED)
                );
    }

    @AllureFeatures.Fixtures
    @AllureFeatures.Base
    @Test
    void shouldSupportDryRunForHooks() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/hooks.feature", "--dry-run", "-t",
                "@WithHooks or @BeforeHookWithException or @AfterHookWithException");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .startsWith(
                        tuple("Simple scenario with Before and After hooks", Status.SKIPPED)
                );

        assertThat(writer.getTestResultContainers().get(0).getBefores())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("io.qameta.allure.cucumber5jvm.samples.HookSteps.beforeHook()", Status.SKIPPED)
                );

        assertThat(writer.getTestResultContainers().get(0).getAfters())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("io.qameta.allure.cucumber5jvm.samples.HookSteps.afterHook()", Status.SKIPPED)
                );

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 7", Status.SKIPPED),
                        tuple("And  b is 8", Status.SKIPPED),
                        tuple("When  I add a to b", Status.SKIPPED),
                        tuple("Then  result is 15", Status.SKIPPED)
                );
    }

    @AllureFeatures.History
    @Test
    void shouldPersistHistoryIdForScenarios() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults.get(0).getHistoryId())
                .isEqualTo("8eea9ed4458a49d418859d1398580671");
    }

    @AllureFeatures.History
    @Test
    void shouldPersistHistoryIdForExamples() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/examples.feature", "--threads", "2");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getHistoryId)
                .containsExactlyInAnyOrder("42a7821e775ec18b112f92e96f0510a5", "afb27d131ed8d41b3f867895a26d2590");
    }

    private Comparator<TestResult> byHistoryId =
            Comparator.comparing(TestResult::getHistoryId);

    @AllureFeatures.Parallel
    @Test
    void shouldProcessScenariosInParallelMode() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/parallel.feature", "--threads", "3");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .hasSize(3);

        assertThat(testResults)
                .extracting(testResult -> testResult.getSteps().stream().map(StepResult::getName).collect(Collectors.toList()))
                .containsSubsequence(
                        Arrays.asList("Given  a is 1",
                        "And  b is 3",
                        "When  I add a to b",
                        "Then  result is 4")
                );

        assertThat(testResults)
                .extracting(testResult -> testResult.getSteps().stream().map(StepResult::getName).collect(Collectors.toList()))
                .containsSubsequence(
                        Arrays.asList("Given  a is 2",
                                "And  b is 4",
                                "When  I add a to b",
                                "Then  result is 6")
                );

        assertThat(testResults)
                .extracting(testResult -> testResult.getSteps().stream().map(StepResult::getName).collect(Collectors.toList()))
                .containsSubsequence(
                        Arrays.asList("Given  a is 7",
                                "And  b is 8",
                                "When  I add a to b",
                                "Then  result is 15")
                );
    }

    @AllureFeatures.Fixtures
    @AllureFeatures.Stages
    @Test
    void shouldDisplayHooksAsStages() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/hooks.feature", "-t",
                "@WithHooks or @BeforeHookWithException or @AfterHookWithException");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Simple scenario with Before and After hooks", Status.PASSED),
                        tuple("Simple scenario with Before hook with Exception", Status.SKIPPED),
                        tuple("Simple scenario with After hook with Exception", Status.BROKEN)
                );

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 7", Status.PASSED),
                        tuple("And  b is 8", Status.PASSED),
                        tuple("When  I add a to b", Status.PASSED),
                        tuple("Then  result is 15", Status.PASSED)
                );


        assertThat(writer.getTestResultContainers().get(0).getBefores())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("io.qameta.allure.cucumber5jvm.samples.HookSteps.beforeHook()", Status.PASSED)
                );

        assertThat(writer.getTestResultContainers().get(0).getAfters())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("io.qameta.allure.cucumber5jvm.samples.HookSteps.afterHook()", Status.PASSED)
                );

        assertThat(testResults.get(1).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 7", Status.SKIPPED),
                        tuple("And  b is 8", Status.SKIPPED),
                        tuple("When  I add a to b", Status.SKIPPED),
                        tuple("Then  result is 15", Status.SKIPPED)
                );


        assertThat(writer.getTestResultContainers().get(1).getBefores())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("io.qameta.allure.cucumber5jvm.samples.HookSteps.beforeHookWithException()", Status.FAILED)
                );


        assertThat(testResults.get(2).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 7", Status.PASSED),
                        tuple("And  b is 8", Status.PASSED),
                        tuple("When  I add a to b", Status.PASSED),
                        tuple("Then  result is 15", Status.PASSED)
                );

        assertThat(writer.getTestResultContainers().get(2).getAfters())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("io.qameta.allure.cucumber5jvm.samples.HookSteps.afterHookWithException()", Status.FAILED)
                );

    }

    @AllureFeatures.BrokenTests
    @Test
    void shouldHandleAmbigiousStepsExceptions() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/ambigious.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Simple scenario with ambigious steps", Status.SKIPPED)
                );

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("When  ambigious step present", null),
                        tuple("Then  something bad should happen", Status.SKIPPED)
                );
    }

    private byte runFeature(final AllureResultsWriterStub writer,
                            final String featureResource,
                            final String... moreOptions) {

        final AllureLifecycle lifecycle = new AllureLifecycle(writer);
        final AllureCucumber5Jvm cucumber5Jvm = new AllureCucumber5Jvm(lifecycle);
        Supplier<ClassLoader> classLoader = ClassLoaders::getDefaultClassLoader;
        final List<String> opts = new ArrayList<>(Arrays.asList(
                "--glue", "io.qameta.allure.cucumber5jvm.samples",
                "--plugin", "null_summary"
        ));
        opts.addAll(Arrays.asList(moreOptions));
        FeatureWithLines featureWithLines = FeatureWithLines.parse("src/test/resources/"+featureResource);
        final RuntimeOptions options = new CommandlineOptionsParser().parse(opts).addFeature(featureWithLines).build();
        boolean mt = options.isMultiThreaded();

        EventBus bus = new TimeServiceEventBus(Clock.systemUTC(), UUID::randomUUID);
        FeatureParser parser = new FeatureParser(bus::generateId);
        FeaturePathFeatureSupplier supplier = new FeaturePathFeatureSupplier(classLoader, options, parser);

        final Runtime runtime = Runtime.builder()
                .withClassLoader(classLoader)
                .withRuntimeOptions(options)
                .withAdditionalPlugins(cucumber5Jvm)
                .withFeatureSupplier(supplier)
                .build();

        runtime.run();
        return runtime.exitStatus();
    }
}
