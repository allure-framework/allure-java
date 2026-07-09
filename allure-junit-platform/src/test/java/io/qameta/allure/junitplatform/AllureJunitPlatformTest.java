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
package io.qameta.allure.junitplatform;

import io.github.glytching.junit.extension.system.SystemProperty;
import io.qameta.allure.Issue;
import io.qameta.allure.junitplatform.features.ActualExpectedStatusDetailsTests;
import io.qameta.allure.junitplatform.features.AllureIdAnnotationSupport;
import io.qameta.allure.junitplatform.features.BrokenInAfterAllTests;
import io.qameta.allure.junitplatform.features.BrokenInBeforeAllTests;
import io.qameta.allure.junitplatform.features.BrokenTests;
import io.qameta.allure.junitplatform.features.DescriptionJavadocTest;
import io.qameta.allure.junitplatform.features.DisabledRepeatedTests;
import io.qameta.allure.junitplatform.features.DisabledTests;
import io.qameta.allure.junitplatform.features.DynamicTests;
import io.qameta.allure.junitplatform.features.FailedTests;
import io.qameta.allure.junitplatform.features.JupiterUniqueIdTest;
import io.qameta.allure.junitplatform.features.MarkerAnnotationSupport;
import io.qameta.allure.junitplatform.features.NestedDisplayNameTests;
import io.qameta.allure.junitplatform.features.NestedTests;
import io.qameta.allure.junitplatform.features.OneTest;
import io.qameta.allure.junitplatform.features.OwnerTest;
import io.qameta.allure.junitplatform.features.ParallelTests;
import io.qameta.allure.junitplatform.features.ParameterisedClassTests;
import io.qameta.allure.junitplatform.features.ParameterisedTests;
import io.qameta.allure.junitplatform.features.ParameterisedTestsWithDisplayName;
import io.qameta.allure.junitplatform.features.PassedTests;
import io.qameta.allure.junitplatform.features.RepeatedTests;
import io.qameta.allure.junitplatform.features.ReportEntryParameter;
import io.qameta.allure.junitplatform.features.SeverityTest;
import io.qameta.allure.junitplatform.features.SkippedInBeforeAllTests;
import io.qameta.allure.junitplatform.features.SkippedTests;
import io.qameta.allure.junitplatform.features.TaggedTests;
import io.qameta.allure.junitplatform.features.TestClassDisabled;
import io.qameta.allure.junitplatform.features.TestClassWithDisplayNameAnnotation;
import io.qameta.allure.junitplatform.features.TestClassWithoutDisplayNameAnnotation;
import io.qameta.allure.junitplatform.features.TestWithClassLabels;
import io.qameta.allure.junitplatform.features.TestWithClassLinks;
import io.qameta.allure.junitplatform.features.TestWithDescription;
import io.qameta.allure.junitplatform.features.TestWithDisplayName;
import io.qameta.allure.junitplatform.features.TestWithInheritedMetadata;
import io.qameta.allure.junitplatform.features.TestWithInterfaceDefaultMetadata;
import io.qameta.allure.junitplatform.features.TestWithMethodLabels;
import io.qameta.allure.junitplatform.features.TestWithMethodLinks;
import io.qameta.allure.junitplatform.features.TestWithOverriddenMetadata;
import io.qameta.allure.junitplatform.features.TestWithPublishedFile;
import io.qameta.allure.junitplatform.features.TestWithSteps;
import io.qameta.allure.junitplatform.features.TestWithSystemErr;
import io.qameta.allure.junitplatform.features.TestWithSystemOut;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.qameta.allure.junitplatform.AllureJunitPlatform.JUNIT_PLATFORM_UNIQUE_ID;
import static io.qameta.allure.junitplatform.AllureJunitPlatformTestUtils.runClasses;
import static io.qameta.allure.junitplatform.features.TaggedTests.CLASS_TAG;
import static io.qameta.allure.junitplatform.features.TaggedTests.METHOD_TAG;
import static io.qameta.allure.test.AllurePredicates.hasLabel;
import static io.qameta.allure.test.AllurePredicates.hasStatus;
import static io.qameta.allure.util.ResultsUtils.ALLURE_ID_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.EPIC_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.FEATURE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.HOST_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.OWNER_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.PACKAGE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.PARENT_SUITE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.SEVERITY_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.STORY_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.SUB_SUITE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.SUITE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TAG_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TEST_CLASS_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TEST_METHOD_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.THREAD_LABEL_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;
@SuppressWarnings("unchecked")
@IsolatedLifecycle
public class AllureJunitPlatformTest {

    @Test
    @AllureFeatures.FullName
    void shouldSetFullName() {
        final AllureResults results = runClasses(PassedTests.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getFullName)
                .containsExactlyInAnyOrder(
                        "io.qameta.allure.junitplatform.features.PassedTests.second",
                        "io.qameta.allure.junitplatform.features.PassedTests.first",
                        "io.qameta.allure.junitplatform.features.PassedTests.third"
                );
    }

    @Test
    @AllureFeatures.Timeline
    void shouldSetExecutionLabels() {
        final AllureResults results = runClasses(OneTest.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .contains(HOST_LABEL_NAME, THREAD_LABEL_NAME);
    }

    @Test
    @AllureFeatures.PassedTests
    void shouldProcessPassedTests() {
        final AllureResults results = runClasses(PassedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
                .filteredOn(hasStatus(Status.PASSED))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("first()", "second()", "third()");
    }

    @Test
    @AllureFeatures.FailedTests
    void shouldProcessFailedTests() {
        final AllureResults results = runClasses(FailedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrPropertyWithValue("name", "failedTest()")
                .hasFieldOrPropertyWithValue("status", Status.FAILED);

        assertThat(testResult.getStatusDetails())
                .hasFieldOrPropertyWithValue("message", "Make the test failed")
                .hasFieldOrProperty("trace");

    }

    @Test
    @AllureFeatures.FailedTests
    void shouldReportActualAndExpectedStatusDetails() {
        final AllureResults results = runClasses(ActualExpectedStatusDetailsTests.class);
        final TestResult testResult = results.getTestResults().stream()
                .filter(
                        result -> ("io.qameta.allure.junitplatform.features.ActualExpectedStatusDetailsTests"
                                + ".failingComparison").equals(result.getFullName())
                )
                .findFirst()
                .get();

        assertThat(testResult.getStatus()).isEqualTo(Status.FAILED);
        assertThat(testResult.getStatusDetails().getActual()).startsWith("actual value (");
        assertThat(testResult.getStatusDetails().getExpected()).startsWith("expected value (");
    }

    @Test
    @AllureFeatures.BrokenTests
    void shouldProcessBrokenTests() {
        final AllureResults results = runClasses(BrokenTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "brokenTest()")
                .hasFieldOrPropertyWithValue("status", Status.BROKEN);

        assertThat(testResult.getStatusDetails())
                .hasFieldOrPropertyWithValue("message", "Make the test broken")
                .hasFieldOrProperty("trace");
    }

    @Test
    @AllureFeatures.BrokenTests
    void shouldProcessBrokenInBeforeAllTests() {
        final AllureResults results = runClasses(BrokenInBeforeAllTests.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .extracting(
                        TestResult::getName,
                        TestResult::getStatus,
                        tr -> Optional.of(tr).map(TestResult::getStatusDetails).map(StatusDetails::getMessage).orElse(null)
                )
                .containsExactlyInAnyOrder(
                        tuple("BrokenInBeforeAllTests", Status.BROKEN, "Exception in @BeforeAll")
                );
    }

    @Test
    @AllureFeatures.BrokenTests
    void shouldProcessBrokenInAfterAllTests() {
        final AllureResults results = runClasses(BrokenInAfterAllTests.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .extracting(
                        TestResult::getName,
                        TestResult::getStatus,
                        tr -> Optional.of(tr).map(TestResult::getStatusDetails).map(StatusDetails::getMessage).orElse(null)
                )
                .containsExactlyInAnyOrder(
                        tuple("BrokenInAfterAllTests", Status.BROKEN, "Exception in @AfterAll"),
                        tuple("parameterisedTest(String) [1] value = \"a\"", Status.PASSED, null),
                        tuple("parameterisedTest(String) [2] value = \"b\"", Status.PASSED, null),
                        tuple("parameterisedTest(String) [3] value = \"c\"", Status.PASSED, null),
                        tuple("test1()", Status.PASSED, null),
                        tuple("test2()", Status.PASSED, null)
                );
    }

    @Test
    @AllureFeatures.SkippedTests
    void shouldProcessSkippedTests() {
        final AllureResults results = runClasses(SkippedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "skippedTest()")
                .hasFieldOrPropertyWithValue("status", Status.SKIPPED);

        assertThat(testResult.getStatusDetails())
                .hasFieldOrPropertyWithValue("message", "Assumption failed: Make the test skipped")
                .hasFieldOrProperty("trace");
    }

    @Test
    @AllureFeatures.SkippedTests
    void shouldProcessSkippedInBeforeAllTests() {
        final AllureResults results = runClasses(SkippedInBeforeAllTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "SkippedInBeforeAllTests")
                .hasFieldOrPropertyWithValue("status", Status.SKIPPED);

        assertThat(testResult.getStatusDetails())
                .hasFieldOrPropertyWithValue("message", "Assumption failed: Skip in @BeforeAll")
                .hasFieldOrProperty("trace");
    }

    @Test
    @AllureFeatures.DisplayName
    void shouldProcessDisplayName() {
        final AllureResults results = runClasses(TestWithDisplayName.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrPropertyWithValue("name", "Some test with changed name");
    }

    @Test
    @AllureFeatures.Timings
    void shouldSetStartAndStopTimes() {
        final AllureResults results = runClasses(FailedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrProperty("start")
                .hasFieldOrProperty("stop");
    }

    @Test
    @AllureFeatures.Stages
    void shouldSetFinishedStage() {
        final AllureResults results = runClasses(FailedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED);
    }

    @Test
    @AllureFeatures.Base
    void shouldProcessDynamicTests() {
        final AllureResults results = runClasses(DynamicTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
                .filteredOn(hasStatus(Status.PASSED))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("testA", "testB", "testC");
    }

    @Test
    @AllureFeatures.Parameters
    void shouldProcessParametrisedTests() {
        final AllureResults results = runClasses(ParameterisedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .filteredOn(hasStatus(Status.PASSED))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "testWithStringParameter(String) [1] argument = \"Hello\"",
                        "testWithStringParameter(String) [2] argument = \"World\""
                );
    }

    @Test
    @AllureFeatures.Steps
    void shouldAddSteps() {
        final AllureResults results = runClasses(TestWithSteps.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult.getSteps())
                .hasSize(3)
                .flatExtracting(StepResult::getName)
                .containsExactly("first", "second", "third");

    }

    @Test
    @AllureFeatures.MarkerAnnotations
    void shouldAddTags() {
        final AllureResults results = runClasses(TaggedTests.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(TAG_LABEL_NAME, CLASS_TAG),
                        tuple(TAG_LABEL_NAME, METHOD_TAG)
                );
    }

    @Test
    @AllureFeatures.DisplayName
    void shouldProcessDefaultTestClassDisplayName() {
        final AllureResults results = runClasses(TestClassWithoutDisplayNameAnnotation.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", SUITE_LABEL_NAME)
                .extracting(Label::getValue)
                .contains("io.qameta.allure.junitplatform.features.TestClassWithoutDisplayNameAnnotation");
    }

    @Test
    @AllureFeatures.Descriptions
    void shouldProcessJunit5Description() {
        final AllureResults results = runClasses(TestWithDescription.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getDescription)
                .contains("Verifies that JUnit Platform reads an explicit @Description value from the test method.");
    }

    @Test
    @AllureFeatures.IgnoredTests
    void shouldProcessDisabledTests() {
        final AllureResults results = runClasses(DisabledTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.SKIPPED);
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    void shouldProcessMethodLabels() {
        final AllureResults results = runClasses(TestWithMethodLabels.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getValue)
                .contains(
                        "epic1", "epic2", "epic3",
                        "feature1", "feature2", "feature3",
                        "story1", "story2", "story3",
                        "some-owner"
                );
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    @Issue("1032")
    void shouldProcessAnnotationsOnInheritedTestMethods() {
        final AllureResults results = runClasses(TestWithInheritedMetadata.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult.getDescription())
                .isEqualTo("Inherited test description");

        assertThat(testResult.getLabels())
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(EPIC_LABEL_NAME, "inherited-epic"),
                        tuple(FEATURE_LABEL_NAME, "inherited-feature"),
                        tuple(STORY_LABEL_NAME, "inherited-story"),
                        tuple(OWNER_LABEL_NAME, "inherited-owner"),
                        tuple(SEVERITY_LABEL_NAME, "critical")
                );

        assertThat(testResult.getLinks())
                .extracting(Link::getName, Link::getUrl)
                .contains(tuple("INHERITED-LINK", "https://example.org/inherited"));
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    @Issue("1032")
    void shouldProcessAnnotationsOnInterfaceDefaultTestMethods() {
        final AllureResults results = runClasses(TestWithInterfaceDefaultMetadata.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult.getDescription())
                .isEqualTo("Interface default test description");

        assertThat(testResult.getLabels())
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(STORY_LABEL_NAME, "interface-story"),
                        tuple(OWNER_LABEL_NAME, "interface-owner"),
                        tuple(SEVERITY_LABEL_NAME, "minor")
                );

        assertThat(testResult.getLinks())
                .extracting(Link::getName, Link::getUrl)
                .contains(tuple("INTERFACE-LINK", "https://example.org/interface"));
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    @Issue("1032")
    void shouldUseOverriddenTestMethodAnnotations() {
        final AllureResults results = runClasses(TestWithOverriddenMetadata.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        // the overriding method fully replaces the inherited one, so only
        // the annotations declared on the override are reported
        final TestResult testResult = testResults.get(0);
        assertThat(testResult.getDescription())
                .isEqualTo("Overridden test description");

        assertThat(testResult.getLabels())
                .extracting(Label::getName, Label::getValue)
                .contains(tuple(STORY_LABEL_NAME, "overridden-story"))
                .doesNotContain(
                        tuple(STORY_LABEL_NAME, "interface-story"),
                        tuple(OWNER_LABEL_NAME, "interface-owner"),
                        tuple(SEVERITY_LABEL_NAME, "minor")
                );

        assertThat(testResult.getLinks())
                .extracting(Link::getName)
                .doesNotContain("INTERFACE-LINK");
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    void shouldProcessClassLabels() {
        final AllureResults results = runClasses(TestWithClassLabels.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getValue)
                .contains(
                        "epic1", "epic2", "epic3",
                        "feature1", "feature2", "feature3",
                        "story1", "story2", "story3",
                        "some-owner"
                );
    }

    @AllureFeatures.Links
    @ResourceLock(
            value = SYSTEM_PROPERTIES,
            mode = READ_WRITE
    )
    @SystemProperty(
            name = "allure.link.issue.pattern",
            value = "https://example.org/issue/{}"
    )
    @SystemProperty(
            name = "allure.link.tms.pattern",
            value = "https://example.org/tms/{}"
    )
    @SystemProperty(
            name = "allure.link.custom.pattern",
            value = "https://example.org/custom/{}"
    )
    @Test
    void shouldProcessMethodLinks() {
        final AllureResults results = runClasses(TestWithMethodLinks.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType, Link::getUrl)
                .contains(
                        tuple("LINK-1", "custom", "https://example.org/custom/LINK-1"),
                        tuple("LINK-2", "custom", "https://example.org/link/2"),
                        tuple("", "custom", "https://example.org/some-custom-link"),
                        tuple("ISSUE-1", "issue", "https://example.org/issue/ISSUE-1"),
                        tuple("ISSUE-2", "issue", "https://example.org/issue/ISSUE-2"),
                        tuple("ISSUE-3", "issue", "https://example.org/issue/ISSUE-3"),
                        tuple("TMS-1", "tms", "https://example.org/tms/TMS-1"),
                        tuple("TMS-2", "tms", "https://example.org/tms/TMS-2"),
                        tuple("TMS-3", "tms", "https://example.org/tms/TMS-3")
                );
    }

    @AllureFeatures.Links
    @ResourceLock(
            value = SYSTEM_PROPERTIES,
            mode = READ_WRITE
    )
    @SystemProperty(
            name = "allure.link.issue.pattern",
            value = "https://example.org/issue/{}"
    )
    @SystemProperty(
            name = "allure.link.tms.pattern",
            value = "https://example.org/tms/{}"
    )
    @SystemProperty(
            name = "allure.link.custom.pattern",
            value = "https://example.org/custom/{}"
    )
    @Test
    void shouldProcessClassLinks() {
        final AllureResults results = runClasses(TestWithClassLinks.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType, Link::getUrl)
                .contains(
                        tuple("LINK-1", "custom", "https://example.org/custom/LINK-1"),
                        tuple("LINK-2", "custom", "https://example.org/link/2"),
                        tuple("", "custom", "https://example.org/some-custom-link"),
                        tuple("ISSUE-1", "issue", "https://example.org/issue/ISSUE-1"),
                        tuple("ISSUE-2", "issue", "https://example.org/issue/ISSUE-2"),
                        tuple("ISSUE-3", "issue", "https://example.org/issue/ISSUE-3"),
                        tuple("TMS-1", "tms", "https://example.org/tms/TMS-1"),
                        tuple("TMS-2", "tms", "https://example.org/tms/TMS-2"),
                        tuple("TMS-3", "tms", "https://example.org/tms/TMS-3")
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Issue("189")
    @Test
    void shouldProcessDynamicTestLabels() {
        final AllureResults results = runClasses(DynamicTests.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getValue)
                .contains(
                        "epic1", "epic2", "epic3",
                        "feature1", "feature2", "feature3",
                        "story1", "story2", "story3",
                        "some-owner"
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldCommonLabels() {
        final AllureResults results = runClasses(OneTest.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(PACKAGE_LABEL_NAME, "io.qameta.allure.junitplatform.features.OneTest"),
                        tuple(SUITE_LABEL_NAME, "io.qameta.allure.junitplatform.features.OneTest"),
                        tuple(TEST_CLASS_LABEL_NAME, "io.qameta.allure.junitplatform.features.OneTest"),
                        tuple(TEST_METHOD_LABEL_NAME, "single")
                );
    }

    @AllureFeatures.DisplayName
    @Issue("180")
    @Test
    void shouldSetSuiteNameFromDisplayNameAnnotation() {
        final AllureResults results = runClasses(TestClassWithDisplayNameAnnotation.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(SUITE_LABEL_NAME, "Display name of test class")
                );
    }

    @AllureFeatures.Severity
    @Test
    void shouldSetSeverity() {
        final AllureResults results = runClasses(SeverityTest.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .filteredOn("name", "criticalSeverityTest()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", SEVERITY_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactly("critical");

        assertThat(testResults)
                .filteredOn("name", "defaultSeverityTest()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", SEVERITY_LABEL_NAME)
                .extracting(Label::getValue)
                .contains("trivial");
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldSetOwner() {
        final AllureResults results = runClasses(OwnerTest.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .filteredOn("name", "secondOwnerTest()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", OWNER_LABEL_NAME)
                .extracting(Label::getValue)
                .contains("first", "second");

        assertThat(testResults)
                .filteredOn("name", "defaultOwnerTest()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", OWNER_LABEL_NAME)
                .extracting(Label::getValue)
                .contains("first");
    }

    @AllureFeatures.Descriptions
    @Test
    void shouldSetJavadocDescription() {
        final AllureResults results = runClasses(DescriptionJavadocTest.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getDescription, TestResult::getDescriptionHtml)
                .containsExactly(
                        tuple(
                                "Runs a JUnit Platform test whose JavaDoc is used as the Allure description.",
                                null
                        )
                );
    }

    @AllureFeatures.Attachments
    @ResourceLock(
            value = SYSTEM_PROPERTIES,
            mode = READ_WRITE
    )
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @SystemProperty(
            name = "junit.platform.output.capture.stdout",
            value = "true"
    )
    @Test
    void shouldCaptureSystemOut() {
        final AllureResults results = runClasses(TestWithSystemOut.class);

        final List<Attachment> attachments = results.getAttachmentsRecursively();

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .contains(
                        tuple("Stdout", "text/plain")
                );

        final Attachment found = attachments.stream()
                .filter(attachment -> "Stdout".equals(attachment.getName()))
                .findAny()
                .get();

        assertThat(results.getAttachments())
                .containsKeys(found.getSource());

        assertThat(results.getAttachmentContentAsString(found))
                .isEqualTo("SYS OUT CONTENT\n");

    }

    @AllureFeatures.Attachments
    @ResourceLock(
            value = SYSTEM_PROPERTIES,
            mode = READ_WRITE
    )
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @SystemProperty(
            name = "junit.platform.output.capture.stderr",
            value = "true"
    )
    @Test
    void shouldCaptureSystemErr() {
        final AllureResults results = runClasses(TestWithSystemErr.class);

        final List<Attachment> attachments = results.getAttachmentsRecursively();

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .contains(
                        tuple("Stderr", "text/plain")
                );

        final Attachment found = attachments.stream()
                .filter(attachment -> "Stderr".equals(attachment.getName()))
                .findAny()
                .get();

        assertThat(results.getAttachments())
                .containsKeys(found.getSource());

        assertThat(results.getAttachmentContentAsString(found))
                .isEqualTo("SYS ERR CONTENT\n");

    }

    @AllureFeatures.Parallel
    @ResourceLock(
            value = SYSTEM_PROPERTIES,
            mode = READ_WRITE
    )
    @SystemProperty(
            name = "junit.jupiter.execution.parallel.enabled",
            value = "true"
    )
    @Test
    void shouldRunTestsInParallel() {
        final AllureResults results = runClasses(ParallelTests.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "first()",
                        "second()"
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldSupportDisabledTestClasses() {
        final AllureResults results = runClasses(TestClassDisabled.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "First",
                        "second()",
                        "third()"
                );

        assertThat(testResults)
                .filteredOn("name", "second()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", FEATURE_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("A");

        assertThat(testResults)
                .filteredOn("name", "third()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", OWNER_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("me");
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldSupportRepeatedTests() {
        final AllureResults results = runClasses(RepeatedTests.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(5)
                .allMatch(hasStatus(Status.PASSED))
                .allMatch(hasLabel(OWNER_LABEL_NAME, "me"));

    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldSupportDisabledRepeatedTests() {
        final AllureResults results = runClasses(DisabledRepeatedTests.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .allMatch(hasStatus(Status.SKIPPED))
                .allMatch(hasLabel(OWNER_LABEL_NAME, "other guy"));

    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldSupportMarkerAnnotations() {
        final AllureResults results = runClasses(MarkerAnnotationSupport.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", FEATURE_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactly("Basic framework support");
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", STORY_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactly("Core features");

    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldSupportAllureIdAnnotations() {
        final AllureResults results = runClasses(AllureIdAnnotationSupport.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", ALLURE_ID_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactly("123");

    }

    @AllureFeatures.Base
    @Test
    void shouldPopulateTestCaseId() {
        final AllureResults results = runClasses(JupiterUniqueIdTest.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", JUNIT_PLATFORM_UNIQUE_ID)
                .extracting(Label::getValue)
                .first()
                .isEqualTo("[engine:junit-jupiter]/[class:io.qameta.allure.junitplatform.features.JupiterUniqueIdTest]/[method:jupiterTest()]");
    }

    @AllureFeatures.Parameters
    @Test
    void shouldAllowUsageOfDisplayForParameterisedTests() {
        final AllureResults results = runClasses(ParameterisedTestsWithDisplayName.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(
                        TestResult::getName
                )
                .containsExactlyInAnyOrder(
                        "Second Test [1] value = \"a\"",
                        "Second Test [2] value = \"b\""
                );
    }

    @AllureFeatures.Parameters
    @Test
    void shouldProcessAllureParameterReportingEvents() {
        final AllureResults results = runClasses(ReportEntryParameter.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .filteredOn("name", "simpleParameterEvent(TestReporter)")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .containsExactlyInAnyOrder(
                        tuple("some parameter name", "some parameter value", null, null)
                );
        assertThat(testResults)
                .filteredOn("name", "multipleParameterEvent(TestReporter)")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .containsExactlyInAnyOrder(
                        tuple("first name", "first value", null, null),
                        tuple("second name", "second value", null, null),
                        tuple("third name", "third value", null, null)
                );
        assertThat(testResults)
                .filteredOn("name", "modeAndExcluded(TestReporter)")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .containsExactlyInAnyOrder(
                        tuple("hidden excluded", "hidden excluded value", Parameter.Mode.HIDDEN, true),
                        tuple("default excluded", "default excluded value", Parameter.Mode.DEFAULT, true),
                        tuple("masked not excluded", "masked not excluded value", Parameter.Mode.MASKED, false)
                );
    }

    @Test
    @AllureFeatures.Retries
    void shouldSetDifferentUuidsInDifferentRuns() {
        final AllureResults results = RunUtils.runTests(lifecycle -> {
            final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .filters(new AllurePostDiscoveryFilter(null))
                    .selectors(DiscoverySelectors.selectClass(OneTest.class))
                    .build();

            final LauncherConfig config = LauncherConfig.builder()
                    .enableTestExecutionListenerAutoRegistration(false)
                    .addTestExecutionListeners(new AllureJunitPlatform(lifecycle))
                    .enablePostDiscoveryFilterAutoRegistration(false)
                    .build();
            final Launcher launcher = LauncherFactory.create(config);

            // execute request twice
            launcher.execute(request);
            launcher.execute(request);
        });

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2);

        final TestResult tr1 = testResults.get(0);
        final TestResult tr2 = testResults.get(1);

        assertThat(tr1)
                .extracting(TestResult::getUuid)
                .isNotEqualTo(tr2.getUuid());

        assertThat(tr1)
                .extracting(TestResult::getHistoryId)
                .isEqualTo(tr2.getHistoryId());

    }

    @Test
    void shouldSupportNestedClasses() {
        final AllureResults allureResults = runClasses(NestedTests.class);

        assertThat(allureResults.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "parentTest()",
                        "feature1Test()",
                        "feature2Test()",
                        "story1Test()"
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "story1Test()")
                .extracting(TestResult::getTitlePath)
                .containsExactly(
                        Arrays.asList(
                                "io", "qameta", "allure", "junitplatform", "features",
                                "NestedTests", "Feature2", "Story1"
                        )
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "parentTest()")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("epic", "Parent epic"),
                        tuple("feature", "Parent feature")
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "feature1Test()")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("epic", "Parent epic"),
                        tuple("feature", "Feature 1")
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "feature2Test()")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("epic", "Parent epic"),
                        tuple("feature", "Feature 2")
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "story1Test()")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("epic", "Parent epic"),
                        tuple("feature", "Feature 2"),
                        tuple("story", "Story 1")
                );
    }

    @AllureFeatures.Trees
    @Issue("1234")
    @Issue("1052")
    @Test
    void shouldSetSuiteLabelsForNestedClasses() {
        final AllureResults allureResults = runClasses(NestedTests.class);

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "story1Test()")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(PARENT_SUITE_LABEL_NAME, "io.qameta.allure.junitplatform.features.NestedTests"),
                        tuple(SUITE_LABEL_NAME, "Feature2"),
                        tuple(SUB_SUITE_LABEL_NAME, "Story1")
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "feature1Test()")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(PARENT_SUITE_LABEL_NAME, "io.qameta.allure.junitplatform.features.NestedTests"),
                        tuple(SUITE_LABEL_NAME, "Feature1")
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "feature1Test()")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .doesNotContain(SUB_SUITE_LABEL_NAME);

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "parentTest()")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(tuple(SUITE_LABEL_NAME, "io.qameta.allure.junitplatform.features.NestedTests"));

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "parentTest()")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .doesNotContain(PARENT_SUITE_LABEL_NAME, SUB_SUITE_LABEL_NAME);
    }

    @AllureFeatures.Trees
    @Issue("1234")
    @Issue("1052")
    @Test
    void shouldUseDisplayNamesForNestedClassesSuiteStructure() {
        final AllureResults allureResults = runClasses(NestedDisplayNameTests.class);

        assertThat(allureResults.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "can be created with the dao",
                        "it must be saved to the dao",
                        "it can be fetched from the dao",
                        "it cannot be deleted by wrong id",
                        "it is still present"
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "can be created with the dao")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(tuple(SUITE_LABEL_NAME, "A customer object"));

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "can be created with the dao")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .doesNotContain(PARENT_SUITE_LABEL_NAME, SUB_SUITE_LABEL_NAME);

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "it must be saved to the dao")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(PARENT_SUITE_LABEL_NAME, "A customer object"),
                        tuple(SUITE_LABEL_NAME, "when created")
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "it must be saved to the dao")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .doesNotContain(SUB_SUITE_LABEL_NAME);

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "it can be fetched from the dao")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(PARENT_SUITE_LABEL_NAME, "A customer object"),
                        tuple(SUITE_LABEL_NAME, "when created"),
                        tuple(SUB_SUITE_LABEL_NAME, "after saving a customer")
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "it is still present")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(PARENT_SUITE_LABEL_NAME, "A customer object"),
                        tuple(SUITE_LABEL_NAME, "when created"),
                        tuple(SUB_SUITE_LABEL_NAME, "after saving a customer > and reloading the dao")
                );

        assertThat(allureResults.getTestResults())
                .filteredOn("name", "it can be fetched from the dao")
                .extracting(TestResult::getTitlePath)
                .containsExactly(
                        Arrays.asList(
                                "io", "qameta", "allure", "junitplatform", "features",
                                "A customer object", "when created", "after saving a customer"
                        )
                );
    }

    @AllureFeatures.Attachments
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void shouldAttachPublishedFiles(@TempDir final Path outputDir) {
        final AllureResults results = RunUtils.runTests(lifecycle -> {
            final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(TestWithPublishedFile.class))
                    .configurationParameter("junit.platform.reporting.output.dir", outputDir.toString())
                    .build();
            final LauncherConfig config = LauncherConfig.builder()
                    .enableTestExecutionListenerAutoRegistration(false)
                    .addTestExecutionListeners(new AllureJunitPlatform(lifecycle))
                    .build();
            LauncherFactory.create(config).execute(request);
        });

        final List<Attachment> attachments = results.getAttachmentsRecursively();

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .contains(
                        tuple("published-file.txt", "text/plain")
                );

        final Attachment found = attachments.stream()
                .filter(attachment -> "published-file.txt".equals(attachment.getName()))
                .findAny()
                .get();

        assertThat(found.getSource())
                .endsWith(".txt");

        assertThat(results.getAttachments())
                .containsKeys(found.getSource());

        assertThat(results.getAttachmentContentAsString(found))
                .isEqualTo("PUBLISHED FILE CONTENT");
    }

    @Test
    @AllureFeatures.Parameters
    void shouldProcessParameterizedClassInvocations() {
        final AllureResults results = runClasses(ParameterisedClassTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(4);

        // invocation display names make results of the same method distinguishable
        final List<String> firstNames = testResults.stream()
                .map(TestResult::getName)
                .filter(name -> name.endsWith("first()"))
                .collect(Collectors.toList());
        assertThat(firstNames)
                .hasSize(2)
                .doesNotHaveDuplicates()
                .allMatch(name -> name.startsWith("["));

        // invocations of the same method share a test case id free of invocation segments
        final List<String> firstCaseIds = testResults.stream()
                .filter(testResult -> testResult.getName().endsWith("first()"))
                .map(TestResult::getTestCaseId)
                .collect(Collectors.toList());
        assertThat(firstCaseIds)
                .hasSize(2)
                .containsOnly(firstCaseIds.get(0));
        assertThat(firstCaseIds.get(0))
                .doesNotContain("class-template-invocation")
                .contains("[method:first()]");

        final List<String> secondCaseIds = testResults.stream()
                .filter(testResult -> testResult.getName().endsWith("second()"))
                .map(TestResult::getTestCaseId)
                .collect(Collectors.toList());
        assertThat(secondCaseIds)
                .doesNotContain(firstCaseIds.get(0));

        // every invocation carries a distinct hidden UniqueId parameter
        final List<Parameter> uniqueIdParameters = testResults.stream()
                .flatMap(testResult -> testResult.getParameters().stream())
                .filter(parameter -> "UniqueId".equals(parameter.getName()))
                .collect(Collectors.toList());
        assertThat(uniqueIdParameters)
                .hasSize(4)
                .allMatch(parameter -> Parameter.Mode.HIDDEN.equals(parameter.getMode()));
        assertThat(uniqueIdParameters)
                .extracting(Parameter::getValue)
                .doesNotHaveDuplicates();
    }

    @Test
    @AllureFeatures.Fixtures
    void shouldLinkTestsToTheirScopes() {
        final AllureResults results = runClasses(PassedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3);

        final List<String> uuids = testResults.stream()
                .map(TestResult::getUuid)
                .collect(Collectors.toList());

        final List<TestResultContainer> containers = results.getTestResultContainers();

        // the class scope references every test of the class
        assertThat(containers)
                .filteredOn(container -> container.getChildren().containsAll(uuids))
                .hasSize(1);

        // every test also gets a method scope of its own
        uuids.forEach(
                uuid -> assertThat(containers)
                        .filteredOn(container -> container.getChildren().equals(List.of(uuid)))
                        .hasSize(1)
        );
    }

    @Test
    @AllureFeatures.Fixtures
    void shouldLinkFailedContainerFakeTestToItsScope() {
        final AllureResults results = runClasses(BrokenInBeforeAllTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final String uuid = testResults.get(0).getUuid();
        assertThat(results.getTestResultContainers())
                .filteredOn(container -> container.getChildren().contains(uuid))
                .hasSize(1);
    }
}
