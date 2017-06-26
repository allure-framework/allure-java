package io.qameta.allure.testng;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Issue;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.testdata.AllureResultsWriterStub;
import org.assertj.core.api.Condition;
import org.assertj.core.groups.Tuple;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@Test
public class FeatureCombinationsTest {

    private static final Condition<List<? extends ExecutableItem>> ALL_FINISHED = new Condition<>(items ->
            items.stream().allMatch(item -> item.getStage() == Stage.FINISHED),
            "All items should have be in a finished stage");

    private static final Condition<List<? extends ExecutableItem>> WITH_STEPS = new Condition<>(items ->
            items.stream().allMatch(item -> item.getSteps().size() == 1),
            "All items should have a step attached");

    private TestNG testNg;
    private AllureResultsWriterStub results;

    @BeforeMethod
    public void prepare() {
        results = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(results);
        StepsAspects.setLifecycle(lifecycle);
        AttachmentsAspects.setLifecycle(lifecycle);
        AllureTestNg adapter = new AllureTestNg(lifecycle);
        testNg = new TestNG(false);
        testNg.addListener((ITestNGListener) adapter);
    }

    private void runTestNgSuites(String... suites) {
        final ClassLoader classLoader = getClass().getClassLoader();
        List<String> suiteFiles = Arrays.stream(suites)
                .map(classLoader::getResource)
                .map(URL::getFile)
                .collect(Collectors.toList());
        assertThat(suites).as("Cannot find all suite xml files").hasSameSizeAs(suiteFiles);
        testNg.setTestSuites(suiteFiles);
        testNg.run();
    }

    @Test(description = "Parallel data provider tests")
    public void parallelDataProvider() {
        runTestNgSuites("suites/parallel-data-provider.xml");
        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> containers = results.getTestContainers();
        assertThat(testResult).as("Not all testng case results have been written").hasSize(2000);
        assertThat(containers).as("Not all testng containers have been written").hasSize(2);
    }

    @Test(description = "Singe testng")
    public void singleTest() {
        final String testName = "testWithOneStep";
        runTestNgSuites("suites/single-test.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected passed testng property")
                .hasFieldOrPropertyWithValue("status", Status.PASSED)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(1)
                .flatExtracting(StepResult::getStatus)
                .contains(Status.PASSED);
    }

    @Test(description = "Descriptions of tests and steps")
    public void descriptionsTest() {
        final String testDescription = "Sample test description";
        final String stepDescription = "Sample step description";
        runTestNgSuites("suites/descriptions-test.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written")
                .hasSize(1).first()
                .extracting(result -> result.getDescriptionHtml().trim())
                .as("Javadoc description of test case has not been processed")
                .contains(testDescription);

        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .as("Steps have not been processed")
                .hasSize(1)
                .extracting(result -> result.getDescriptionHtml().trim())
                .as("Javadoc description of steps has not been processed")
                .contains(stepDescription);
    }

    @Test(description = "Test failing by assertion")
    public void failingByAssertion() {
        String testName = "failingByAssertion";
        runTestNgSuites("suites/failing-by-assertion.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected failed testng property")
                .hasFieldOrPropertyWithValue("status", Status.FAILED)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .flatExtracting(StepResult::getStatus)
                .contains(Status.PASSED, Status.FAILED);
    }

    @Test(description = "Broken testng")
    public void brokenTest() {
        String testName = "brokenTest";
        runTestNgSuites("suites/broken.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected broken testng property")
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .flatExtracting(StepResult::getStatus)
                .contains(Status.PASSED, Status.BROKEN);
    }

    @Test(description = "Suite fixtures")
    public void perSuiteFixtures() {
        String suiteName = "Test suite 12";
        String testTagName = "Test tag 12";
        String before1 = "beforeSuite1";
        String before2 = "beforeSuite2";
        String after1 = "afterSuite1";
        String after2 = "afterSuite2";

        runTestNgSuites("suites/per-suite-fixtures-combination.xml");

        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();

        assertThat(testResult).as("Unexpected quantity of testng case results has been written").hasSize(1);
        List<String> testUuid = singletonList(testResult.get(0).getUuid());
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written")
                .hasSize(2);

        assertContainersChildren(testTagName, testContainers, testUuid);
        assertContainersChildren(suiteName, testContainers, getUidsByName(testContainers, testTagName));
        assertBeforeFixtures(suiteName, testContainers, before1, before2);
        assertAfterFixtures(suiteName, testContainers, after1, after2);
    }

    @Test(description = "Class fixtures")
    public void perClassFixtures() {
        String suiteName = "Test suite 11";
        String testTagName = "Test tag 11";
        String beforeClass = "beforeClass";
        String afterClass = "afterClass";

        runTestNgSuites("suites/per-class-fixtures-combination.xml");

        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written").hasSize(2);
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written").hasSize(2);

        List<String> uuids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());

        assertContainersChildren(testTagName, testContainers, uuids);
        assertContainersChildren(suiteName, testContainers, getUidsByName(testContainers, testTagName));

        assertBeforeFixtures(testTagName, testContainers, beforeClass);
        assertAfterFixtures(testTagName, testContainers, afterClass);
    }

    @Test(description = "Method fixtures")
    public void perMethodFixtures() {
        String suiteName = "Test suite 11";
        String testTagName = "Test tag 11";
        String before1 = "io.qameta.allure.testng.samples.PerMethodFixtures.beforeMethod1";
        String before2 = "io.qameta.allure.testng.samples.PerMethodFixtures.beforeMethod2";
        String after1 = "io.qameta.allure.testng.samples.PerMethodFixtures.afterMethod1";
        String after2 = "io.qameta.allure.testng.samples.PerMethodFixtures.afterMethod2";

        runTestNgSuites("suites/per-method-fixtures-combination.xml");

        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written").hasSize(2);
        List<String> uuids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written")
                .hasSize(10);

        assertContainersChildren(testTagName, testContainers, uuids);
        assertContainersChildren(suiteName, testContainers, getUidsByName(testContainers, testTagName));
        assertContainersPerMethod(before1, testContainers, uuids);
        assertContainersPerMethod(before2, testContainers, uuids);
        assertContainersPerMethod(after1, testContainers, uuids);
        assertContainersPerMethod(after2, testContainers, uuids);
    }

    @Test(description = "Test fixtures")
    public void perTestTagFixtures() {
        String suiteName = "Test suite 13";
        String testTagName = "Test tag 13";
        String before1 = "beforeTest1";
        String before2 = "beforeTest2";
        String after1 = "afterTest1";
        String after2 = "afterTest2";

        runTestNgSuites("suites/per-test-tag-fixtures-combination.xml");

        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();

        assertThat(testResult).as("Unexpected quantity of testng case results has been written").hasSize(1);
        List<String> testUuid = singletonList(testResult.get(0).getUuid());
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written")
                .hasSize(2);

        assertContainersChildren(testTagName, testContainers, testUuid);
        assertContainersChildren(suiteName, testContainers, getUidsByName(testContainers, testTagName));
        assertBeforeFixtures(testTagName, testContainers, before1, before2);
        assertAfterFixtures(testTagName, testContainers, after1, after2);
    }

    @Test(description = "Skipped suite")
    public void skippedSuiteTest() {
        final Condition<StepResult> skipReason = new Condition<>(step ->
                step.getStatusDetails().getTrace().startsWith("java.lang.RuntimeException: Skip all"),
                "Suite should be skipped because of an exception in before suite");

        runTestNgSuites("suites/skipped-suite.xml");
        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(2)
                .flatExtracting(TestResult::getStatus).contains(Status.SKIPPED, Status.SKIPPED);
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written").hasSize(2);

        assertThat(testContainers.get(1).getBefores())
                .as("Before suite container should have a before method with one step")
                .hasSize(1)
                .flatExtracting(FixtureResult::getSteps)
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .has(skipReason);
    }

    @Test(description = "Multi suites")
    public void multipleSuites() {
        String beforeMethodName = "io.qameta.allure.testng.samples.ParameterizedTest.beforeMethod";
        String firstSuiteName = "Test suite 6";
        String firstTagName = "Test tag 6";
        String secondSuiteName = "Test suite 7";
        String secondTagName = "Test tag 7";

        runTestNgSuites("suites/parameterized-test.xml", "suites/single-test.xml");

        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(3);
        List<String> uids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written")
                .hasSize(6).extracting(TestResultContainer::getName)
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

    @Test(description = "Before Suite Parameter")
    public void testBeforeSuiteParameter() {
        runTestNgSuites("suites/parameterized-suite1.xml", "suites/parameterized-suite2.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .extracting(TestResult::getFullName)
                .containsExactly(
                        "io.qameta.allure.testng.samples.SuiteParametersTest.simpleTest[param=first]",
                        "io.qameta.allure.testng.samples.SuiteParametersTest.simpleTest[param=second]"
                );
    }

    @Test(description = "Parallel methods")
    public void parallelMethods() {
        String before1 = "io.qameta.allure.testng.samples.ParallelMethods.beforeMethod";
        String before2 = "io.qameta.allure.testng.samples.ParallelMethods.beforeMethod2";
        String after = "io.qameta.allure.testng.samples.ParallelMethods.afterMethod";
        String testTag = "Test tag 9";

        runTestNgSuites("suites/parallel-methods.xml");
        List<TestResult> testResults = results.getTestResults();
        List<String> uids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());
        List<TestResultContainer> testContainers = results.getTestContainers();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(2001);
        assertThat(testContainers).as("Unexpected quantity of testng containers has been written")
                .hasSize(6005);

        assertContainersPerMethod(before1, testContainers, uids);
        assertContainersPerMethod(before2, testContainers, uids);
        assertContainersPerMethod(after, testContainers, uids);
        assertContainersChildren(testTag, testContainers, uids);
    }

    @Test(description = "Nested steps")
    public void nestedSteps() {
        String beforeMethod = "io.qameta.allure.testng.samples.NestedSteps.beforeMethod";
        String nestedStep = "nestedStep";
        String stepInBefore = "stepTwo";
        String stepInTest = "stepThree";
        final Condition<StepResult> substep = new Condition<>(step ->
                step.getSteps().get(0).getName().equals(nestedStep),
                "Given step should have a substep with name " + nestedStep);

        runTestNgSuites("suites/nested-steps.xml");
        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> containers = results.getTestContainers();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(1);
        assertThat(containers).as("Unexpected quantity of testng containers has been written")
                .hasSize(3);

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

    @Test(description = "Flaky tests")
    public void flakyTests() throws Exception {
        runTestNgSuites("suites/flaky.xml");

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

    @Test(description = "Muted tests")
    public void mutedTests() throws Exception {
        runTestNgSuites("suites/muted.xml");

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

    @Test(description = "Tests with links")
    public void linksTest() throws Exception {
        runTestNgSuites("suites/links.xml");

        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(4)
                .filteredOn(hasLinks())
                .hasSize(4)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .containsExactly("testClass", "a", "b", "c", "testClassIssue", "testClassTmsLink",
                        "testClass", "nested1", "nested2", "nested3", "testClassIssue", "issue1", "issue2", "issue3",
                        "testClassTmsLink", "tms1", "tms2", "tms3", "testClass", "a", "b", "c", "testClassIssue",
                        "testClassTmsLink", "testClass", "inheritedLink1", "inheritedLink2", "testClassIssue",
                        "inheritedIssue", "testClassTmsLink", "inheritedTmsLink"
                );
    }

    @Test(description = "BDD annotations")
    public void bddAnnotationsTest() throws Exception {
        runTestNgSuites("suites/bdd-annotations.xml");

        List<String> bddLabels = Arrays.asList("epic", "feature", "story");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> bddLabels.contains(label.getName()))
                .extracting(Label::getValue)
                .containsExactly(
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

    @Test
    public void retryTest() throws Exception {
        runTestNgSuites("suites/retry.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2);
    }

    @Test
    public void severityTest() throws Exception {
        runTestNgSuites("suites/severity.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(8)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "severity".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("critical", "critical", "minor", "blocker", "minor", "blocker");
    }

    @Test
    public void ownerTest() throws Exception {
        runTestNgSuites("suites/owner.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(8)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "owner".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("charlie", "charlie", "other-guy", "eroshenkoam", "other-guy", "eroshenkoam");
    }

    @Test
    public void attachmentsTest() throws Exception {
        runTestNgSuites("suites/attachments.xml");
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getAttachments)
                .hasSize(1)
                .flatExtracting(Attachment::getName)
                .containsExactly("String attachment");
    }

    @Test
    public void shouldAddFlakyToFailedTests() throws Exception {
        runTestNgSuites("suites/flaky-with-failure.xml");

        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .filteredOn(flakyPredicate())
                .extracting(TestResult::getFullName)
                .hasSize(1)
                .containsExactly(
                        "io.qameta.allure.testng.samples.FailedFlakyTest.flakyWithFailure");
    }

    @Test
    public void shouldUseParametersForHistoryIdGeneration() throws Exception {
        runTestNgSuites("suites/history-id-parameters.xml");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
                .extracting(TestResult::getHistoryId)
                .containsExactlyInAnyOrder(
                        "84bf9104f500de5d87d57530adbd6721",
                        "84bf9104f500de5d87d57530adbd6721",
                        "71e91659283b08ec583ddcdad73075c7"
                );
    }

    @Issue("67")
    @Test
    public void shouldSetCorrectStatusesForFixtures() throws Exception {
        runTestNgSuites(
                "suites/per-suite-fixtures-combination.xml",
                "suites/per-method-fixtures-combination.xml",
                "suites/per-class-fixtures-combination.xml",
                "suites/per-test-tag-fixtures-combination.xml",
                "suites/failed-test-passed-fixture.xml"
        );

        assertThat(results.getTestContainers())
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

        assertThat(results.getTestContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .hasSize(9)
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

    @Issue("67")
    @Test
    public void shouldSetCorrectStatusForFailedBeforeFixtures() throws Exception {
        runTestNgSuites(
                "suites/failed-before-suite-fixture.xml",
                "suites/failed-before-test-fixture.xml",
                "suites/failed-before-method-fixture.xml"
        );

        assertThat(results.getTestContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .hasSize(3)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("beforeSuite", Status.BROKEN),
                        Tuple.tuple("beforeTest", Status.BROKEN),
                        Tuple.tuple("beforeMethod", Status.BROKEN)
                );
    }

    @Issue("67")
    @Test
    public void shouldSetCorrectStatusForFailedAfterFixtures() throws Exception {
        runTestNgSuites(
                "suites/failed-after-suite-fixture.xml",
                "suites/failed-after-test-fixture.xml",
                "suites/failed-after-method-fixture.xml"
        );

        assertThat(results.getTestContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .hasSize(3)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("afterSuite", Status.BROKEN),
                        Tuple.tuple("afterTest", Status.BROKEN),
                        Tuple.tuple("afterMethod", Status.BROKEN)
                );
    }

    private Predicate<TestResult> hasLinks() {
        return testResult -> !testResult.getLinks().isEmpty();
    }

    private Predicate<TestResult> flakyPredicate() {
        return testResult -> Objects.nonNull(testResult.getStatusDetails())
                && testResult.getStatusDetails().isFlaky();
    }

    private Predicate<TestResult> mutedPredicate() {
        return testResult -> Objects.nonNull(testResult.getStatusDetails())
                && testResult.getStatusDetails().isMuted();
    }

    private static List<String> getUidsByName(List<TestResultContainer> containers, String name) {
        return containers.stream().filter(container -> container.getName().equals(name))
                .map(TestResultContainer::getUuid).collect(Collectors.toList());
    }

    private static void assertContainersChildren(String name, List<TestResultContainer> containers, List<String> uids) {
        assertThat(containers)
                .filteredOn("name", name)
                .flatExtracting(TestResultContainer::getChildren)
                .as("Unexpected children for testng container " + name)
                .containsOnlyElementsOf(uids);
    }

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
}
