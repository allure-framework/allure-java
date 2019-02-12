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
package io.qameta.allure.cucumberjvm;

import cucumber.api.testng.FeatureResultListener;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
class AllureCucumberJvmTest {

    @AllureFeatures.Base
    @Test
    void shouldSetName() {
        final AllureResults results = runFeature("features/simple.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Add a to b");

    }

    @AllureFeatures.PassedTests
    @Test
    void shouldSetStatus() {
        final AllureResults results = runFeature("features/simple.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
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

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactlyInAnyOrder("expected: <15> but was: <123>");
    }

    @AllureFeatures.BrokenTests
    @Test
    void shouldSetBrokenStatus() {
        final AllureResults results = runFeature("features/broken.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.BROKEN);
    }

    @AllureFeatures.Stages
    @Test
    void shouldSetStage() {
        final AllureResults results = runFeature("features/simple.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStage)
                .containsExactlyInAnyOrder(Stage.FINISHED);
    }

    @AllureFeatures.Timings
    @Test
    void shouldSetStart() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runFeature("features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.Timings
    @Test
    void shouldSetStop() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runFeature("features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.FullName
    @Test
    void shouldSetFullName() {
        final AllureResults results = runFeature("features/simple.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getFullName)
                .containsExactlyInAnyOrder("Simple feature: Add a to b");
    }

    @AllureFeatures.Descriptions
    @Test
    void shouldSetDescription() {
        final AllureResults results = runFeature("features/description.feature");

        final String expected = "\nThis is description for current feature.\n"
                + "It should appear on each scenario in report";

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
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

    @AllureFeatures.Steps
    @Disabled("unsupported")
    @Test
    void shouldAddBackgroundSteps() {
        final AllureResults results = runFeature("features/background.feature");

        final List<TestResult> testResults = results.getTestResults();

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
    @Disabled("unsupported")
    @Test
    void shouldAddParametersFromExamples() {
        final AllureResults results = runFeature("features/examples.feature");

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

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldAddTags() {
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

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
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

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
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

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
    void shouldThreadHostLabels() {
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .contains("host", "thread");
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldCommonLabels() {
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
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

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder((Status) null);
    }

    @AllureFeatures.Base
    @Disabled("unsupported")
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
        final AllureCucumberJvm cucumberJvm = new AllureCucumberJvm(lifecycle);
        final ClassLoader classLoader = currentThread().getContextClassLoader();
        final ResourceLoader resourceLoader = new MultiLoader(classLoader);
        final ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        final List<String> opts = new ArrayList<>(Arrays.asList(
                "--glue", "io.qameta.allure.cucumberjvm.samples",
                "--plugin", "null",
                "src/test/resources/" + featureResource
        ));
        opts.addAll(Arrays.asList(moreOptions));
        final RuntimeOptions options = new RuntimeOptions(opts);
        final Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, options);

        options.addPlugin(cucumberJvm);

        final FeatureResultListener resultListener = new FeatureResultListener(
                options.reporter(classLoader),
                options.isStrict()
        );
        final List<CucumberFeature> features = options.cucumberFeatures(resourceLoader);
        features.forEach(cucumberFeature -> cucumberFeature.run(
                options.formatter(classLoader),
                resultListener,
                runtime)
        );
        return writer;
    }
}
