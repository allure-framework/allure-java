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
package io.qameta.allure.junit4;

import io.qameta.allure.Step;
import io.qameta.allure.junit4.samples.ActualExpectedStatusDetailsTest;
import io.qameta.allure.junit4.samples.AssumptionFailedTest;
import io.qameta.allure.junit4.samples.BrokenTest;
import io.qameta.allure.junit4.samples.BrokenWithoutMessageTest;
import io.qameta.allure.junit4.samples.DescriptionsJavadoc;
import io.qameta.allure.junit4.samples.FailedTest;
import io.qameta.allure.junit4.samples.FilterSimpleTests;
import io.qameta.allure.junit4.samples.FlakyMutedOnClassTest;
import io.qameta.allure.junit4.samples.FlakyMutedTest;
import io.qameta.allure.junit4.samples.IgnoredClassTest;
import io.qameta.allure.junit4.samples.IgnoredTests;
import io.qameta.allure.junit4.samples.MetaAnnotationTest;
import io.qameta.allure.junit4.samples.OneTest;
import io.qameta.allure.junit4.samples.RuntimeParametersTest;
import io.qameta.allure.junit4.samples.SeverityTest;
import io.qameta.allure.junit4.samples.TaggedTests;
import io.qameta.allure.junit4.samples.TestBasedOnSampleRunner;
import io.qameta.allure.junit4.samples.TestWithAnnotations;
import io.qameta.allure.junit4.samples.TestWithSteps;
import io.qameta.allure.junit4.samples.TestWithTimeout;
import io.qameta.allure.junit4.samples.TheoriesTest;
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
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanV1_0;
import org.junit.jupiter.api.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

import java.util.Collections;
import java.util.List;

import static io.qameta.allure.junit4.samples.TaggedTests.CLASS_TAG1;
import static io.qameta.allure.junit4.samples.TaggedTests.CLASS_TAG2;
import static io.qameta.allure.junit4.samples.TaggedTests.METHOD_TAG1;
import static io.qameta.allure.junit4.samples.TaggedTests.METHOD_TAG2;
import static io.qameta.allure.test.AllureTestCommonsUtils.expectedHistoryId;
import static io.qameta.allure.util.ResultsUtils.HOST_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.SEVERITY_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.THREAD_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.md5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IsolatedLifecycle
class AllureJunit4Test {

    @Test
    @AllureFeatures.History
    @AllureFeatures.Parameters
    void shouldCalculateHistoryIdFromRuntimeParametersAtTestEnd() {
        final AllureResults results = runClasses(RuntimeParametersTest.class);
        final String testIdentifier = RuntimeParametersTest.class.getName() + "runtimeParameters";
        final String testCaseId = md5(testIdentifier);
        final String historyId = expectedHistoryId(
                testCaseId,
                List.of(
                        new Parameter().setName("runtime").setValue("included"),
                        new Parameter().setName("ignored").setValue("excluded").setExcluded(true)
                )
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getTestCaseId, TestResult::getHistoryId)
                .containsExactly(tuple("runtimeParameters", testCaseId, historyId));
    }

    @Test
    @AllureFeatures.FullName
    void shouldSetTestFullName() {
        final AllureResults results = runClasses(OneTest.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getFullName)
                .containsExactly("io.qameta.allure.junit4.samples.OneTest.simpleTest");
        assertThat(testResults.get(0).getTitlePath())
                .containsExactly(
                        "io", "qameta", "allure", "junit4", "samples",
                        "Should be overwritten by method annotation"
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
    @AllureFeatures.Timings
    void shouldSetTestStart() {
        final AllureResults results = runClasses(OneTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStart)
                .isNotNull();
    }

    @Test
    @AllureFeatures.Timings
    void shouldSetTestStop() {
        final AllureResults results = runClasses(OneTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStop)
                .isNotNull();
    }

    @Test
    @AllureFeatures.Stages
    void shouldSetStageFinished() {
        final AllureResults results = runClasses(OneTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStage)
                .containsExactly(Stage.FINISHED);
    }

    @Test
    @AllureFeatures.PassedTests
    void shouldSetPassedStatus() {
        final AllureResults results = runClasses(OneTest.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.PASSED);
    }

    @Test
    @AllureFeatures.FailedTests
    void shouldProcessFailedTest() {
        final AllureResults results = runClasses(FailedTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.FAILED);
    }

    @Test
    @AllureFeatures.FailedTests
    void shouldReportActualAndExpectedStatusDetails() {
        final AllureResults results = runClasses(ActualExpectedStatusDetailsTest.class);

        assertThat(results.getTestResults())
                .filteredOn(
                        "fullName",
                        "io.qameta.allure.junit4.samples.ActualExpectedStatusDetailsTest.failingComparison"
                )
                .extracting(
                        TestResult::getStatus,
                        testResult -> testResult.getStatusDetails().getActual(),
                        testResult -> testResult.getStatusDetails().getExpected()
                )
                .containsExactly(
                        tuple(Status.FAILED, "actual value", "expected value")
                );
    }

    @Test
    @AllureFeatures.BrokenTests
    void shouldProcessBrokenTest() {
        final AllureResults results = runClasses(BrokenTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.BROKEN);
        assertThat(testResults.get(0).getStatusDetails())
                .hasFieldOrPropertyWithValue("message", "Hello, everybody")
                .hasFieldOrProperty("trace");
    }

    @Test
    @AllureFeatures.BrokenTests
    void shouldProcessBrokenWithoutMessageTest() {
        final AllureResults results = runClasses(BrokenWithoutMessageTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.BROKEN);
        assertThat(testResults.get(0).getStatusDetails())
                .hasFieldOrPropertyWithValue("message", "java.lang.RuntimeException")
                .hasFieldOrProperty("trace");
    }

    @Test
    @AllureFeatures.SkippedTests
    void shouldProcessSkippedTest() {
        final AllureResults results = runClasses(AssumptionFailedTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.SKIPPED);
    }

    @Test
    @AllureFeatures.IgnoredTests
    void shouldProcessIgnoredTest() {
        final AllureResults results = runClasses(IgnoredTests.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .allSatisfy(testResult -> {
                    assertThat(testResult.getStatus()).isEqualTo(Status.SKIPPED);
                    assertThat(testResult.getStage()).isEqualTo(Stage.FINISHED);
                });
        final String ignoredTestId = md5(IgnoredTests.class.getName() + "ignoredTest");
        final String ignoredWithDescriptionTestId = md5(
                IgnoredTests.class.getName() + "ignoredWithDescriptionTest"
        );
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getTestCaseId, TestResult::getHistoryId)
                .containsExactlyInAnyOrder(
                        tuple("ignoredTest", ignoredTestId, md5(ignoredTestId)),
                        tuple(
                                "ignoredWithDescriptionTest",
                                ignoredWithDescriptionTestId,
                                md5(ignoredWithDescriptionTestId)
                        )
                );
    }

    @Test
    @AllureFeatures.IgnoredTests
    void shouldProcessIgnoredTestDescription() {
        final AllureResults results = runClasses(IgnoredTests.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactlyInAnyOrder("Test ignored (without reason)!", "Ignored for some reason");
    }

    @Test
    @AllureFeatures.IgnoredTests
    @DisplayName("Test result for ignored class gets named by the class name")
    void shouldSetNameForIgnoredClass() {
        final AllureResults results = runClasses(IgnoredClassTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactly("io.qameta.allure.junit4.samples.IgnoredClassTest");
    }

    @Test
    @AllureFeatures.Steps
    @DisplayName("Test with steps")
    void shouldAddStepsToTest() {
        final AllureResults results = runClasses(TestWithSteps.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .hasSize(3)
                .extracting(StepResult::getName)
                .containsExactly("step1", "step2", "step3");
    }

    @Test
    @AllureFeatures.Steps
    void testWithTimeoutAndSteps() {
        final AllureResults results = runClasses(TestWithTimeout.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .extracting(StepResult::getName)
                .containsExactly("Step 1", "Step 2");
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    void shouldProcessMethodAnnotations() {
        final AllureResults results = runClasses(TestWithAnnotations.class);
        List<TestResult> testResults = results.getTestResults();
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
    @AllureFeatures.Severity
    void shouldProcessSeverityAnnotation() {
        final AllureResults results = runClasses(SeverityTest.class);
        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .filteredOn(TestResult::getName, "criticalSeverityTest")
                .flatExtracting(TestResult::getLabels)
                .filteredOn(Label::getName, SEVERITY_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactly("critical");

        assertThat(testResults)
                .filteredOn(TestResult::getName, "defaultSeverityTest")
                .flatExtracting(TestResult::getLabels)
                .filteredOn(Label::getName, SEVERITY_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactly("trivial");
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    void shouldProcessFlakyAndMutedAnnotations() {
        final AllureResults results = runClasses(FlakyMutedTest.class);
        assertThat(results.getTestResults())
                .extracting(
                        TestResult::getName,
                        tr -> tr.getStatusDetails().isFlaky(),
                        tr -> tr.getStatusDetails().isMuted()
                )
                .contains(tuple("passedFlakyMutedTest", true, true));
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    void shouldKeepFlakyAndMutedForFailedTest() {
        final AllureResults results = runClasses(FlakyMutedTest.class);
        assertThat(results.getTestResults())
                .filteredOn(TestResult::getName, "failedFlakyMutedTest")
                .extracting(
                        TestResult::getStatus,
                        tr -> tr.getStatusDetails().isFlaky(),
                        tr -> tr.getStatusDetails().isMuted()
                )
                .containsExactly(tuple(Status.FAILED, true, true));
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    void shouldProcessFlakyAndMutedAnnotationsOnClass() {
        final AllureResults results = runClasses(FlakyMutedOnClassTest.class);
        assertThat(results.getTestResults())
                .extracting(
                        TestResult::getName,
                        tr -> tr.getStatusDetails().isFlaky(),
                        tr -> tr.getStatusDetails().isMuted()
                )
                .containsExactly(tuple("someTest", true, true));
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    void shouldSupportFlakyMutedSeverityAsMetaAnnotation() {
        final AllureResults results = runClasses(MetaAnnotationTest.class);
        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .extracting(tr -> tr.getStatusDetails().isFlaky(), tr -> tr.getStatusDetails().isMuted())
                .containsExactly(tuple(true, true));

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(Label::getName, SEVERITY_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactly("critical");
    }

    @Test
    @AllureFeatures.DisplayName
    void shouldSetDisplayName() {
        final AllureResults results = runClasses(OneTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getName)
                .containsExactly("Simple test");
    }

    @Test
    @AllureFeatures.Trees
    void shouldSetSuiteName() {
        final AllureResults results = runClasses(OneTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "suite".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("Should be overwritten by method annotation");
    }

    @Test
    @AllureFeatures.Descriptions
    void shouldSetDescription() {
        final AllureResults results = runClasses(OneTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getDescription)
                .containsExactly("Verifies that JUnit 4 reads an explicit @Description value from the test method.");
    }

    @Test
    @AllureFeatures.Links
    void shouldSetLinks() {
        final AllureResults results = runClasses(FailedTest.class);
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .containsExactlyInAnyOrder("link-1", "link-2", "issue-1", "issue-2", "tms-1", "tms-2");
    }

    @Test
    @AllureFeatures.MarkerAnnotations
    void shouldSetTags() {
        final AllureResults results = runClasses(TaggedTests.class);
        List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "tag".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder(CLASS_TAG1, CLASS_TAG2, METHOD_TAG1, METHOD_TAG2);
    }

    @Test
    @AllureFeatures.Base
    void shouldProcessTestFromDefaultPackage() throws Exception {
        Class<?> testInDefaultPackage = Class.forName("SampleTestInDefaultPackage");
        final AllureResults results = runClasses(testInDefaultPackage);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getFullName, TestResult::getStatus)
                .containsExactly(
                        tuple("SampleTestInDefaultPackage.testMethod", Status.PASSED)
                );
    }

    @Test
    @AllureFeatures.Base
    void shouldSupportTestsNotBasedOnClasses() {
        final AllureResults results = runClasses(TestBasedOnSampleRunner.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(
                        tuple("Some human readable name", Status.PASSED)
                );
    }

    @Test
    @AllureFeatures.FullName
    void shouldFilterByFullName() {
        final AllureResults results = runClasses(
                new TestPlanV1_0()
                        .setTests(
                                Collections.singletonList(
                                        new TestPlanV1_0.TestCase()
                                                .setSelector("io.qameta.allure.junit4.samples.OneTest.simpleTest")
                                )
                        ),
                OneTest.class, FailedTest.class,
                TaggedTests.class, BrokenTest.class
        );
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getFullName)
                .containsExactly("io.qameta.allure.junit4.samples.OneTest.simpleTest");
    }

    @Test
    @AllureFeatures.FullName
    void shouldFilterByAllureId() {
        final AllureResults results = runClasses(
                new TestPlanV1_0()
                        .setTests(
                                Collections.singletonList(
                                        new TestPlanV1_0.TestCase()
                                                .setId("771")
                                                .setSelector("invalid")
                                )
                        ),
                OneTest.class, FailedTest.class,
                TaggedTests.class, BrokenTest.class,
                FilterSimpleTests.class
        );
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getFullName)
                .containsExactly("io.qameta.allure.junit4.samples.FilterSimpleTests.test3");
    }

    @Test
    void shouldProcessJavadocDescriptions() {
        final AllureResults results = runClasses(DescriptionsJavadoc.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getDescription)
                .containsExactlyInAnyOrder(
                        tuple("simpleTest", "Runs a JUnit 4 test whose JavaDoc is used as the Allure description.")
                );
    }

    @Test
    void shouldProcessJavadocDescriptionsInTheories() {
        final AllureResults results = runClasses(TheoriesTest.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getDescription)
                .containsExactlyInAnyOrder(
                        tuple(
                                "simpleTest",
                                "Runs a JUnit 4 theory for each data point while preserving the JavaDoc description."
                        )
                );
    }

    @Step("Run classes {classes}")
    private AllureResults runClasses(final Class<?>... classes) {
        return runClasses(null, classes);
    }

    @Step("Run classes {classes}")
    private AllureResults runClasses(final TestPlan testPlan, final Class<?>... classes) {
        return RunUtils.runTests(lifecycle -> {
            final JUnitCore core = new JUnitCore();
            core.addListener(new AllureJunit4(lifecycle));

            final Request request = Request
                    .classes(classes)
                    .filterWith(new AllureJunit4Filter(testPlan));

            core.run(request);
        });
    }
}
