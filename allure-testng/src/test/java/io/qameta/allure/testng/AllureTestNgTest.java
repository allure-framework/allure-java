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
package io.qameta.allure.testng;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Issue;
import io.qameta.allure.Param;
import io.qameta.allure.Step;
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
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.model.WithSteps;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanV1_0;
import io.qameta.allure.testng.config.AllureTestNgConfig;
import io.qameta.allure.testng.samples.CustomListenerAttachments;
import io.qameta.allure.testng.samples.PriorityTests;
import io.qameta.allure.testng.samples.RuntimeParametersTest;
import io.qameta.allure.testng.samples.SuccessPercentageTest;
import io.qameta.allure.testng.samples.TestsWithIdForFilter;
import org.assertj.core.api.Condition;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestNG;
import org.testng.annotations.Parameters;
import org.testng.internal.ConstructorOrMethod;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.test.AllureTestCommonsUtils.expectedHistoryId;
import static io.qameta.allure.util.ResultsUtils.ALLURE_SEPARATE_LINES_SYSPROP;
import static io.qameta.allure.util.ResultsUtils.md5;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@SuppressWarnings("deprecation")
@IsolatedLifecycle
public class AllureTestNgTest {

    private static final Condition<List<? extends FixtureResult>> ALL_FINISHED = new Condition<>(
            items -> items.stream().allMatch(item -> item.getStage() == Stage.FINISHED),
            "All items should have be in a finished stage"
    );

    private static final Condition<List<? extends WithSteps>> WITH_STEPS = new Condition<>(
            items -> items.stream().allMatch(item -> item.getSteps().size() == 1),
            "All items should have a step attached"
    );

    @Test
    @AllureFeatures.History
    @AllureFeatures.Parameters
    public void shouldCalculateHistoryIdFromRuntimeParametersAtTestEnd() {
        final AllureResults results = runTestPlan(null, RuntimeParametersTest.class);
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
    @AllureFeatures.History
    public void shouldFinalizeFailuresWithinSuccessPercentage() {
        SuccessPercentageTest.resetInvocations();
        try {
            final List<TestResult> testResults = runTestPlan(null, SuccessPercentageTest.class).getTestResults();

            assertThat(testResults)
                    .hasSize(3)
                    .allSatisfy(result -> {
                        assertThat(result.getStage()).isEqualTo(Stage.FINISHED);
                        assertThat(result.getTestCaseId()).isNotBlank();
                        assertThat(result.getHistoryId())
                                .isEqualTo(expectedHistoryId(result.getTestCaseId(), result.getParameters()));
                    });
            assertThat(testResults)
                    .extracting(TestResult::getStatus)
                    .containsExactlyInAnyOrder(Status.FAILED, Status.PASSED, Status.PASSED);
            assertThat(testResults)
                    .extracting(TestResult::getTestCaseId)
                    .containsOnly(testResults.get(0).getTestCaseId());
            assertThat(testResults)
                    .extracting(TestResult::getHistoryId)
                    .doesNotHaveDuplicates();
        } finally {
            SuccessPercentageTest.resetInvocations();
        }
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> parallelConfiguration() {
        return Stream.of(
                arguments(XmlSuite.ParallelMode.NONE, 10),
                arguments(XmlSuite.ParallelMode.NONE, 5),
                arguments(XmlSuite.ParallelMode.NONE, 2),
                arguments(XmlSuite.ParallelMode.NONE, 1),
                arguments(XmlSuite.ParallelMode.METHODS, 10),
                arguments(XmlSuite.ParallelMode.METHODS, 5),
                arguments(XmlSuite.ParallelMode.METHODS, 2),
                arguments(XmlSuite.ParallelMode.METHODS, 1),
                arguments(XmlSuite.ParallelMode.CLASSES, 10),
                arguments(XmlSuite.ParallelMode.CLASSES, 5),
                arguments(XmlSuite.ParallelMode.CLASSES, 2),
                arguments(XmlSuite.ParallelMode.CLASSES, 1),
                arguments(XmlSuite.ParallelMode.INSTANCES, 10),
                arguments(XmlSuite.ParallelMode.INSTANCES, 5),
                arguments(XmlSuite.ParallelMode.INSTANCES, 2),
                arguments(XmlSuite.ParallelMode.INSTANCES, 1),
                arguments(XmlSuite.ParallelMode.TESTS, 10),
                arguments(XmlSuite.ParallelMode.TESTS, 5),
                arguments(XmlSuite.ParallelMode.TESTS, 2),
                arguments(XmlSuite.ParallelMode.TESTS, 1)
        );
    }

    @AllureFeatures.Fixtures
    @Issue("356")
    @Test
    public void shouldSetHideConfigFailProperty() {
        AllureTestNgConfig allureTestNgConfig = AllureTestNgConfig.loadConfigProperties();
        assertThat(allureTestNgConfig.isHideConfigurationFailures()).isFalse();
        allureTestNgConfig.setHideConfigurationFailures(true);
        assertThat(allureTestNgConfig.isHideConfigurationFailures()).isTrue();
    }

    @AllureFeatures.Fixtures
    @Issue("356")
    @Test
    public void shouldNotDisplayConfigurationFailsAsTests() {
        AllureTestNgConfig allureTestNgConfig = AllureTestNgConfig.loadConfigProperties();
        allureTestNgConfig.setHideConfigurationFailures(true);
        final AllureResults results = runTestNgSuites(allureTestNgConfig, "suites/gh-135.xml");
        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsOnly(tuple("someTest", Status.SKIPPED));
    }

    @Test
    public void shouldSetConfigurationProperty() {
        AllureTestNgConfig allureTestNgConfig = AllureTestNgConfig.loadConfigProperties();
        allureTestNgConfig.setHideDisabledTests(true);
        assertThat(allureTestNgConfig.isHideDisabledTests()).isTrue();
    }

    @AllureFeatures.Parallel
    @Test
    @DisplayName("Parallel data provider tests")
    public void parallelDataProvider() {
        final AllureResults results = runTestNgSuites("suites/parallel-data-provider.xml");
        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> containers = results.getTestResultContainers();
        assertThat(testResult).as("Not all testng case results have been written").hasSize(2000);
        assertThat(containers).as("Not all testng containers have been written").hasSize(3);
    }

    @AllureFeatures.Parallel
    @Issue("1013")
    @Test
    @DisplayName("Parallel data provider tests with global thread pool")
    public void globalThreadPool() {
        final AllureResults results = runTestNgSuites("suites/global-thread-pool.xml");
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .as("Not all data driven results have been written")
                .hasSize(50)
                .extracting(TestResult::getStatus)
                .as("All data driven tests should pass")
                .containsOnly(Status.PASSED);
    }

    @AllureFeatures.Base
    @Test
    @DisplayName("Singe testng")
    public void singleTest() {
        final String testName = "testWithOneStep";
        final AllureResults results = runTestNgSuites("suites/single-test.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected passed testng property")
                .hasFieldOrPropertyWithValue("status", Status.PASSED)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult.get(0).getTitlePath())
                .containsExactly(
                        "Test suite 7", "Test tag 7",
                        "io", "qameta", "allure", "testng", "samples", "TestsWithSteps"
                );
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(1)
                .extracting(StepResult::getStatus)
                .contains(Status.PASSED);
    }

    @AllureFeatures.Base
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("Test with timeout")
    public void testWithTimeout(final XmlSuite.ParallelMode mode, final int threadCount) {

        final String testNameWithTimeout = "testWithTimeout";
        final String testNameWithoutTimeout = "testWithoutTimeout";
        final AllureResults results = runTestNgSuites(parallel(mode, threadCount), "suites/tests-with-timeout.xml");
        List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .as("Test case results have not been written")
                .hasSize(2)
                .as("Unexpectedly passed status or stage of tests")
                .allMatch(
                        testResult -> testResult.getStatus().equals(Status.PASSED) &&
                                testResult.getStage().equals(Stage.FINISHED)
                )
                .extracting(TestResult::getName)
                .as("Unexpectedly passed name of tests")
                .containsOnlyElementsOf(
                        asList(
                                testNameWithoutTimeout,
                                testNameWithTimeout
                        )
                );
        assertThat(testResults)
                .flatExtracting(TestResult::getSteps)
                .as("No steps present for test with timeout")
                .hasSize(2)
                .extracting(StepResult::getName)
                .containsOnlyElementsOf(
                        asList(
                                "Step of the test with timeout",
                                "Step of the test with no timeout"
                        )
                );
    }

    @AllureFeatures.Descriptions
    @Test
    @DisplayName("Javadoc description with line separation")
    public void descriptionsWithLineSeparationTest() {
        String initialSeparateLines = System.getProperty(ALLURE_SEPARATE_LINES_SYSPROP);
        if (!Boolean.parseBoolean(initialSeparateLines)) {
            System.setProperty(ALLURE_SEPARATE_LINES_SYSPROP, "true");
        }
        try {
            final String testDescription = "Runs a TestNG test whose JavaDoc contains multiple summary lines.  \n"
                    + "- verifies the first summary line  \n"
                    + "- verifies the following list items";
            final AllureResults results = runTestNgSuites("suites/descriptions-test.xml");
            List<TestResult> testResult = results.getTestResults();

            assertThat(testResult).as("Test case result has not been written")
                    .hasSize(2)
                    .filteredOn(result -> result.getName().equals("testSeparated"))
                    .extracting(TestResult::getDescription, TestResult::getDescriptionHtml)
                    .as("Javadoc description of test case has not been processed correctly")
                    .contains(tuple(testDescription, null));
        } finally {
            System.setProperty(ALLURE_SEPARATE_LINES_SYSPROP, String.valueOf(initialSeparateLines));
        }
    }

    @AllureFeatures.Descriptions
    @Test
    @DisplayName("Javadoc description of tests")
    public void descriptionsTest() {
        final String testDescription = "Runs a TestNG test that records a step through the sample fixture.";
        final AllureResults results = runTestNgSuites("suites/descriptions-test.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written")
                .hasSize(2)
                .filteredOn(result -> result.getName().equals("test"))
                .extracting(TestResult::getDescription, TestResult::getDescriptionHtml)
                .as("Javadoc description of test case has not been processed")
                .contains(tuple(testDescription, null));
    }

    @AllureFeatures.Descriptions
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("Javadoc description of befores")
    public void descriptionsBefores(final XmlSuite.ParallelMode mode, final int threadCount) {
        final String beforeClassDescription = "Initializes the TestNG sample class and verifies class fixture descriptions are available.";
        final String beforeMethodDescription = "Initializes each TestNG sample method and verifies method fixture descriptions are available.";
        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/descriptions-test.xml"
        );
        final List<TestResultContainer> testContainers = results.getTestResultContainers();

        assertThat(testContainers).as("Test containers has not been written")
                .isNotEmpty()
                .filteredOn(container -> !container.getBefores().isEmpty())
                .extracting(
                        container -> container.getBefores().get(0).getDescription(),
                        container -> container.getBefores().get(0).getDescriptionHtml()
                )
                .as("Javadoc description of befores have not been processed")
                .containsOnly(
                        tuple(beforeClassDescription, null),
                        tuple(beforeMethodDescription, null)
                );
    }

    @AllureFeatures.Descriptions
    @Test
    @DisplayName("Javadoc description of befores with the same names")
    public void javadocDescriptionsOfBeforesWithTheSameNames() {
        final AllureResults results = runTestNgSuites("suites/descriptions-test-two-classes.xml");
        List<TestResultContainer> testContainers = results.getTestResultContainers();

        checkBeforeJavadocDescriptions(
                results,
                "io.qameta.allure.testng.samples.DescriptionsTest",
                "setUpMethod",
                "Initializes each TestNG sample method and verifies method fixture descriptions are available."
        );
        checkBeforeJavadocDescriptions(
                results,
                "io.qameta.allure.testng.samples.DescriptionsTest",
                "setUpClass",
                "Initializes the TestNG sample class and verifies class fixture descriptions are available."
        );

        checkBeforeJavadocDescriptions(
                results,
                "io.qameta.allure.testng.samples.DescriptionsAnotherTest",
                "setUpMethod",
                "Initializes each secondary TestNG sample method and verifies method fixture descriptions are available."
        );
        checkBeforeJavadocDescriptions(
                results,
                "io.qameta.allure.testng.samples.DescriptionsAnotherTest",
                "setUpClass",
                "Initializes the secondary TestNG sample class and verifies class fixture descriptions are available."
        );
    }

    @AllureFeatures.Descriptions
    @Test
    @DisplayName("Javadoc description of tests with the same names")
    public void javadocDescriptionsOfTestsWithTheSameNames() {
        final AllureResults results = runTestNgSuites("suites/descriptions-test-two-classes.xml");
        List<TestResult> testResults = results.getTestResults();

        checkTestJavadocDescriptions(
                testResults,
                "io.qameta.allure.testng.samples.DescriptionsTest.test",
                "Runs a TestNG test that records a step through the sample fixture."
        );

        checkTestJavadocDescriptions(
                testResults,
                "io.qameta.allure.testng.samples.DescriptionsAnotherTest.test",
                "Runs a secondary TestNG test that records a step through the sample fixture."
        );
    }

    @AllureFeatures.FailedTests
    @Test
    @DisplayName("Test failing by assertion")
    public void failingByAssertion() {
        String testName = "failingByAssertion";
        final AllureResults results = runTestNgSuites("suites/failing-by-assertion.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected failed testng property")
                .hasFieldOrPropertyWithValue("status", Status.FAILED)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .extracting(StepResult::getStatus)
                .contains(Status.PASSED, Status.FAILED);
    }

    @AllureFeatures.BrokenTests
    @Test
    @DisplayName("Broken testng")
    public void brokenTest() {
        String testName = "brokenTest";
        final AllureResults results = runTestNgSuites("suites/broken.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected broken testng property")
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult.get(0).getStatusDetails()).as("Test Status Details")
                .hasFieldOrPropertyWithValue("message", "Exception")
                .hasFieldOrProperty("trace");
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .extracting(StepResult::getStatus)
                .contains(Status.PASSED, Status.BROKEN);
    }

    @AllureFeatures.BrokenTests
    @Test
    @DisplayName("Broken testng - Exception without message")
    public void brokenTestWithOutMessage() {
        String testName = "brokenTestWithoutMessage";
        final AllureResults results = runTestNgSuites("suites/brokenWithoutMessage.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected broken testng property")
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult.get(0).getStatusDetails()).as("Test Status Details")
                .hasFieldOrPropertyWithValue("message", "java.lang.RuntimeException")
                .hasFieldOrProperty("trace");

        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .extracting(StepResult::getStatus)
                .contains(Status.PASSED, Status.BROKEN);
    }

    @AllureFeatures.Fixtures
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("Suite fixtures")
    public void perSuiteFixtures(final XmlSuite.ParallelMode mode, final int threadCount) {
        String before1 = "beforeSuite1";
        String before2 = "beforeSuite2";
        String after1 = "afterSuite1";
        String after2 = "afterSuite2";

        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/per-suite-fixtures-combination.xml"
        );

        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestResultContainers();

        assertThat(testResult).as("Unexpected quantity of testng case results has been written").hasSize(1);
        List<String> testUuid = singletonList(testResult.get(0).getUuid());

        // test tag and class scopes carry no fixtures; the suite scope is found through its fixtures
        assertGroupingContainersWithChildren(testContainers, testUuid, 2);
        assertContainersChildrenByFixture(before1, testContainers, testUuid);
        assertBeforeFixtures(testContainers, before1, before2);
        assertAfterFixtures(testContainers, after1, after2);
    }

    @AllureFeatures.Fixtures
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("Class fixtures")
    public void perClassFixtures(final XmlSuite.ParallelMode mode, final int threadCount) {
        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/per-class-fixtures-combination.xml"
        );
        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("test1", "test2");

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(FixtureResult::getName)
                .containsExactlyInAnyOrder("beforeClass");

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .extracting(FixtureResult::getName)
                .containsExactlyInAnyOrder("afterClass");

        final TestResult test1 = findTestResultByName(results, "test1");
        final TestResult test2 = findTestResultByName(results, "test2");

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getChildren)
                .contains(test1.getUuid(), test2.getUuid());
    }

    @AllureFeatures.Fixtures
    @Issue("896")
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("Class fixtures of factory-created instances")
    public void perInstanceClassFixtures(final XmlSuite.ParallelMode mode, final int threadCount) {
        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/factory-class-fixtures.xml"
        );

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults).hasSize(6);

        // tests created from the same factory instance share the instance parameter value
        final Map<String, String> instanceByUuid = testResults.stream()
                .collect(
                        Collectors.toMap(
                                TestResult::getUuid,
                                result -> result.getParameters().stream()
                                        .filter(parameter -> "param".equals(parameter.getName()))
                                        .map(Parameter::getValue)
                                        .findFirst()
                                        .orElse("")
                        )
                );

        final List<TestResultContainer> classScopes = findContainersByFixtureName(
                results.getTestResultContainers(), "beforeClass"
        );
        assertThat(classScopes)
                .as("Each factory-created instance should get its own class scope")
                .hasSize(3);

        assertThat(classScopes).allSatisfy(scope -> {
            assertThat(scope.getBefores())
                    .extracting(FixtureResult::getName)
                    .containsExactly("beforeClass");
            assertThat(scope.getAfters())
                    .extracting(FixtureResult::getName)
                    .containsExactly("afterClass");
            final Set<String> instances = scope.getChildren().stream()
                    .map(instanceByUuid::get)
                    .collect(Collectors.toSet());
            assertThat(scope.getChildren()).hasSize(2);
            assertThat(instances)
                    .as("Class scope should hold the tests of a single instance")
                    .hasSize(1);
        });

        assertThat(classScopes)
                .flatExtracting(TestResultContainer::getChildren)
                .as("Class scopes should partition all tests without overlap")
                .containsExactlyInAnyOrderElementsOf(instanceByUuid.keySet());
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Before method fixture metadata propagates to the test")
    public void beforeMethodFixtureMetadata() {
        final AllureResults results = runTestNgSuites("suites/before-method-metadata.xml");

        // the per-method scope is written at test start, before the test stops — the label set
        // in the fixture must still reach the test result
        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "layer".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("rest");
    }

    @AllureFeatures.Base
    @Test
    @DisplayName("Suite label from the runtime api replaces the default suite label")
    public void runtimeApiSuiteLabelReplacesDefault() {
        final AllureResults results = runTestNgSuites("suites/runtime-suite-label.xml");

        assertThat(results.getTestResults()).hasSize(1);
        final List<Label> labels = results.getTestResults().get(0).getLabels();

        // the default suite label is applied at test stop only when the user has not set one
        assertThat(labels)
                .filteredOn(label -> "suite".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("Runtime Suite");

        // defaults for names the user did not touch are still applied
        assertThat(labels)
                .filteredOn(label -> "parentSuite".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("Runtime suite label suite");
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Suite label from a before method fixture replaces the default suite label")
    public void beforeMethodSuiteLabelReplacesDefault() {
        final AllureResults results = runTestNgSuites("suites/before-method-runtime-suite-label.xml");

        // the per-method scope metadata is merged into the test before default labels apply,
        // so the fixture-provided suite wins over the default one
        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "suite".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("Fixture Suite");
    }

    @AllureFeatures.Base
    @Test
    @DisplayName("Labels from the runtime api do not replace the system labels")
    public void runtimeApiLabelsKeepSystemLabels() {
        final AllureResults results = runTestNgSuites("suites/runtime-system-labels.xml");

        assertThat(results.getTestResults()).hasSize(1);
        final List<Label> labels = results.getTestResults().get(0).getLabels();

        // package, testClass, and testMethod are system labels: they are set eagerly
        // and are not replaced by user values
        assertThat(labels)
                .filteredOn(label -> "package".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder(
                        "io.qameta.allure.testng.samples.RuntimeSystemLabels",
                        "Runtime Package"
                );

        assertThat(labels)
                .filteredOn(label -> "testClass".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder(
                        "io.qameta.allure.testng.samples.RuntimeSystemLabels",
                        "Runtime Test Class"
                );

        assertThat(labels)
                .filteredOn(label -> "testMethod".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder(
                        "testWithRuntimeSystemLabels",
                        "Runtime Test Method"
                );
    }

    @AllureFeatures.Fixtures
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("Method fixtures")
    public void perMethodFixtures(final XmlSuite.ParallelMode mode, final int threadCount) {
        String before1 = "beforeMethod1";
        String before2 = "beforeMethod2";
        String after1 = "afterMethod1";
        String after2 = "afterMethod2";

        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/per-method-fixtures-combination.xml"
        );

        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestResultContainers();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written").hasSize(2);
        List<String> uuids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());

        // test tag, suite, and class scopes are the fixture-less grouping containers
        assertGroupingContainersWithChildren(testContainers, uuids, 3);
        assertContainersPerMethod(before1, testContainers, uuids);
        assertContainersPerMethod(before2, testContainers, uuids);
        assertContainersPerMethod(after1, testContainers, uuids);
        assertContainersPerMethod(after2, testContainers, uuids);
    }

    @AllureFeatures.Fixtures
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("Test fixtures")
    public void perTestTagFixtures(final XmlSuite.ParallelMode mode, final int threadCount) {
        String before1 = "beforeTest1";
        String before2 = "beforeTest2";
        String after1 = "afterTest1";
        String after2 = "afterTest2";

        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/per-test-fixtures-combination.xml"
        );

        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestResultContainers();

        assertThat(testResult).as("Unexpected quantity of testng case results has been written").hasSize(1);
        List<String> testUuid = singletonList(testResult.get(0).getUuid());

        // suite and class scopes carry no fixtures; the test tag scope is found through its fixtures
        assertGroupingContainersWithChildren(testContainers, testUuid, 2);
        assertContainersChildrenByFixture(before1, testContainers, testUuid);
        assertBeforeFixtures(testContainers, before1, before2);
        assertAfterFixtures(testContainers, after1, after2);
    }

    @AllureFeatures.Fixtures
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("Group fixtures")
    public void perGroupFixtures(final XmlSuite.ParallelMode mode, final int threadCount) {
        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/per-group-fixtures-combination.xml"
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("test1", "test2", "testWithoutGroup");

        final TestResult test1 = findTestResultByName(results, "test1");
        final TestResult test2 = findTestResultByName(results, "test2");

        final List<TestResultContainer> containers = results.getTestResultContainers();
        assertBeforeFixtures(containers, "beforeGroup");
        assertAfterFixtures(containers, "afterGroup");

        final List<TestResultContainer> groupScopes = findContainersByFixtureName(containers, "beforeGroup");
        assertThat(groupScopes)
                .as("Group fixtures declaring the same groups should share a single scope")
                .hasSize(1);
        final TestResultContainer groupScope = groupScopes.get(0);
        assertThat(groupScope.getAfters())
                .extracting(FixtureResult::getName)
                .containsExactly("afterGroup");
        assertThat(groupScope.getChildren())
                .as("Group scope should list only the tests belonging to the group")
                .containsExactlyInAnyOrder(test1.getUuid(), test2.getUuid());
    }

    @AllureFeatures.Fixtures
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("After groups fixture without matching before groups fixture")
    public void afterGroupsFixtureWithoutBeforeGroupsFixture(final XmlSuite.ParallelMode mode,
                                                             final int threadCount) {
        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/after-group-fixtures.xml"
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("test1", "test2");

        final TestResult test1 = findTestResultByName(results, "test1");
        final TestResult test2 = findTestResultByName(results, "test2");

        final List<TestResultContainer> containers = results.getTestResultContainers();
        assertAfterFixtures(containers, "afterGroup");

        final List<TestResultContainer> groupScopes = findContainersByFixtureName(containers, "afterGroup");
        assertThat(groupScopes)
                .as("After groups fixture should be reported in a single scope")
                .hasSize(1);
        assertThat(groupScopes.get(0).getChildren())
                .as("Tests already finished when the scope appears should be linked to it by uuid")
                .containsExactlyInAnyOrder(test1.getUuid(), test2.getUuid());
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("After groups fixture declaring multiple groups")
    public void afterGroupsFixtureDeclaringMultipleGroups() {
        final AllureResults results = runTestNgSuites("suites/after-groups-multiple-groups.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("test1", "test2", "testInBothGroups");

        final TestResult test1 = findTestResultByName(results, "test1");
        final TestResult test2 = findTestResultByName(results, "test2");
        final TestResult testInBothGroups = findTestResultByName(results, "testInBothGroups");

        final List<TestResultContainer> containers = results.getTestResultContainers();
        assertAfterFixtures(containers, "afterGroups");

        final List<TestResultContainer> groupScopes = findContainersByFixtureName(containers, "afterGroups");
        assertThat(groupScopes)
                .as("Group fixtures declaring the same groups should share a single scope")
                .hasSize(1);
        assertThat(groupScopes.get(0).getChildren())
                .as("Tests from all declared groups should be linked exactly once")
                .containsExactlyInAnyOrder(test1.getUuid(), test2.getUuid(), testInBothGroups.getUuid());
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Mixed single-group and multi-group fixtures")
    public void mixedGroupFixtures() {
        final AllureResults results = runTestNgSuites("suites/mixed-group-fixtures.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("testA", "testB", "testAB", "testBA");

        final TestResult testA = findTestResultByName(results, "testA");
        final TestResult testB = findTestResultByName(results, "testB");
        final TestResult testAB = findTestResultByName(results, "testAB");
        final TestResult testBA = findTestResultByName(results, "testBA");

        final List<TestResultContainer> containers = results.getTestResultContainers();
        assertThat(containers)
                .filteredOn(container -> !container.getBefores().isEmpty())
                .as("Four group fixtures declaring three distinct group sets should share three scopes")
                .hasSize(3);

        final List<TestResultContainer> scopeA = findContainersByFixtureName(containers, "beforeGroupA");
        assertThat(scopeA).hasSize(1);
        assertThat(scopeA.get(0).getBefores())
                .extracting(FixtureResult::getName)
                .containsExactly("beforeGroupA");
        assertThat(scopeA.get(0).getChildren())
                .as("Scope of group a should list every test belonging to a")
                .containsExactlyInAnyOrder(testA.getUuid(), testAB.getUuid(), testBA.getUuid());

        final List<TestResultContainer> scopeB = findContainersByFixtureName(containers, "beforeGroupB");
        assertThat(scopeB).hasSize(1);
        assertThat(scopeB.get(0).getBefores())
                .extracting(FixtureResult::getName)
                .containsExactly("beforeGroupB");
        assertThat(scopeB.get(0).getChildren())
                .as("Scope of group b should list every test belonging to b")
                .containsExactlyInAnyOrder(testB.getUuid(), testAB.getUuid(), testBA.getUuid());

        final List<TestResultContainer> scopeAB = findContainersByFixtureName(containers, "beforeGroupsAB");
        assertThat(scopeAB).hasSize(1);
        // TestNG invokes a multi-group before-groups method once per declared group, so the shared
        // scope is asserted by fixture names without invocation counts
        assertThat(scopeAB.get(0).getBefores())
                .as("Fixtures declaring the same groups in different order should share the scope")
                .extracting(FixtureResult::getName)
                .containsOnly("beforeGroupsAB", "beforeGroupsBA");
        assertThat(scopeAB.get(0).getChildren())
                .as("Scope of groups a+b should list every test belonging to either group")
                .containsExactlyInAnyOrder(
                        testA.getUuid(), testB.getUuid(), testAB.getUuid(), testBA.getUuid()
                );
    }

    @AllureFeatures.Fixtures
    @Issue("953")
    @Test
    @DisplayName("Suite fixtures are linked directly to tests from all test tags")
    public void perSuiteFixturesAcrossTestTags() {
        final AllureResults results = runTestNgSuites("suites/suite-fixtures-multiple-tags.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("test", "shouldTest");

        final TestResult test = findTestResultByName(results, "test");
        final TestResult shouldTest = findTestResultByName(results, "shouldTest");
        final List<String> testUuids = asList(test.getUuid(), shouldTest.getUuid());

        final List<TestResultContainer> containers = results.getTestResultContainers();

        // the report does not resolve nested containers, so every scope must list its tests directly
        assertThat(containers)
                .allSatisfy(container -> assertThat(container.getChildren()).isSubsetOf(testUuids));

        final List<TestResultContainer> suiteScopes = findContainersByFixtureName(containers, "beforeSuite1");
        assertThat(suiteScopes).hasSize(1);
        assertThat(suiteScopes.get(0).getChildren())
                .as("Suite fixtures should apply to the tests of every test tag")
                .containsExactlyInAnyOrder(test.getUuid(), shouldTest.getUuid());
        assertThat(suiteScopes.get(0).getAfters())
                .extracting(FixtureResult::getName)
                .containsExactly("afterSuite1", "afterSuite2");
    }

    @AllureFeatures.SkippedTests
    @Test
    @DisplayName("Skipped suite")
    public void skippedSuiteTest() {
        final Condition<StepResult> skipReason = new Condition<>(
                step -> step.getStatusDetails().getTrace().startsWith("java.lang.RuntimeException: Skip all"),
                "Suite should be skipped because of an exception in before suite"
        );

        final AllureResults results = runTestNgSuites("suites/skipped-suite.xml");
        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestResultContainers();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("skippedTest", Status.SKIPPED),
                        tuple("skipSuite", Status.BROKEN),
                        tuple("testWithOneStep", Status.SKIPPED)
                );
        assertThat(testContainers)
                .as("Unexpected quantity of testng containers has been written")
                .hasSize(5);

        assertThat(findContainersByFixtureName(testContainers, "skipSuite"))
                .as("Before suite container should have a before method with one step")
                .hasSize(1)
                .flatExtracting(TestResultContainer::getBefores)
                .hasSize(1)
                .flatExtracting(FixtureResult::getSteps)
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .has(skipReason);
    }

    @AllureFeatures.Base
    @Test
    @DisplayName("Multi suites")
    public void multipleSuites() {
        String beforeMethodName = "beforeMethod";

        final AllureResults results = runTestNgSuites("suites/parameterized-test.xml", "suites/single-test.xml");

        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestResultContainers();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(3);
        List<String> uids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written")
                .hasSize(9);

        final List<String> firstSuite = uids.subList(0, 2);
        assertContainersChildrenByFixture(beforeMethodName, testContainers, firstSuite);
        // first suite: tag, suite, and class scopes group both parameterized tests
        assertGroupingContainersWithChildren(testContainers, firstSuite, 3);
        final List<String> secondSuite = singletonList(uids.get(2));
        // second suite: tag, suite, and class scopes group the single test
        assertGroupingContainersWithChildren(testContainers, secondSuite, 3);
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Parameters
    @Test
    @DisplayName("Before Suite Parameter")
    public void testBeforeSuiteParameter() {
        final AllureResults results = runTestNgSuites("suites/parameterized-suite1.xml", "suites/parameterized-suite2.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactly(
                        tuple("param", "first"),
                        tuple("parameter", "first"),
                        tuple("param", "second"),
                        tuple("parameter", "second")
                );
    }

    @AllureFeatures.Parallel
    @Test
    @DisplayName("Parallel methods")
    public void parallelMethods() {
        String before1 = "beforeMethod";
        String before2 = "beforeMethod2";
        String after = "afterMethod";

        final AllureResults results = runTestNgSuites("suites/parallel-methods.xml");
        List<TestResult> testResults = results.getTestResults();
        List<String> uids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());
        List<TestResultContainer> testContainers = results.getTestResultContainers();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(2001);
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written")
                .hasSize(6007);

        assertContainersPerMethod(before1, testContainers, uids);
        assertContainersPerMethod(before2, testContainers, uids);
        assertContainersPerMethod(after, testContainers, uids);
        // the class scope is the only fixture-less grouping container; the test tag and suite
        // scopes carry the sample's @BeforeTest/@BeforeSuite fixtures and group all the tests
        assertGroupingContainersWithChildren(testContainers, uids, 1);
        assertContainersChildrenByFixture("beforeTest", testContainers, uids);
        assertContainersChildrenByFixture("beforeSuite", testContainers, uids);
    }

    @AllureFeatures.Steps
    @Test
    @DisplayName("Nested steps")
    public void nestedSteps() {
        String beforeMethod = "beforeMethod";
        String nestedStep = "nestedStep";
        String stepInBefore = "stepTwo";
        String stepInTest = "stepThree";
        final Condition<StepResult> substep = new Condition<>(
                step -> step.getSteps().get(0).getName().equals(nestedStep),
                "Given step should have a substep with name " + nestedStep
        );

        final AllureResults results = runTestNgSuites("suites/nested-steps.xml");
        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> containers = results.getTestResultContainers();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(1);

        assertThat(findContainersByFixtureName(containers, beforeMethod))
                .flatExtracting(TestResultContainer::getBefores)
                .flatExtracting(FixtureResult::getSteps)
                .as("Before method should have a step")
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("name", stepInBefore)
                .has(substep);

        assertThat(testResults)
                .flatExtracting(TestResult::getSteps)
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("name", stepInTest)
                .has(substep);
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    @DisplayName("Flaky tests")
    public void flakyTests() {
        final AllureResults results = runTestNgSuites("suites/flaky.xml");

        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(9)
                .filteredOn(flakyPredicate())
                .extracting(TestResult::getFullName)
                .hasSize(7)
                .containsExactly(
                        "io.qameta.allure.testng.samples.FlakyMethods.flakyTest",
                        "io.qameta.allure.testng.samples.FlakyMethods.flakyTest",
                        "io.qameta.allure.testng.samples.FlakyTestClass.flakyAsWell",
                        "io.qameta.allure.testng.samples.FlakyTestClass.flakyTest",
                        "io.qameta.allure.testng.samples.FlakyTestClass.flakyAsWell",
                        "io.qameta.allure.testng.samples.FlakyTestClass.flakyTest",
                        "io.qameta.allure.testng.samples.FlakyTestClassInherited.flakyInherited"
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    @DisplayName("Muted tests")
    public void mutedTests() {
        final AllureResults results = runTestNgSuites("suites/muted.xml");

        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(9)
                .filteredOn(mutedPredicate())
                .extracting(TestResult::getFullName)
                .hasSize(7)
                .containsExactly(
                        "io.qameta.allure.testng.samples.MutedMethods.mutedTest",
                        "io.qameta.allure.testng.samples.MutedMethods.mutedTest",
                        "io.qameta.allure.testng.samples.MutedTestClass.mutedAsWell",
                        "io.qameta.allure.testng.samples.MutedTestClass.mutedTest",
                        "io.qameta.allure.testng.samples.MutedTestClass.mutedAsWell",
                        "io.qameta.allure.testng.samples.MutedTestClass.mutedTest",
                        "io.qameta.allure.testng.samples.MutedTestClassInherited.mutedInherited"
                );
    }

    @AllureFeatures.Links
    @Test
    @DisplayName("Tests with links")
    public void linksTest() {
        final AllureResults results = runTestNgSuites("suites/links.xml");

        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(4)
                .filteredOn(hasLinks())
                .hasSize(4)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .contains(
                        "testClass", "a", "b", "c", "testClassIssue", "testClassTmsLink",
                        "testClass", "nested1", "nested2", "nested3", "testClassIssue", "issue1", "issue2", "issue3",
                        "testClassTmsLink", "tms1", "tms2", "tms3", "testClass", "a", "b", "c", "testClassIssue",
                        "testClassTmsLink", "testClass", "inheritedLink1", "inheritedLink2", "testClassIssue",
                        "inheritedIssue", "testClassTmsLink", "inheritedTmsLink"
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    @DisplayName("BDD annotations")
    public void bddAnnotationsTest() {
        final AllureResults results = runTestNgSuites("suites/bdd-annotations.xml");

        List<String> bddLabels = asList("epic", "feature", "story");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> bddLabels.contains(label.getName()))
                .extracting(Label::getValue)
                .contains(
                        "epic1",
                        "epic2",
                        "feature1",
                        "feature2",
                        "story1",
                        "story2",
                        "epic-inherited",
                        "class-feature1",
                        "class-feature2",
                        "story-inherited"
                );
    }

    @AllureFeatures.Base
    @Test
    @DisplayName("Should support TestNG retries")
    public void retryTest() {
        final AllureResults results = runTestNgSuites("suites/retry.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2);
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    @DisplayName("Should support flaky, muted and severity as meta annotations")
    public void shouldSupportFlakyMutedSeverityAsMetaAnnotation() {
        final AllureResults results = runTestNgSuites("suites/meta-annotation.xml");
        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .extracting(tr -> tr.getStatusDetails().isFlaky(), tr -> tr.getStatusDetails().isMuted())
                .containsExactly(tuple(true, true));

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "severity".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("critical");
    }

    @AllureFeatures.Severity
    @Test
    @DisplayName("Should add severity for tests")
    public void severityTest() {
        final AllureResults results = runTestNgSuites("suites/severity.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(8)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "severity".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("critical", "critical", "minor", "blocker", "minor", "blocker");
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    @DisplayName("Should add owner to tests")
    public void ownerTest() {
        final AllureResults results = runTestNgSuites("suites/owner.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(
                        TestResult::getFullName, tr -> tr.getLabels()
                                .stream()
                                .filter(label -> "owner".equals(label.getName()))
                                .map(Label::getValue)
                                .sorted()
                                .collect(Collectors.joining(",", "[", "]"))
                )
                .containsExactlyInAnyOrder(
                        tuple("io.qameta.allure.testng.samples.OwnerMethodTest.testWithOwner", "[charlie]"),
                        tuple("io.qameta.allure.testng.samples.OwnerMethodTest.testWithOwner", "[charlie]"),
                        tuple("io.qameta.allure.testng.samples.OwnerMethodTest.testWithoutOwner", "[]"),
                        tuple("io.qameta.allure.testng.samples.OwnerMethodTest.testWithoutOwner", "[]"),
                        tuple("io.qameta.allure.testng.samples.OwnerClassTest.testWithOwner", "[eroshenkoam,other-guy]"),
                        tuple("io.qameta.allure.testng.samples.OwnerClassTest.testWithOwner", "[eroshenkoam,other-guy]"),
                        tuple("io.qameta.allure.testng.samples.OwnerClassTest.testWithoutOwner", "[eroshenkoam]"),
                        tuple("io.qameta.allure.testng.samples.OwnerClassTest.testWithoutOwner", "[eroshenkoam]")
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    @DisplayName("Should add tag to tests")
    public void tagTest() {
        final AllureResults results = runTestNgSuites("suites/tags.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(
                        TestResult::getFullName, tr -> tr.getLabels()
                                .stream()
                                .filter(label -> "tag".equals(label.getName()))
                                .map(Label::getValue)
                                .sorted()
                                .collect(Collectors.joining(",", "[", "]"))
                )
                .containsExactlyInAnyOrder(
                        tuple("io.qameta.allure.testng.samples.TagMethodTest.testWithTag", "[regress]"),
                        tuple("io.qameta.allure.testng.samples.TagMethodTest.testWithTags", "[regress,smoke]"),
                        tuple("io.qameta.allure.testng.samples.TagMethodTest.testWithoutTag", "[]"),
                        tuple("io.qameta.allure.testng.samples.TagClassTest.testWithoutTag", "[class-tag]"),
                        tuple("io.qameta.allure.testng.samples.TagClassTest.testWithTag", "[class-tag,method-tag-single]"),
                        tuple("io.qameta.allure.testng.samples.TagClassTest.testWithTags", "[class-tag,method-tag-1,method-tag-2]")
                );
    }

    @AllureFeatures.Attachments
    @Test
    @DisplayName("Should add attachments to tests")
    public void attachmentsTest() {
        final AllureResults results = runTestNgSuites("suites/attachments.xml");
        List<TestResult> testResults = results.getTestResults();
        // the sample's assertThat is recorded by the woven assertj integration as a step of its own
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("String attachment", "assert \"<p>HELLO</p>\"");
        assertThat(testResults)
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getAttachments)
                .hasSize(1)
                .extracting(Attachment::getName)
                .containsExactly("String attachment");
    }

    @AllureFeatures.Attachments
    @Issue("1150")
    @Test
    @DisplayName("Should keep steps and attachments added by a custom listener on test failure")
    public void shouldKeepCustomListenerEvidenceOnTestFailure() {
        final AllureResults results = runTestNgSuites("suites/custom-listener-attachments.xml");

        final TestResult failed = findTestResultByName(results, "failingTest");
        assertThat(failed.getStatus())
                .isEqualTo(Status.FAILED);
        assertThat(failed.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("On test start", "Take screenshot on failure", "Screenshot on failure");
        assertThat(failed.getSteps())
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .containsExactly("On test start", "Screenshot on failure");
    }

    @AllureFeatures.Attachments
    @Issue("1150")
    @Test
    @DisplayName("Should keep attachments added by a custom listener on test start and success")
    public void shouldKeepCustomListenerAttachmentsOnTestSuccess() {
        final AllureResults results = runTestNgSuites("suites/custom-listener-attachments.xml");

        final TestResult passed = findTestResultByName(results, "passingTest");
        assertThat(passed.getStatus())
                .isEqualTo(Status.PASSED);
        assertThat(passed.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("On test start", "On test success");
        assertThat(passed.getSteps())
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .containsExactly("On test start", "On test success");
    }

    @AllureFeatures.Attachments
    @Issue("1150")
    @Test
    @DisplayName("Should keep attachments added by a custom listener registered via suite xml")
    public void shouldKeepCustomListenerEvidenceWithXmlRegistration() {
        final AllureResults results = runTestNgSuites("suites/custom-listener-attachments-xml.xml");

        final TestResult failed = findTestResultByName(results, "failingTest");
        assertThat(failed.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("On test start", "Take screenshot on failure", "Screenshot on failure");

        final TestResult passed = findTestResultByName(results, "passingTest");
        assertThat(passed.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("On test start", "On test success");
    }

    @AllureFeatures.Base
    @Issue("1150")
    @Test
    @DisplayName("Should move the Allure listener to the front of the TestNG listener list")
    public void shouldMoveAllureListenerToFront() {
        final AllureTestNg adapter = new AllureTestNg(new AllureLifecycle(new AllureResultsWriterStub()));
        final ITestListener custom = new CustomListenerAttachments.EvidenceListener();
        final List<ITestListener> listeners = new ArrayList<>(Arrays.asList(custom, adapter));

        adapter.moveSelfToFront(listeners);

        assertThat(listeners)
                .containsExactly(adapter, custom);
    }

    @AllureFeatures.Base
    @Issue("1150")
    @Test
    @DisplayName("Should keep the default listener order when the TestNG listener list is unmodifiable")
    public void shouldNotFailOnUnmodifiableListenerList() {
        final AllureTestNg adapter = new AllureTestNg(new AllureLifecycle(new AllureResultsWriterStub()));
        final ITestListener custom = new CustomListenerAttachments.EvidenceListener();
        final List<ITestListener> listeners = Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(custom, adapter))
        );

        assertThatCode(() -> adapter.moveSelfToFront(listeners))
                .doesNotThrowAnyException();
        assertThat(listeners)
                .containsExactly(custom, adapter);
    }

    @AllureFeatures.MarkerAnnotations
    @Issue("42")
    @Test
    @DisplayName("Should process flaky for failed tests")
    public void shouldAddFlakyToFailedTests() {
        final AllureResults results = runTestNgSuites("suites/gh-42.xml");

        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .filteredOn(flakyPredicate())
                .extracting(TestResult::getFullName)
                .hasSize(1)
                .containsExactly(
                        "io.qameta.allure.testng.samples.FailedFlakyTest.flakyWithFailure"
                );
    }

    @AllureFeatures.History
    @Test
    @DisplayName("Should use parameters for history id")
    public void shouldUseParametersForHistoryIdGeneration() {
        final AllureResults results = runTestNgSuites("suites/history-id-parameters.xml");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getHistoryId)
                .doesNotHaveDuplicates();
    }

    @AllureFeatures.History
    @Test
    @DisplayName("Should generate the same history id for the same tests")
    public void shouldGenerateSameHistoryIdForTheSameTests() {
        final AllureResults results = runTestNgSuites("suites/history-id-the-same.xml");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getHistoryId)
                .hasSize(2)
                .containsOnly("d3a93ece5b6ec2223df8676cf6e82509");
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Fixtures
    @Issue("67")
    @Test
    @DisplayName("Should set correct status for fixtures")
    public void shouldSetCorrectStatusesForFixtures() {
        final AllureResults results = runTestNgSuites(
                "suites/per-suite-fixtures-combination.xml",
                "suites/per-method-fixtures-combination.xml",
                "suites/per-class-fixtures-combination.xml",
                "suites/per-test-fixtures-combination.xml",
                "suites/failed-test-passed-fixture.xml"
        );

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .hasSize(10)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("beforeSuite1", Status.PASSED),
                        Tuple.tuple("beforeSuite2", Status.PASSED),
                        Tuple.tuple("beforeMethod1", Status.PASSED),
                        Tuple.tuple("beforeMethod2", Status.PASSED),
                        Tuple.tuple("beforeMethod1", Status.PASSED),
                        Tuple.tuple("beforeMethod2", Status.PASSED),
                        Tuple.tuple("beforeClass", Status.PASSED),
                        Tuple.tuple("beforeTest1", Status.PASSED),
                        Tuple.tuple("beforeTest2", Status.PASSED),
                        Tuple.tuple("beforeTestPassed", Status.PASSED)
                );

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("afterSuite1", Status.PASSED),
                        Tuple.tuple("afterSuite2", Status.PASSED),
                        Tuple.tuple("afterMethod1", Status.PASSED),
                        Tuple.tuple("afterMethod2", Status.PASSED),
                        Tuple.tuple("afterMethod1", Status.PASSED),
                        Tuple.tuple("afterMethod2", Status.PASSED),
                        Tuple.tuple("afterClass", Status.PASSED),
                        Tuple.tuple("afterTest1", Status.PASSED),
                        Tuple.tuple("afterTest2", Status.PASSED)
                );
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Fixtures
    @Issue("67")
    @Test
    @DisplayName("Should set correct status for failed before fixtures")
    public void shouldSetCorrectStatusForFailedBeforeFixtures() {
        final AllureResults results = runTestNgSuites(
                "suites/failed-before-suite-fixture.xml",
                "suites/failed-before-test-fixture.xml",
                "suites/failed-before-method-fixture.xml"
        );

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .hasSize(3)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("beforeSuite", Status.BROKEN),
                        Tuple.tuple("beforeTest", Status.BROKEN),
                        Tuple.tuple("beforeMethod", Status.BROKEN)
                );
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Fixtures
    @Issue("67")
    @Test
    @DisplayName("Should set correct status for failed after fixtures")
    public void shouldSetCorrectStatusForFailedAfterFixtures() {
        final Consumer<TestNG> configurer = parallel(XmlSuite.ParallelMode.METHODS, 5);

        final AllureResults results = runTestNgSuites(
                configurer,
                "suites/failed-after-suite-fixture.xml",
                "suites/failed-after-test-fixture.xml",
                "suites/failed-after-method-fixture.xml"
        );

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .hasSize(3)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("afterSuite", Status.BROKEN),
                        Tuple.tuple("afterTest", Status.BROKEN),
                        Tuple.tuple("afterMethod", Status.BROKEN)
                );
    }

    @AllureFeatures.Parameters
    @Issue("97")
    @Test
    @DisplayName("Should process varargs test parameters")
    public void shouldProcessVarargsParameters() {
        final AllureResults results = runTestNgSuites("suites/gh-97.xml");

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getValue)
                .containsExactlyInAnyOrder(
                        "[a, b, c]"
                );
    }

    @AllureFeatures.Fixtures
    @Issue("99")
    @Test
    @DisplayName("Should attach class fixtures correctly")
    public void shouldAttachClassFixturesCorrectly() {
        final Consumer<TestNG> configurer = parallel(XmlSuite.ParallelMode.METHODS, 5);

        final AllureResults results = runTestNgSuites(configurer, "suites/gh-99.xml");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "classFixtures1", "classFixtures2",
                        "classFixtures3", "classFixturesInParent"
                );

        final TestResult classFixtures1 = findTestResultByName(results, "classFixtures1");
        final TestResult classFixtures2 = findTestResultByName(results, "classFixtures2");
        final TestResult classFixtures3 = findTestResultByName(results, "classFixtures3");
        final TestResult classFixturesInParent = findTestResultByName(results, "classFixturesInParent");

        // each class scope carries its class fixtures and exactly its own test
        Stream.of(classFixtures1, classFixtures2, classFixtures3, classFixturesInParent)
                .forEach(
                        testResult -> assertThat(findFixtureContainersWithChild(results, testResult.getUuid()))
                                .as("Class fixture container for " + testResult.getName())
                                .hasSize(1)
                                .flatExtracting(TestResultContainer::getChildren)
                                .containsExactlyInAnyOrder(testResult.getUuid())
                );
    }

    @AllureFeatures.History
    @Issue("102")
    @Test
    @DisplayName("Should generate different history id for inherited tests")
    public void shouldGenerateDifferentHistoryIdForInheritedTests() {
        final AllureResults results = runTestNgSuites("suites/gh-102.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getHistoryId)
                .doesNotHaveDuplicates();
    }

    @AllureFeatures.Fixtures
    @Issue("101")
    @Test
    @DisplayName("Should use fixture description")
    public void shouldUseFixtureDescriptions() {
        final AllureResults results = runTestNgSuites("suites/gh-101.xml");

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(FixtureResult::getName)
                .containsExactlyInAnyOrder("Set up method with description");
    }

    @AllureFeatures.Descriptions
    @Issue("106")
    @Test
    public void shouldProcessCyrillicDescriptions() {
        final AllureResults results = runTestNgSuites("suites/gh-106.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Тест с описанием на русском языке");
    }

    @AllureFeatures.Fixtures
    @AllureFeatures.Parallel
    @Issue("219")
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    @DisplayName("Should not mix up fixtures during parallel run")
    public void shouldAddCorrectBeforeMethodFixturesInCaseOfParallelRun(
                                                                        final XmlSuite.ParallelMode mode, final int threadCount) {
        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/gh-219.xml"
        );

        final List<TestResultContainer> testContainers = results.getTestResultContainers();
        final List<TestResult> testResults = results.getTestResults();

        testResults.forEach(testResult -> {
            final List<FixtureResult> firstBefore = testContainers.stream()
                    .filter(container -> container.getChildren().contains(testResult.getUuid()))
                    .map(TestResultContainer::getBefores)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            final List<FixtureResult> firstAfter = testContainers.stream()
                    .filter(container -> container.getChildren().contains(testResult.getUuid()))
                    .map(TestResultContainer::getAfters)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            assertThat(firstBefore)
                    .extracting(FixtureResult::getName)
                    .contains(
                            "beforeTest",
                            "beforeClass",
                            "beforeMethod1",
                            "beforeMethod2"
                    );

            assertThat(firstAfter)
                    .extracting(FixtureResult::getName)
                    .contains(
                            "afterTest",
                            "afterClass",
                            "afterMethod1",
                            "afterMethod2"
                    );

        });
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Fixtures
    @Issue("135")
    @Test
    public void shouldProcessConfigurationFailure() {
        final AllureResults results = runTestNgSuites("suites/gh-135.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("someTest", Status.SKIPPED),
                        tuple("failed configuration", Status.BROKEN)
                );

        assertThat(results.getTestResults())
                .filteredOn("name", "failed configuration")
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactly("fail");

        assertThat(results.getTestResults())
                .filteredOn("name", "failed configuration")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("AS_ID", "-1")
                );
    }

    @AllureFeatures.IgnoredTests
    @Issue("49")
    @Test
    public void shouldDisplayDisabledTests() {
        final AllureResults results = runTestNgSuites("suites/gh-49.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("disabled", null),
                        tuple("enabled", Status.PASSED)
                );

    }

    @AllureFeatures.IgnoredTests
    @Issue("369")
    @Test
    public void shouldNotDisplayDisabledTests() {
        AllureTestNgConfig allureTestNgConfig = AllureTestNgConfig.loadConfigProperties();
        allureTestNgConfig.setHideDisabledTests(true);
        final AllureResults results = runTestNgSuites(allureTestNgConfig, "suites/gh-369.xml");
        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsOnly(tuple("enabled", Status.PASSED));
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Parameters
    @Issue("129")
    @Test
    public void shouldNotFailForNullParameters() {
        final AllureResults results = runTestNgSuites("suites/gh-129.xml");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("param", "null")
                );
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Parameters
    @Issue("128")
    @Test
    public void shouldProcessArrayParameters() {
        final AllureResults results = runTestNgSuites("suites/gh-128.xml");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("first", "a"),
                        tuple("second", "false"),
                        tuple("third", "[1, 2, 3]")
                );
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Fixtures
    @Issue("304")
    @ParameterizedTest
    @MethodSource("parallelConfiguration")
    public void shouldProcessFailedSetUps(final XmlSuite.ParallelMode mode, final int threadCount) {
        final AllureResults results = runTestNgSuites(parallel(mode, threadCount), "suites/gh-304.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .contains(tuple("skippedTest", Status.SKIPPED));

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .contains(tuple("afterAlways", Status.PASSED));

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .filteredOn("name", "afterAlways")
                .flatExtracting(FixtureResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "first", "second"
                );
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Parameters
    @Test
    public void shouldOverrideParameters() {
        final AllureResults results = runTestNgSuites("suites/parameters-override.xml");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("first", "first-test"),
                        tuple("second", "second-test")
                );
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Parameters
    @Issue("141")
    @Test
    public void shouldSupportFactoryOnConstructor() {
        final AllureResults results = runTestNgSuites("suites/gh-141.xml");
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("number", "1"),
                        tuple("number", "2"),
                        tuple("Name", "first"),
                        tuple("Name", "second")
                );
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Parameters
    @Test
    public void shouldSupportInheritedTestInstanceParameterMetadata() {
        final AllureResults results = runTestNgSuites("suites/test-instance-parameters.xml");

        assertThat(results.getTestResults()).hasSize(3);
        assertThat(results.getTestResults())
                .allSatisfy(
                        testResult -> assertThat(testResult.getParameters())
                                .hasSize(3)
                                .extracting(
                                        Parameter::getName,
                                        Parameter::getValue,
                                        Parameter::getExcluded,
                                        Parameter::getMode
                                )
                                .contains(
                                        tuple("overridden", "child-value", false, Parameter.Mode.DEFAULT)
                                )
                );
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .filteredOn(parameter -> "iteration".equals(parameter.getName()))
                .extracting(Parameter::getValue, Parameter::getExcluded, Parameter::getMode)
                .containsExactlyInAnyOrder(
                        tuple("first", true, Parameter.Mode.DEFAULT),
                        tuple("second", true, Parameter.Mode.DEFAULT),
                        tuple("third", true, Parameter.Mode.DEFAULT)
                );
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .filteredOn(parameter -> "hidden".equals(parameter.getName()))
                .extracting(Parameter::getValue, Parameter::getExcluded, Parameter::getMode)
                .containsExactlyInAnyOrder(
                        tuple("hidden-value", false, Parameter.Mode.HIDDEN),
                        tuple("hidden-value", false, Parameter.Mode.HIDDEN),
                        tuple("different-hidden-value", false, Parameter.Mode.HIDDEN)
                );
    }

    @AllureFeatures.Parameters
    @AllureFeatures.History
    @Test
    public void shouldExcludeTestInstanceParameterFromHistoryId() {
        final AllureResults results = runTestNgSuites("suites/test-instance-parameters.xml");

        assertThat(results.getTestResults()).hasSize(3);
        final Map<String, String> historyIds = results.getTestResults().stream()
                .collect(
                        Collectors.toMap(
                                testResult -> testResult.getParameters().stream()
                                        .filter(parameter -> "iteration".equals(parameter.getName()))
                                        .map(Parameter::getValue)
                                        .findFirst()
                                        .orElseThrow(),
                                TestResult::getHistoryId
                        )
                );
        assertThat(historyIds.values()).doesNotContainNull();
        assertThat(historyIds.get("first")).isEqualTo(historyIds.get("second"));
        assertThat(historyIds.get("first")).isNotEqualTo(historyIds.get("third"));
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Parameters
    @Issue("893")
    @Test
    public void shouldDisplayCustomNamesOfParameters() {
        final AllureResults results = runTestNgSuites("suites/gh-893.xml");
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded, Parameter::getMode)
                .containsExactlyInAnyOrder(
                        tuple("First", "1", false, Parameter.Mode.DEFAULT),
                        tuple("Second", "1", false, Parameter.Mode.DEFAULT),
                        tuple("Third", "2", false, Parameter.Mode.DEFAULT),
                        tuple("Fourth", "5", true, Parameter.Mode.HIDDEN)
                );
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Parameters
    @Issue("893")
    @Test
    public void shouldDisplayCustomNamesWhenSkippingInjectedParameters() {
        final AllureResults results = runTestNgSuites("suites/gh-893-injected-parameters.xml");
        assertThat(results.getTestResults()).hasSize(1);
        final TestResult testResult = results.getTestResults().get(0);
        assertThat(testResult.getParameters())
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded, Parameter::getMode)
                .containsExactlyInAnyOrder(
                        tuple("First", "first-value", false, Parameter.Mode.DEFAULT),
                        tuple("Second", "second-value", true, Parameter.Mode.HIDDEN),
                        tuple("third", "third-value", false, Parameter.Mode.MASKED),
                        tuple("fourth", "fourth-value", null, null)
                );
    }

    @AllureFeatures.Parameters
    @Issue("893")
    @Test
    public void shouldSkipAllNativeInjectedParameterTypesWhenResolvingNames() throws Exception {
        final List<Parameter> parameters = resolveParametersFromMethodWithAllNativeInjectedTypes();

        assertResolvedCustomParameters(parameters);
    }

    @Step("Resolve parameters from method with all native TestNG injected types")
    @SuppressWarnings({"unchecked", "PMD.AvoidAccessibilityAlteration"})
    private List<Parameter> resolveParametersFromMethodWithAllNativeInjectedTypes() throws Exception {
        final XmlTest xmlTest = new XmlTest(new XmlSuite());
        xmlTest.addParameter("first", "first-value");
        xmlTest.addParameter("second", "second-value");
        xmlTest.addParameter("third", "third-value");
        xmlTest.addParameter("fourth", "fourth-value");

        final Method source = getClass().getDeclaredMethod(
                "methodWithAllNativeInjectedParameterTypes",
                Method.class,
                String.class,
                ITestContext.class,
                String.class,
                ITestResult.class,
                XmlTest.class,
                Object[].class,
                String.class,
                String.class
        );
        final ITestContext context = mock(ITestContext.class);
        when(context.getCurrentXmlTest()).thenReturn(xmlTest);

        final ITestNGMethod method = mock(ITestNGMethod.class);
        when(method.getInstance()).thenReturn(null);
        when(method.getConstructorOrMethod()).thenReturn(new ConstructorOrMethod(source));

        final AllureTestNg adapter = new AllureTestNg(
                new AllureLifecycle(new AllureResultsWriterStub()),
                new AllureTestNgTestFilter(),
                AllureTestNgConfig.loadConfigProperties()
        );
        final Method getParameters = AllureTestNg.class.getDeclaredMethod(
                "getParameters",
                ITestContext.class,
                ITestNGMethod.class,
                Object[].class
        );
        getParameters.setAccessible(true);

        final List<Parameter> parameters = (List<Parameter>) getParameters.invoke(
                adapter,
                context,
                method,
                (Object) new Object[]{
                        source,
                        "first-value",
                        context,
                        "second-value",
                        mock(ITestResult.class),
                        xmlTest,
                        new Object[]{"ignored"},
                        "third-value",
                        "fourth-value",
                }
        );
        return parameters;
    }

    @Step("Assert custom names and metadata after skipping injected parameters")
    private void assertResolvedCustomParameters(final List<Parameter> parameters) {
        assertThat(parameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded, Parameter::getMode)
                .containsExactlyInAnyOrder(
                        tuple("First", "first-value", false, Parameter.Mode.DEFAULT),
                        tuple("Second", "second-value", true, Parameter.Mode.HIDDEN),
                        tuple("third", "third-value", false, Parameter.Mode.MASKED),
                        tuple("fourth", "fourth-value", null, null)
                );
    }

    @Parameters({"first", "second", "third", "fourth"})
    void methodWithAllNativeInjectedParameterTypes(final Method method,
                                                   @Param("First") final String first,
                                                   final ITestContext context,
                                                   @Param(
                                                           name = "Second",
                                                           excluded = true,
                                                           mode = Parameter.Mode.HIDDEN
                                                   ) final String second,
                                                   final ITestResult testResult,
                                                   final XmlTest xmlTest,
                                                   final Object[] parameters,
                                                   @Param(
                                                           name = " ",
                                                           mode = Parameter.Mode.MASKED
                                                   ) final String third,
                                                   final String fourth) {
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> failedFixtures() {
        return Stream.of(
                arguments("suites/failed-before-test-fixture.xml", "beforeTest"),
                arguments("suites/failed-before-class-fixture.xml", "beforeClass"),
                arguments("suites/failed-before-suite-fixture.xml", "beforeSuite"),
                arguments("suites/failed-before-groups-fixture.xml", "beforeGroups")
        );
    }

    @ParameterizedTest
    @MethodSource("failedFixtures")
    @AllureFeatures.Fixtures
    public void shouldAddBeforeFixtureToFakeTestResult(final String suite, final String fixture) {
        final AllureResults results = runTestNgSuites(suite);
        final Optional<TestResult> result = results.getTestResults().stream()
                .filter(r -> r.getName().contains(fixture))
                .findAny();
        assertThat(result).as("Before failed fake test result").isNotEmpty();
        final Optional<TestResultContainer> befores = results.getTestResultContainers().stream()
                .filter(c -> Objects.nonNull(c.getBefores()) && !c.getBefores().isEmpty())
                .findAny();
        assertThat(result).as("Before failed configuration container").isNotEmpty();
        assertThat(befores.get().getChildren())
                .contains(result.get().getUuid());
    }

    @Test
    @AllureFeatures.Ordering
    public void shouldOrderTests() {
        final AllureResults results = runTestPlan(null, PriorityTests.class);
        final List<String> ordered = results.getTestResults().stream()
                .sorted(Comparator.comparing(this::getOrderParameter))
                .map(TestResult::getName)
                .collect(Collectors.toList());
        System.out.println(Arrays.toString(ordered.toArray()));
        assertThat(ordered)
                .containsExactly("zTest", "yTest", "xTest", "wTest", "vTest", "vTest");
    }

    private AllureResults runTestNgSuites(final String... suites) {
        final Consumer<TestNG> emptyConfigurer = testNg -> {
        };
        return runTestNgSuites(emptyConfigurer, suites);
    }

    private AllureResults runTestNgSuites(AllureTestNgConfig config, final String... suites) {
        final Consumer<TestNG> emptyConfigurer = testNg -> {
        };
        return runTestNgSuites(emptyConfigurer, config, suites);
    }

    private AllureResults runTestNgSuites(final Consumer<TestNG> configurer,
                                          final String... suites) {
        return runTestNgSuites(configurer, AllureTestNgConfig.loadConfigProperties(), suites);
    }

    @Step("Run testng suites {suites}")
    private AllureResults runTestNgSuites(final Consumer<TestNG> configurer,
                                          final AllureTestNgConfig config,
                                          final String... suites) {
        final ClassLoader classLoader = getClass().getClassLoader();
        List<String> suiteFiles = Arrays.stream(suites)
                .map(classLoader::getResource)
                .filter(Objects::nonNull)
                .map(URL::getFile)
                .collect(Collectors.toList());

        assertThat(suites)
                .as("Cannot find all suite xml files")
                .hasSameSizeAs(suiteFiles);

        return RunUtils.runTests(lifecycle -> {
            final AllureTestNg adapter = new AllureTestNg(
                    lifecycle,
                    new AllureTestNgTestFilter(),
                    config
            );
            final TestNG testNg = new TestNG(false);
            testNg.addListener(adapter);
            testNg.setTestSuites(suiteFiles);

            configurer.accept(testNg);
            testNg.run();
        });
    }

    protected Consumer<TestNG> parallel(final XmlSuite.ParallelMode mode,
                                        final int threadCount) {
        return testNG -> {
            testNG.setParallel(mode);
            testNG.setThreadCount(threadCount);
        };
    }

    @Step("Find results by name")
    private TestResult findTestResultByName(final AllureResults results, final String name) {
        return results.getTestResults().stream()
                .filter(testResult -> name.equalsIgnoreCase(testResult.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("can not find result by name " + name));
    }

    @Step("Find containers holding a fixture with the given name")
    private static List<TestResultContainer> findContainersByFixtureName(final List<TestResultContainer> containers,
                                                                         final String fixtureName) {
        return containers.stream()
                .filter(
                        container -> Stream
                                .concat(container.getBefores().stream(), container.getAfters().stream())
                                .anyMatch(fixture -> fixtureName.equals(fixture.getName()))
                )
                .collect(Collectors.toList());
    }

    @Step("Find fixture containers linked to the given test")
    private static List<TestResultContainer> findFixtureContainersWithChild(final AllureResults results,
                                                                            final String childUuid) {
        return results.getTestResultContainers().stream()
                .filter(container -> !container.getBefores().isEmpty() || !container.getAfters().isEmpty())
                .filter(container -> container.getChildren().contains(childUuid))
                .collect(Collectors.toList());
    }

    @Step("Has links")
    private Predicate<TestResult> hasLinks() {
        return testResult -> !testResult.getLinks().isEmpty();
    }

    @Step("Find flaky")
    private Predicate<TestResult> flakyPredicate() {
        return testResult -> Objects.nonNull(testResult.getStatusDetails())
                && testResult.getStatusDetails().isFlaky();
    }

    @Step("Find muted")
    private Predicate<TestResult> mutedPredicate() {
        return testResult -> Objects.nonNull(testResult.getStatusDetails())
                && testResult.getStatusDetails().isMuted();
    }

    @Step("Check containers children")
    private static void assertContainersChildrenByFixture(String fixtureName, List<TestResultContainer> containers,
                                                          List<String> uids) {
        assertThat(findContainersByFixtureName(containers, fixtureName))
                .isNotEmpty()
                .flatExtracting(TestResultContainer::getChildren)
                .as("Unexpected children for containers holding fixture " + fixtureName)
                .containsOnlyElementsOf(uids);
    }

    @Step("Check grouping containers children")
    private static void assertGroupingContainersWithChildren(List<TestResultContainer> containers, List<String> uids,
                                                             int expectedCount) {
        assertThat(containers)
                .filteredOn(
                        container -> container.getBefores().isEmpty() && container.getAfters().isEmpty()
                                && new HashSet<>(container.getChildren()).equals(new HashSet<>(uids))
                )
                .as("Unexpected quantity of fixture-less grouping containers with children " + uids)
                .hasSize(expectedCount);
    }

    @Step("Check containers per method")
    private static void assertContainersPerMethod(String name, List<TestResultContainer> containersList,
                                                  List<String> uids) {
        final Condition<List<? extends TestResultContainer>> singlyMapped = new Condition<>(
                containers -> containers.stream().allMatch(c -> c.getChildren().size() == 1),
                format("All containers for per-method fixture %s should be linked to only one testng result", name)
        );

        assertThat(findContainersByFixtureName(containersList, name))
                .is(singlyMapped)
                .flatExtracting(TestResultContainer::getChildren)
                .as("Unexpected children for per-method fixtures " + name)
                .containsOnlyElementsOf(uids);
    }

    @SuppressWarnings("unchecked")
    @Step("Check after fixtures")
    private static void assertAfterFixtures(List<TestResultContainer> containers, Object... afters) {
        assertThat(containers)
                .filteredOn(
                        container -> container.getAfters().stream()
                                .map(FixtureResult::getName)
                                .collect(Collectors.toList())
                                .equals(Arrays.asList(afters))
                )
                .as("Expected a container with after fixtures " + Arrays.toString(afters))
                .hasSize(1)
                .flatExtracting(TestResultContainer::getAfters)
                .is(ALL_FINISHED)
                .is(WITH_STEPS);
    }

    @SuppressWarnings("unchecked")
    @Step("Check before fixtures")
    private static void assertBeforeFixtures(List<TestResultContainer> containers, Object... befores) {
        assertThat(containers)
                .filteredOn(
                        container -> container.getBefores().stream()
                                .map(FixtureResult::getName)
                                .collect(Collectors.toList())
                                .equals(Arrays.asList(befores))
                )
                .as("Expected a container with before fixtures " + Arrays.toString(befores))
                .hasSize(1)
                .flatExtracting(TestResultContainer::getBefores)
                .is(ALL_FINISHED)
                .is(WITH_STEPS);
    }

    @Step("Check that before fixtures javadoc description refer to correct fixture methods")
    private static void checkBeforeJavadocDescriptions(AllureResults results, String testClassName,
                                                       String fixtureName, String expectedDescription) {
        final String testUuid = results.getTestResults().stream()
                .filter(result -> result.getFullName().startsWith(testClassName + "."))
                .map(TestResult::getUuid)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no test result for class " + testClassName));
        assertThat(results.getTestResultContainers())
                .filteredOn(container -> container.getChildren().contains(testUuid))
                .flatExtracting(TestResultContainer::getBefores)
                .filteredOn(fixture -> fixture.getName().equals(fixtureName))
                .extracting(FixtureResult::getDescription, FixtureResult::getDescriptionHtml)
                .as(
                        "Javadoc description of before fixture " + fixtureName + " of " + testClassName
                                + " has been processed incorrectly"
                )
                .containsOnly(tuple(expectedDescription, null));
    }

    @Step("Check that javadoc description of tests refer to correct test methods")
    private static void checkTestJavadocDescriptions(List<TestResult> results, String methodReference, String expectedDescription) {
        assertThat(results).as("Test results has not been written")
                .isNotEmpty()
                .filteredOn(result -> result.getFullName().equals(methodReference))
                .extracting(TestResult::getDescription, TestResult::getDescriptionHtml)
                .as("Javadoc description of befores have been processed incorrectly")
                .containsOnly(tuple(expectedDescription, null));
    }

    private final TestPlanV1_0.TestCase onlyId2 = new TestPlanV1_0.TestCase().setId("2");
    private final TestPlanV1_0.TestCase onlyId4 = new TestPlanV1_0.TestCase().setId("4");

    private final TestPlanV1_0.TestCase test1 = new TestPlanV1_0.TestCase().setId("1")
            .setSelector("io.qameta.allure.testng.samples.TestsWithIdForFilter.test1");
    private final TestPlanV1_0.TestCase test2 = new TestPlanV1_0.TestCase()
            .setId("2")
            .setSelector("io.qameta.allure.testng.samples.TestsWithIdForFilter.test2");
    private final TestPlanV1_0.TestCase test3 = new TestPlanV1_0.TestCase()
            .setId("3")
            .setSelector("io.qameta.allure.testng.samples.TestsWithIdForFilter.test3");
    private final TestPlanV1_0.TestCase otherId = new TestPlanV1_0.TestCase().setId("4")
            .setSelector("io.qameta.allure.testng.samples.TestsWithIdForFilter.test1");
    private final TestPlanV1_0.TestCase skipped = new TestPlanV1_0.TestCase().setId("5")
            .setSelector("io.qameta.allure.testng.samples.TestsWithIdForFilter.skipped");
    private final TestPlanV1_0.TestCase correctIdIncorrectSelector = new TestPlanV1_0.TestCase()
            .setId("3")
            .setSelector("io.qameta.allure.testng.samples.TestsWithIdForFilter.test3");
    private final TestPlanV1_0.TestCase correctIdIncorrectSelectorFailed = new TestPlanV1_0.TestCase()
            .setId("6")
            .setSelector("allure.junit4.samples.TestsWithIdForFilter.test3");

    @Test
    @AllureFeatures.Filtration
    public void simpleFiltration() {
        TestPlanV1_0 plan = new TestPlanV1_0().setTests(Arrays.asList(test1, test2, test3));
        List<TestResult> testResults = runTestPlan(plan, TestsWithIdForFilter.class).getTestResults();

        assertThat(testResults)
                .hasSize(3)
                .extracting(TestResult::getName, TestResult::getStatus)
                .contains(
                        tuple("test1", Status.PASSED),
                        tuple("test2", Status.PASSED),
                        tuple("test3", Status.PASSED)
                );
    }

    @Test
    @AllureFeatures.Filtration
    public void onlyId() {
        TestPlanV1_0 plan = new TestPlanV1_0().setTests(Arrays.asList(onlyId2, onlyId4));
        List<TestResult> testResults = runTestPlan(plan, TestsWithIdForFilter.class).getTestResults();

        assertThat(testResults)
                .hasSize(2)
                .extracting(TestResult::getName, TestResult::getStatus)
                .contains(
                        tuple("test4", Status.PASSED),
                        tuple("test2", Status.PASSED)
                );
    }

    @Test
    @AllureFeatures.Filtration
    public void idAssignToOtherTest() {
        TestPlanV1_0 plan = new TestPlanV1_0().setTests(singletonList(otherId));
        List<TestResult> testResults = runTestPlan(plan, TestsWithIdForFilter.class).getTestResults();

        assertThat(testResults)
                .hasSize(2)
                .extracting(TestResult::getName, TestResult::getStatus)
                .contains(
                        tuple("test1", Status.PASSED),
                        tuple("test4", Status.PASSED)
                );
    }

    @Test
    @AllureFeatures.Filtration
    public void skippedTest() {
        TestPlanV1_0 plan = new TestPlanV1_0().setTests(singletonList(skipped));
        List<TestResult> testResults = runTestPlan(plan, TestsWithIdForFilter.class).getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getName, TestResult::getStatus)
                .contains(
                        tuple("skipped", null)
                );
    }

    @Test
    @AllureFeatures.Filtration
    public void correctIdIncorrectSelector() {
        TestPlanV1_0 plan = new TestPlanV1_0().setTests(
                Arrays.asList(test1, test2, correctIdIncorrectSelector, correctIdIncorrectSelectorFailed)
        );
        List<TestResult> testResults = runTestPlan(plan, TestsWithIdForFilter.class).getTestResults();
        assertThat(testResults)
                .hasSize(4)
                .extracting(TestResult::getName, TestResult::getStatus)
                .contains(
                        tuple("test1", Status.PASSED),
                        tuple("test2", Status.PASSED),
                        tuple("test3", Status.PASSED),
                        tuple("test6", Status.FAILED)
                );
    }

    public AllureResults runTestPlan(final TestPlan plan, final Class<?>... testClasses) {
        return RunUtils.runTests(lifecycle -> {
            final AllureTestNg adapter = new AllureTestNg(lifecycle, new AllureTestNgTestFilter(plan));
            final TestNG testNG = new TestNG(false);
            testNG.addListener(adapter);
            testNG.setTestClasses(testClasses);
            testNG.setOutputDirectory("build/test-output");
            testNG.run();
        });
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Should process data provider in setup")
    public void shouldProcessDataProviderInSetup() {
        final AllureResults results = runTestNgSuites("suites/data-provider-with-attachment.xml");

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .contains(Tuple.tuple("dataProvider", Status.PASSED));

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .filteredOn("name", "dataProvider")
                .flatExtracting(FixtureResult::getSteps)
                .extracting(StepResult::getName)
                .contains("attachment");

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .filteredOn("name", "dataProvider")
                .flatExtracting(FixtureResult::getSteps)
                .flatExtracting(StepResult::getAttachments)
                .hasSize(1)
                .extracting(Attachment::getName)
                .contains("attachment");
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Should process failed data provider in setup")
    public void shouldProcessFailedDataProviderInSetup() {
        final AllureResults results = runTestNgSuites("suites/failed-data-provider.xml");

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .contains(Tuple.tuple("dataProvider", Status.BROKEN));
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Should process flaky data provider in setup")
    public void shouldProcessFlakyDataProvider() {
        final AllureResults results = runTestNgSuites("suites/flaky-data-provider.xml");

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsSubsequence(
                        Tuple.tuple("provide", Status.BROKEN),
                        Tuple.tuple("provide", Status.PASSED)
                );
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Should properly link data provider container to test result")
    public void shouldProperlyLinkDataProviderContainerToTestResult() {
        final AllureResults results = runTestNgSuites("suites/data-provider-with-attachment.xml");

        final TestResult tr = findTestResultByName(results, "test");

        assertThat(findFixtureContainersWithChild(results, tr.getUuid()))
                .as("DP container not found")
                .hasSize(1)
                .flatExtracting(TestResultContainer::getChildren)
                .contains(tr.getUuid());
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Should link multiple tests to data provider container")
    public void shouldLinkMultipleTestsToDataProviderContainer() {
        final AllureResults results = runTestNgSuites("suites/data-provider-multiple-tests.xml");

        final List<TestResult> test1Results = results.getTestResults().stream()
                .filter(tr -> tr.getName().equals("test1"))
                .collect(Collectors.toList());
        final List<TestResult> test2Results = results.getTestResults().stream()
                .filter(tr -> tr.getName().equals("test2"))
                .collect(Collectors.toList());

        assertThat(test1Results).hasSize(2);
        assertThat(test2Results).hasSize(2);

        final List<String> test1Uuids = test1Results.stream().map(TestResult::getUuid).collect(Collectors.toList());
        final List<String> test2Uuids = test2Results.stream().map(TestResult::getUuid).collect(Collectors.toList());

        assertThat(findFixtureContainersWithChild(results, test1Uuids.get(0)))
                .hasSize(1)
                .flatExtracting(TestResultContainer::getChildren)
                .containsAll(test1Uuids);
        assertThat(findFixtureContainersWithChild(results, test2Uuids.get(0)))
                .hasSize(1)
                .flatExtracting(TestResultContainer::getChildren)
                .containsAll(test2Uuids);
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Should link inherited data provider")
    public void shouldLinkInheritedDataProvider() {
        final AllureResults results = runTestNgSuites("suites/data-provider-inheritance.xml");

        final TestResult testBase = findTestResultByName(results, "testBase");
        final TestResult testChild = findTestResultByName(results, "testChild");

        assertThat(findFixtureContainersWithChild(results, testBase.getUuid()))
                .as("DP container for testBase").hasSize(1);
        assertThat(findFixtureContainersWithChild(results, testChild.getUuid()))
                .as("DP container for testChild").hasSize(1);
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Should link correct data provider in multiple classes")
    public void shouldLinkCorrectDataProviderInMultipleClasses() {
        final AllureResults results = runTestNgSuites("suites/data-provider-multiple-classes.xml");

        final TestResult test1 = findTestResultByName(results, "test1");
        final TestResult test2 = findTestResultByName(results, "test2");

        assertThat(findFixtureContainersWithChild(results, test1.getUuid()))
                .hasSize(1)
                .flatExtracting(TestResultContainer::getChildren)
                .contains(test1.getUuid())
                .doesNotContain(test2.getUuid());
        assertThat(findFixtureContainersWithChild(results, test2.getUuid()))
                .hasSize(1)
                .flatExtracting(TestResultContainer::getChildren)
                .contains(test2.getUuid())
                .doesNotContain(test1.getUuid());
    }

    @AllureFeatures.Fixtures
    @Test
    @DisplayName("Should process parallel data provider")
    public void shouldProcessParallelDataProvider() {
        final AllureResults results = runTestNgSuites("suites/data-provider-parallel.xml");

        assertThat(results.getTestResults()).hasSize(4);
        final String anyTestUuid = results.getTestResults().get(0).getUuid();
        assertThat(findFixtureContainersWithChild(results, anyTestUuid))
                .hasSize(1)
                .flatExtracting(TestResultContainer::getChildren)
                .hasSize(4);
    }

    private Integer getOrderParameter(final TestResult result) {
        return result.getParameters().stream()
                .filter(p -> p.getName().equals("order"))
                .map(Parameter::getValue)
                .map(Integer::parseInt)
                .findAny()
                .orElse(0);
    }

}
