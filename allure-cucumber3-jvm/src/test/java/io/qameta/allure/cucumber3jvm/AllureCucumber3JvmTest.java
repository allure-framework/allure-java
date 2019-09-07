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
package io.qameta.allure.cucumber3jvm;

import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
import gherkin.AstBuilder;
import gherkin.Parser;
import gherkin.TokenMatcher;
import gherkin.ast.GherkinDocument;
import io.github.glytching.junit.extension.system.SystemProperty;
import io.github.glytching.junit.extension.system.SystemPropertyExtension;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Issue;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Thread.currentThread;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unchecked")
class AllureCucumber3JvmTest {

    @AllureFeatures.Base
    @Test
    void shouldSetName() {
        final AllureResults results = runFeature("features/simple.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Add a to b");

    }

    @AllureFeatures.PassedTests
    @Test
    void shouldSetStatus() {
        final AllureResults results = runFeature("features/simple.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.PASSED);
    }

    @AllureFeatures.FailedTests
    @Test
    void shouldSetFailedStatus() {
        final AllureResults results = runFeature("features/failed.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED);
    }

    @AllureFeatures.FailedTests
    @Test
    void shouldSetStatusDetails() {
        final AllureResults results = runFeature("features/failed.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactlyInAnyOrder("expected: <15> but was: <123>");
    }

    @AllureFeatures.BrokenTests
    @Test
    void shouldSetBrokenStatus() {
        final AllureResults results = runFeature("features/broken.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.BROKEN);
    }

    @AllureFeatures.Stages
    @Test
    void shouldSetStage() {
        final AllureResults results = runFeature("features/simple.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStage)
                .containsExactlyInAnyOrder(Stage.FINISHED);
    }

    @AllureFeatures.Timings
    @Test
    void shouldSetStart() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runFeature("features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.Timings
    @Test
    void shouldSetStop() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runFeature("features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.FullName
    @Test
    void shouldSetFullName() {
        final AllureResults results = runFeature("features/simple.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getFullName)
                .containsExactlyInAnyOrder("Simple feature: Add a to b");
    }

    @AllureFeatures.Descriptions
    @Test
    void shouldSetDescription() {
        final AllureResults results = runFeature("features/description.feature");

        final String expected = "This is description for current feature.\n"
                + "It should appear on each scenario in report";

        assertThat(results.getTestResults())
                .extracting(TestResult::getDescription)
                .containsExactlyInAnyOrder(
                        expected,
                        expected
                );
    }

    @AllureFeatures.Attachments
    @Test
    void shouldAddDataTableAttachment() {
        final AllureResults results = runFeature("features/datatable.feature");

        final List<Attachment> attachments = results.getTestResults().stream()
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
        final Map<String, byte[]> attachmentFiles = results.getAttachments();
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
        final AllureResults results = runFeature("features/attachments.feature");

        final List<Attachment> attachments = results.getTestResults().stream()
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

        final List<String> attachmentContents = results.getAttachments().values().stream()
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .collect(Collectors.toList());

        assertThat(attachmentContents)
                .containsExactlyInAnyOrder("text attachment", "image attachment");
    }

    @AllureFeatures.Steps
    @Test
    void shouldAddBackgroundSteps() {
        final AllureResults results = runFeature("features/background.feature");

        assertThat(results.getTestResults())
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
        final AllureResults results = runFeature("features/examples.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .hasSize(2);

        assertThat(testResults.get(0).getParameters())
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("a", "1"), tuple("b", "3"), tuple("result", "4")
                );

        assertThat(testResults.get(1).getParameters())
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("a", "2"), tuple("b", "4"), tuple("result", "6")
                );

    }

    @AllureFeatures.Parameters
    @Test
    void shouldHandleMultipleExamplesPerOutline() throws IOException {
        final AllureResults results = runFeature("features/multi-examples.feature");

        final List<TestResult> testResults = results.getTestResults();

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
    void shouldSupportTaggedExamplesBlocks() throws IOException {
        final AllureResults results = runFeature("features/multi-examples.feature", "--tags", "@ExamplesTag2");

        final List<TestResult> testResults = results.getTestResults();

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
        final AllureResults results = runFeature("features/tags.feature");

        assertThat(results.getTestResults())
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
        final AllureResults results = runFeature("features/tags.feature");

        assertThat(results.getTestResults())
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
        final AllureResults results = runFeature("features/tags.feature");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("feature", "Test Simple Scenarios"),
                        tuple("story", "Add a to b")
                );
    }

    @AllureFeatures.Timeline
    @Test
    void shouldThreadHostLabels() {
        final AllureResults results = runFeature("features/tags.feature");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .contains("host", "thread");
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldCommonLabels() {
        final AllureResults results = runFeature("features/tags.feature");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("package", "Test Simple Scenarios"),
                        tuple("suite", "Test Simple Scenarios"),
                        tuple("testClass", "Add a to b")
                );
    }

    @AllureFeatures.NotImplementedTests
    @Test
    void shouldProcessNotImplementedScenario() {
        final AllureResults results = runFeature("features/undefined.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.PASSED);
    }

    @AllureFeatures.Base
    @Test
    void shouldSupportDryRun() {
        final AllureResults results = runFeature("features/simple.feature", "--dry-run");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Add a to b", Status.SKIPPED)
                );
    }

    @AllureFeatures.Base
    @Issue("173")
    @Issue("164")
    @Test
    void shouldUseUuid() {
        final AllureResults results = runFeature("features/simple.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getUuid)
                .allMatch(uuid -> nonNull(uuid) && uuid.matches("[\\-a-z0-9]+"), "UUID");
    }

    private AllureResults runFeature(final String featureResource,
                                     final String... moreOptions) {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writer);
        final AllureCucumber3Jvm cucumber3Jvm = new AllureCucumber3Jvm(lifecycle);
        final ClassLoader classLoader = currentThread().getContextClassLoader();
        final ResourceLoader resourceLoader = new MultiLoader(classLoader);
        final ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        final List<String> opts = new ArrayList<>(Arrays.asList(
                "--glue", "io.qameta.allure.cucumber3jvm.samples",
                "--plugin", "null"
        ));
        opts.addAll(Arrays.asList(moreOptions));
        final RuntimeOptions options = new RuntimeOptions(opts);
        final Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, options);

        options.addPlugin(cucumber3Jvm);
        options.noSummaryPrinter();

        final String gherkin = readResource(featureResource);
        Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        TokenMatcher matcher = new TokenMatcher();
        GherkinDocument gherkinDocument = parser.parse(gherkin, matcher);
        CucumberFeature feature = new CucumberFeature(gherkinDocument, featureResource, gherkin);

        feature.sendTestSourceRead(runtime.getEventBus());
        runtime.runFeature(feature);
        return writer;
    }

    @AllureFeatures.Fixtures
    @Test
    void shouldSetStatusFailedOnBadAfter() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bp_sf_af");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED);
    }


    @AllureFeatures.Fixtures
    @Test
    void shouldSetStatusSkippedOnBadBefore() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bf_ap");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.SKIPPED);
    }

    @AllureFeatures.Fixtures
    @Test
    void shouldSetStatusSkippedOnBadBeforeAndBadAfter() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bf_af");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.SKIPPED);
    }

    @AllureFeatures.Fixtures
    @Test
    void shouldSetStatusBrokenOnBadAfter() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bp_af");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.BROKEN);
    }

    @AllureFeatures.Fixtures
    @Test
    void shouldSetStatusFailedOnBadSteps() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bp_sf_ap");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED);
    }

    @AllureFeatures.NotImplementedTests
    @Test
    void shouldSetStatusBrokenOnUndefinedStepsAndBadAfter() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bp_su_af");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.BROKEN);
    }


    @AllureFeatures.NotImplementedTests
    @Test
    void shouldSetStatusPassedOnUndefinedSteps() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bp_su_ap");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.PASSED);
    }

    @AllureFeatures.NotImplementedTests
    @Test
    void shouldSetStatusSkippedOnUndefinedAndFailedSteps() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bp_suf_ap");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.SKIPPED);
    }


    @AllureFeatures.NotImplementedTests
    @Test
    void shouldSetStatusPassedOnPassedAndUndefinedSteps() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bp_spu_ap");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.PASSED);
    }

    @AllureFeatures.NotImplementedTests
    @Test
    void shouldSetStatusFailedOnFailedAndUndefinedSteps() {
        final AllureResults results = runFeature("features/hooks.feature", "-t", "@bp_sfu_ap");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED);
    }

    private String readResource(final String resourceName) {
        try (InputStream is = currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Feature file not found " + resourceName);
        }
    }
}
