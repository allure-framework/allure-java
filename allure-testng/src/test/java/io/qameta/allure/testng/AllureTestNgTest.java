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
package io.qameta.allure.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.ExecutableItem;
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
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.assertj.core.api.Condition;
import org.assertj.core.groups.Tuple;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.ALLURE_SEPARATE_LINES_SYSPROP;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@SuppressWarnings("deprecation")
public class AllureTestNgTest {

    private static final Condition<List<? extends ExecutableItem>> ALL_FINISHED = new Condition<>(items ->
            items.stream().allMatch(item -> item.getStage() == Stage.FINISHED),
            "All items should have be in a finished stage");

    private static final Condition<List<? extends ExecutableItem>> WITH_STEPS = new Condition<>(items ->
            items.stream().allMatch(item -> item.getSteps().size() == 1),
            "All items should have a step attached");

    @DataProvider(name = "parallelConfiguration")
    public static Object[][] parallelConfiguration() {
        return new Object[][]{
                new Object[]{XmlSuite.ParallelMode.NONE, 10},
                new Object[]{XmlSuite.ParallelMode.NONE, 5},
                new Object[]{XmlSuite.ParallelMode.NONE, 2},
                new Object[]{XmlSuite.ParallelMode.NONE, 1},
                new Object[]{XmlSuite.ParallelMode.METHODS, 10},
                new Object[]{XmlSuite.ParallelMode.METHODS, 5},
                new Object[]{XmlSuite.ParallelMode.METHODS, 2},
                new Object[]{XmlSuite.ParallelMode.METHODS, 1},
                new Object[]{XmlSuite.ParallelMode.CLASSES, 10},
                new Object[]{XmlSuite.ParallelMode.CLASSES, 5},
                new Object[]{XmlSuite.ParallelMode.CLASSES, 2},
                new Object[]{XmlSuite.ParallelMode.CLASSES, 1},
                new Object[]{XmlSuite.ParallelMode.INSTANCES, 10},
                new Object[]{XmlSuite.ParallelMode.INSTANCES, 5},
                new Object[]{XmlSuite.ParallelMode.INSTANCES, 2},
                new Object[]{XmlSuite.ParallelMode.INSTANCES, 1},
                new Object[]{XmlSuite.ParallelMode.TESTS, 10},
                new Object[]{XmlSuite.ParallelMode.TESTS, 5},
                new Object[]{XmlSuite.ParallelMode.TESTS, 2},
                new Object[]{XmlSuite.ParallelMode.TESTS, 1},
        };
    }

    @AllureFeatures.Parallel
    @Test(description = "Parallel data provider tests")
    public void parallelDataProvider() {
        final AllureResults results = runTestNgSuites("suites/parallel-data-provider.xml");
        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> containers = results.getTestResultContainers();
        assertThat(testResult).as("Not all testng case results have been written").hasSize(2000);
        assertThat(containers).as("Not all testng containers have been written").hasSize(3);
    }

    @AllureFeatures.Base
    @Test(description = "Singe testng")
    public void singleTest() {
        final String testName = "testWithOneStep";
        final AllureResults results = runTestNgSuites("suites/single-test.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected passed testng property")
                .hasFieldOrPropertyWithValue("status", Status.PASSED)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(1)
                .extracting(StepResult::getStatus)
                .contains(Status.PASSED);
    }

    @AllureFeatures.Base
    @Test(description = "Test with timeout", dataProvider = "parallelConfiguration")
    public void testWithTimeout(final XmlSuite.ParallelMode mode, final int threadCount) {

        final String testNameWithTimeout = "testWithTimeout";
        final String testNameWithoutTimeout = "testWithoutTimeout";
        final AllureResults results = runTestNgSuites(parallel(mode, threadCount), "suites/tests-with-timeout.xml");
        List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .as("Test case results have not been written")
                .hasSize(2)
                .as("Unexpectedly passed status or stage of tests")
                .allMatch(testResult -> testResult.getStatus().equals(Status.PASSED) &&
                        testResult.getStage().equals(Stage.FINISHED))
                .extracting(TestResult::getName)
                .as("Unexpectedly passed name of tests")
                .containsOnlyElementsOf(asList(
                        testNameWithoutTimeout,
                        testNameWithTimeout)
                );
        assertThat(testResults)
                .flatExtracting(TestResult::getSteps)
                .as("No steps present for test with timeout")
                .hasSize(2)
                .extracting(StepResult::getName)
                .containsOnlyElementsOf(asList(
                        "Step of the test with timeout",
                        "Step of the test with no timeout")
                );
    }

    @AllureFeatures.Descriptions
    @Test(description = "Javadoc description with line separation")
    public void descriptionsWithLineSeparationTest() {
        String initialSeparateLines = System.getProperty(ALLURE_SEPARATE_LINES_SYSPROP);
        if (!Boolean.parseBoolean(initialSeparateLines)) {
            System.setProperty(ALLURE_SEPARATE_LINES_SYSPROP, "true");
        }
        try {
            final String testDescription = "Sample test description<br /> - next line<br /> - another line<br />";
            final AllureResults results = runTestNgSuites("suites/descriptions-test.xml");
            List<TestResult> testResult = results.getTestResults();

            assertThat(testResult).as("Test case result has not been written")
                    .hasSize(2)
                    .filteredOn(result -> result.getName().equals("testSeparated"))
                    .extracting(result -> result.getDescriptionHtml().trim())
                    .as("Javadoc description of test case has not been processed correctly")
                    .contains(testDescription);
        } finally {
            System.setProperty(ALLURE_SEPARATE_LINES_SYSPROP, String.valueOf(initialSeparateLines));
        }
    }

    @AllureFeatures.Descriptions
    @Test(description = "Javadoc description of tests")
    public void descriptionsTest() {
        final String testDescription = "Sample test description";
        final AllureResults results = runTestNgSuites("suites/descriptions-test.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written")
                .hasSize(2)
                .filteredOn(result -> result.getName().equals("test"))
                .extracting(result -> result.getDescriptionHtml().trim())
                .as("Javadoc description of test case has not been processed")
                .contains(testDescription);
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Descriptions
    @Test(description = "Javadoc description of befores", dataProvider = "parallelConfiguration")
    public void descriptionsBefores(final XmlSuite.ParallelMode mode, final int threadCount) {
        final String beforeClassDescription = "Before class description";
        final String beforeMethodDescription = "Before method description";
        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/descriptions-test.xml"
        );
        final List<TestResultContainer> testContainers = results.getTestResultContainers();

        assertThat(testContainers).as("Test containers has not been written")
                .isNotEmpty()
                .filteredOn(container -> !container.getBefores().isEmpty())
                .extracting(container -> container.getBefores().get(0).getDescriptionHtml().trim())
                .as("Javadoc description of befores have not been processed")
                .containsOnly(beforeClassDescription, beforeMethodDescription);
    }

    @AllureFeatures.Descriptions
    @Test(description = "Javadoc description of befores with the same names")
    public void javadocDescriptionsOfBeforesWithTheSameNames() {
        final AllureResults results = runTestNgSuites("suites/descriptions-test-two-classes.xml");
        List<TestResultContainer> testContainers = results.getTestResultContainers();

        checkBeforeJavadocDescriptions(testContainers, "io.qameta.allure.testng.samples.DescriptionsTest.setUpMethod", "Before method description");
        checkBeforeJavadocDescriptions(testContainers, "io.qameta.allure.testng.samples.DescriptionsTest", "Before class description");

        checkBeforeJavadocDescriptions(testContainers, "io.qameta.allure.testng.samples.DescriptionsAnotherTest.setUpMethod", "Before method description from DescriptionsAnotherTest");
        checkBeforeJavadocDescriptions(testContainers, "io.qameta.allure.testng.samples.DescriptionsAnotherTest", "Before class description from DescriptionsAnotherTest");
    }

    @AllureFeatures.Descriptions
    @Test(description = "Javadoc description of tests with the same names")
    public void javadocDescriptionsOfTestsWithTheSameNames() {
        final AllureResults results = runTestNgSuites("suites/descriptions-test-two-classes.xml");
        List<TestResult> testResults = results.getTestResults();

        checkTestJavadocDescriptions(testResults, "io.qameta.allure.testng.samples.DescriptionsTest.test", "Sample test description");

        checkTestJavadocDescriptions(testResults, "io.qameta.allure.testng.samples.DescriptionsAnotherTest.test", "Sample test description from DescriptionsAnotherTest");
    }

    @AllureFeatures.FailedTests
    @Test(description = "Test failing by assertion")
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
    @Test(description = "Broken testng")
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
    @Test(description = "Broken testng - Exception without message")
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
    @Test(description = "Suite fixtures", dataProvider = "parallelConfiguration")
    public void perSuiteFixtures(final XmlSuite.ParallelMode mode, final int threadCount) {
        String suiteName = "Test suite 12";
        String testTagName = "Test tag 12";
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

        assertContainersChildren(testTagName, testContainers, testUuid);
        assertContainersChildren(suiteName, testContainers, getUidsByName(testContainers, testTagName));
        assertBeforeFixtures(suiteName, testContainers, before1, before2);
        assertAfterFixtures(suiteName, testContainers, after1, after2);
    }

    @AllureFeatures.Fixtures
    @Test(description = "Class fixtures", dataProvider = "parallelConfiguration")
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
    @Test(description = "Method fixtures", dataProvider = "parallelConfiguration")
    public void perMethodFixtures(final XmlSuite.ParallelMode mode, final int threadCount) {
        String suiteName = "Test suite 11";
        String testTagName = "Test tag 11";
        String before1 = "io.qameta.allure.testng.samples.PerMethodFixtures.beforeMethod1";
        String before2 = "io.qameta.allure.testng.samples.PerMethodFixtures.beforeMethod2";
        String after1 = "io.qameta.allure.testng.samples.PerMethodFixtures.afterMethod1";
        String after2 = "io.qameta.allure.testng.samples.PerMethodFixtures.afterMethod2";

        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/per-method-fixtures-combination.xml"
        );

        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestResultContainers();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written").hasSize(2);
        List<String> uuids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());

        assertContainersChildren(testTagName, testContainers, uuids);
        assertContainersChildren(suiteName, testContainers, getUidsByName(testContainers, testTagName));
        assertContainersPerMethod(before1, testContainers, uuids);
        assertContainersPerMethod(before2, testContainers, uuids);
        assertContainersPerMethod(after1, testContainers, uuids);
        assertContainersPerMethod(after2, testContainers, uuids);
    }

    @AllureFeatures.Fixtures
    @Test(description = "Test fixtures", dataProvider = "parallelConfiguration")
    public void perTestTagFixtures(final XmlSuite.ParallelMode mode, final int threadCount) {
        String suiteName = "Test suite 13";
        String testTagName = "Test tag 13";
        String before1 = "beforeTest1";
        String before2 = "beforeTest2";
        String after1 = "afterTest1";
        String after2 = "afterTest2";

        final AllureResults results = runTestNgSuites(
                parallel(mode, threadCount),
                "suites/per-test-tag-fixtures-combination.xml"
        );

        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestResultContainers();

        assertThat(testResult).as("Unexpected quantity of testng case results has been written").hasSize(1);
        List<String> testUuid = singletonList(testResult.get(0).getUuid());

        assertContainersChildren(testTagName, testContainers, testUuid);
        assertContainersChildren(suiteName, testContainers, getUidsByName(testContainers, testTagName));
        assertBeforeFixtures(testTagName, testContainers, before1, before2);
        assertAfterFixtures(testTagName, testContainers, after1, after2);
    }

    @AllureFeatures.SkippedTests
    @Test(description = "Skipped suite")
    public void skippedSuiteTest() {
        final Condition<StepResult> skipReason = new Condition<>(step ->
                step.getStatusDetails().getTrace().startsWith("java.lang.RuntimeException: Skip all"),
                "Suite should be skipped because of an exception in before suite");

        final AllureResults results = runTestNgSuites("suites/skipped-suite.xml");
        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestResultContainers();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .contains(
                        tuple("skippedTest", Status.SKIPPED),
                        tuple("testWithOneStep", Status.SKIPPED)
                );
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written").hasSize(4);

        assertThat(findTestContainerByName(results, "Test suite 8").getBefores())
                .as("Before suite container should have a before method with one step")
                .hasSize(1)
                .flatExtracting(FixtureResult::getSteps)
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .has(skipReason);
    }

    @AllureFeatures.Base
    @Test(description = "Multi suites")
    public void multipleSuites() {
        String beforeMethodName = "io.qameta.allure.testng.samples.ParameterizedTest.beforeMethod";
        String firstSuiteName = "Test suite 6";
        String firstTagName = "Test tag 6";
        String secondSuiteName = "Test suite 7";
        String secondTagName = "Test tag 7";

        final AllureResults results = runTestNgSuites("suites/parameterized-test.xml", "suites/single-test.xml");

        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestResultContainers();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(3);
        List<String> uids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written")
                .hasSize(8).extracting(TestResultContainer::getName)
                .contains(beforeMethodName, beforeMethodName, firstTagName, firstSuiteName, secondTagName,
                        secondSuiteName);

        final List<String> firstSuite = uids.subList(0, 2);
        assertContainersChildren(beforeMethodName, testContainers, firstSuite);
        assertContainersChildren(firstTagName, testContainers, firstSuite);
        assertContainersChildren(firstSuiteName, testContainers, getUidsByName(testContainers, firstTagName));
        final List<String> secondSuite = singletonList(uids.get(2));
        assertContainersChildren(secondTagName, testContainers, secondSuite);
        assertContainersChildren(secondSuiteName, testContainers, getUidsByName(testContainers, secondTagName));
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Parameters
    @Test(description = "Before Suite Parameter")
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
    @Test(description = "Parallel methods")
    public void parallelMethods() {
        String before1 = "io.qameta.allure.testng.samples.ParallelMethods.beforeMethod";
        String before2 = "io.qameta.allure.testng.samples.ParallelMethods.beforeMethod2";
        String after = "io.qameta.allure.testng.samples.ParallelMethods.afterMethod";
        String testTag = "Test tag 9";

        final AllureResults results = runTestNgSuites("suites/parallel-methods.xml");
        List<TestResult> testResults = results.getTestResults();
        List<String> uids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());
        List<TestResultContainer> testContainers = results.getTestResultContainers();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(2001);
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written")
                .hasSize(6006);

        assertContainersPerMethod(before1, testContainers, uids);
        assertContainersPerMethod(before2, testContainers, uids);
        assertContainersPerMethod(after, testContainers, uids);
        assertContainersChildren(testTag, testContainers, uids);
    }

    @AllureFeatures.Steps
    @Test(description = "Nested steps")
    public void nestedSteps() {
        String beforeMethod = "io.qameta.allure.testng.samples.NestedSteps.beforeMethod";
        String nestedStep = "nestedStep";
        String stepInBefore = "stepTwo";
        String stepInTest = "stepThree";
        final Condition<StepResult> substep = new Condition<>(step ->
                step.getSteps().get(0).getName().equals(nestedStep),
                "Given step should have a substep with name " + nestedStep);

        final AllureResults results = runTestNgSuites("suites/nested-steps.xml");
        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> containers = results.getTestResultContainers();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(1);

        assertThat(containers)
                .filteredOn("name", beforeMethod)
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
    @Test(description = "Flaky tests")
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
    @Test(description = "Muted tests")
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
    @Test(description = "Tests with links")
    public void linksTest() {
        final AllureResults results = runTestNgSuites("suites/links.xml");

        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(4)
                .filteredOn(hasLinks())
                .hasSize(4)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .contains("testClass", "a", "b", "c", "testClassIssue", "testClassTmsLink",
                        "testClass", "nested1", "nested2", "nested3", "testClassIssue", "issue1", "issue2", "issue3",
                        "testClassTmsLink", "tms1", "tms2", "tms3", "testClass", "a", "b", "c", "testClassIssue",
                        "testClassTmsLink", "testClass", "inheritedLink1", "inheritedLink2", "testClassIssue",
                        "inheritedIssue", "testClassTmsLink", "inheritedTmsLink"
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Test(description = "BDD annotations")
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
    @Test(description = "Should support TestNG retries")
    public void retryTest() {
        final AllureResults results = runTestNgSuites("suites/retry.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2);
    }

    @AllureFeatures.Severity
    @Test(description = "Should add severity for tests")
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
    @Test(description = "Should add owner to tests")
    public void ownerTest() {
        final AllureResults results = runTestNgSuites("suites/owner.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(8)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "owner".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("charlie", "charlie", "other-guy", "eroshenkoam", "eroshenkoam", "other-guy");
    }

    @AllureFeatures.Attachments
    @Test(description = "Should add attachments to tests")
    public void attachmentsTest() {
        final AllureResults results = runTestNgSuites("suites/attachments.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getAttachments)
                .hasSize(1)
                .extracting(Attachment::getName)
                .containsExactly("String attachment");
    }

    @AllureFeatures.MarkerAnnotations
    @Issue("42")
    @Test(description = "Should process flaky for failed tests")
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
    @Test(description = "Should use parameters for history id")
    public void shouldUseParametersForHistoryIdGeneration() {
        final AllureResults results = runTestNgSuites("suites/history-id-parameters.xml");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getHistoryId)
                .doesNotHaveDuplicates();
    }

    @AllureFeatures.History
    @Test(description = "Should generate the same history id for the same tests")
    public void shouldGenerateSameHistoryIdForTheSameTests() {
        final AllureResults results = runTestNgSuites("suites/history-id-the-same.xml");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getHistoryId)
                .containsExactlyInAnyOrder("45e3e2818aabf660b03908be12ba64f7", "45e3e2818aabf660b03908be12ba64f7");
    }

    @SuppressWarnings("unchecked")
    @AllureFeatures.Fixtures
    @Issue("67")
    @Test(description = "Should set correct status for fixtures")
    public void shouldSetCorrectStatusesForFixtures() {
        final AllureResults results = runTestNgSuites(
                "suites/per-suite-fixtures-combination.xml",
                "suites/per-method-fixtures-combination.xml",
                "suites/per-class-fixtures-combination.xml",
                "suites/per-test-tag-fixtures-combination.xml",
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
    @Test(description = "Should set correct status for failed before fixtures")
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
    @Test(description = "Should set correct status for failed after fixtures")
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
    @Test(description = "Should process varargs test parameters")
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
    @Test(description = "Should attach class fixtures correctly")
    public void shouldAttachClassFixturesCorrectly() {
        final Consumer<TestNG> configurer = parallel(XmlSuite.ParallelMode.METHODS, 5);

        final AllureResults results = runTestNgSuites(configurer, "suites/gh-99.xml");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "classFixtures1", "classFixtures2",
                        "classFixtures3", "classFixturesInParent"
                );

        assertThat(results.getTestResultContainers())
                .extracting(TestResultContainer::getName)
                .contains(
                        "io.qameta.allure.testng.samples.ClassFixtures1",
                        "io.qameta.allure.testng.samples.ClassFixtures2",
                        "io.qameta.allure.testng.samples.ClassFixtures3",
                        "io.qameta.allure.testng.samples.ClassFixturesInParent"
                );


        final TestResult classFixtures1 = findTestResultByName(results, "classFixtures1");
        final TestResultContainer c1 = findTestContainerByName(results, "io.qameta.allure.testng.samples.ClassFixtures1");

        assertThat(c1.getChildren())
                .containsExactlyInAnyOrder(classFixtures1.getUuid());

        final TestResult classFixtures2 = findTestResultByName(results, "classFixtures2");
        final TestResultContainer c2 = findTestContainerByName(results, "io.qameta.allure.testng.samples.ClassFixtures2");

        assertThat(c2.getChildren())
                .containsExactlyInAnyOrder(classFixtures2.getUuid());

        final TestResult classFixtures3 = findTestResultByName(results, "classFixtures3");

        final TestResultContainer c3 = findTestContainerByName(results, "io.qameta.allure.testng.samples.ClassFixtures3");

        assertThat(c3.getChildren())
                .containsExactlyInAnyOrder(classFixtures3.getUuid());

        final TestResult classFixturesInParent = findTestResultByName(results, "classFixturesInParent");
        final TestResultContainer c4 = findTestContainerByName(results, "io.qameta.allure.testng.samples.ClassFixturesInParent");

        assertThat(c4.getChildren())
                .containsExactlyInAnyOrder(classFixturesInParent.getUuid());


    }

    @AllureFeatures.History
    @Issue("102")
    @Test(description = "Should generate different history id for inherited tests")
    public void shouldGenerateDifferentHistoryIdForInheritedTests() {
        final AllureResults results = runTestNgSuites("suites/gh-102.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getHistoryId)
                .doesNotHaveDuplicates();
    }

    @AllureFeatures.Fixtures
    @Issue("101")
    @Test(description = "Should use fixture description")
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
    @Test(
            description = "Should not mix up fixtures during parallel run",
            dataProvider = "parallelConfiguration"
    )
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
                .extracting(ExecutableItem::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactly("fail");
    }

    @AllureFeatures.IgnoredTests
    @Issue("49")
    @Test
    public void shouldDisplayDisabledTests() {
        final AllureResults results = runTestNgSuites("suites/gh-49.xml");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(
                        tuple("disabled", null)
                );

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
                .containsExactly(
                        tuple("arg0", "null")
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
                .containsExactly(
                        tuple("arg0", "a"),
                        tuple("arg1", "false"),
                        tuple("arg2", "[1, 2, 3]")
                );
    }

    private AllureResults runTestNgSuites(final String... suites) {
        final Consumer<TestNG> emptyConfigurer = testNg -> {
        };
        return runTestNgSuites(emptyConfigurer, suites);
    }

    @Step("Run testng suites {suites}")
    private AllureResults runTestNgSuites(final Consumer<TestNG> configurer,
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

        final AllureResultsWriterStub results = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(results);
        final AllureTestNg adapter = new AllureTestNg(lifecycle);
        final TestNG testNg = new TestNG(false);
        testNg.addListener((ITestNGListener) adapter);
        testNg.setTestSuites(suiteFiles);

        configurer.accept(testNg);

        final AllureLifecycle cached = Allure.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            StepsAspects.setLifecycle(lifecycle);
            AttachmentsAspects.setLifecycle(lifecycle);
            testNg.run();
        } finally {
            Allure.setLifecycle(cached);
            StepsAspects.setLifecycle(cached);
            AttachmentsAspects.setLifecycle(cached);
        }
        return results;
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

    @Step("Find container by name")
    private TestResultContainer findTestContainerByName(final AllureResults results, final String name) {
        return results.getTestResultContainers().stream()
                .filter(testResultContainer -> name.equalsIgnoreCase(testResultContainer.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("can not find container by name " + name));
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

    @Step("Get uuids by container name")
    private static List<String> getUidsByName(List<TestResultContainer> containers, String name) {
        return containers.stream().filter(container -> container.getName().equals(name))
                .map(TestResultContainer::getUuid).collect(Collectors.toList());
    }

    @Step("Check containers children")
    private static void assertContainersChildren(String name, List<TestResultContainer> containers, List<String> uids) {
        assertThat(containers)
                .filteredOn("name", name)
                .flatExtracting(TestResultContainer::getChildren)
                .as("Unexpected children for testng container " + name)
                .containsOnlyElementsOf(uids);
    }

    @Step("Check containers per method")
    private static void assertContainersPerMethod(String name, List<TestResultContainer> containersList,
                                                  List<String> uids) {
        final Condition<List<? extends TestResultContainer>> singlyMapped = new Condition<>(containers ->
                containers.stream().allMatch(c -> c.getChildren().size() == 1),
                format("All containers for per-method fixture %s should be linked to only one testng result", name));

        assertThat(containersList)
                .filteredOn("name", name)
                .is(singlyMapped)
                .flatExtracting(TestResultContainer::getChildren)
                .as("Unexpected children for per-method fixtures " + name)
                .containsOnlyElementsOf(uids);
    }

    @SuppressWarnings("unchecked")
    @Step("Check after fixtures")
    private static void assertAfterFixtures(String containerName, List<TestResultContainer> containers,
                                            Object... afters) {
        assertThat(containers)
                .filteredOn(container -> container.getName().equals(containerName))
                .as("After fixtures are not attached to container " + containerName)
                .flatExtracting(TestResultContainer::getAfters)
                .is(ALL_FINISHED)
                .is(WITH_STEPS)
                .flatExtracting(FixtureResult::getName)
                .containsExactly(afters);
    }

    @SuppressWarnings("unchecked")
    @Step("Check before fixtures")
    private static void assertBeforeFixtures(String containerName, List<TestResultContainer> containers,
                                             Object... befores) {
        assertThat(containers)
                .filteredOn(container -> container.getName().equals(containerName))
                .as("Before fixtures are not attached to container " + containerName)
                .flatExtracting(TestResultContainer::getBefores)
                .is(ALL_FINISHED)
                .is(WITH_STEPS)
                .flatExtracting(FixtureResult::getName)
                .containsExactly(befores);
    }

    @Step("Check that before fixtures javadoc description refer to correct fixture methods")
    private static void checkBeforeJavadocDescriptions(List<TestResultContainer> containers, String methodReference, String expectedDescriptionHtml) {
        assertThat(containers).as("Test containers has not been written")
                .isNotEmpty()
                .filteredOn(container -> !container.getBefores().isEmpty())
                .filteredOn(container -> container.getName().equals(methodReference))
                .extracting(container -> container.getBefores().get(0).getDescriptionHtml().trim())
                .as("Javadoc description of befores have been processed incorrectly")
                .containsOnly(expectedDescriptionHtml);
    }

    @Step("Check that javadoc description of tests refer to correct test methods")
    private static void checkTestJavadocDescriptions(List<TestResult> results, String methodReference, String expectedDescriptionHtml) {
        assertThat(results).as("Test results has not been written")
                .isNotEmpty()
                .filteredOn(result -> result.getFullName().equals(methodReference))
                .extracting(result -> result.getDescriptionHtml().trim())
                .as("Javadoc description of befores have been processed incorrectly")
                .containsOnly(expectedDescriptionHtml);
    }

}
