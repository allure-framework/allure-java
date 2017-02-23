package io.qameta.allure.test;

import io.qameta.allure.Allure;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.testdata.AllureResultsWriterStub;
import io.qameta.allure.testng.AllureTestNg;
import org.assertj.core.api.Condition;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class FeatureCombinationsTest {

    private AllureTestNg adapter;
    private TestNG testNg;
    private AllureResultsWriterStub results;

    @BeforeMethod
    public void prepare() {
        results = new AllureResultsWriterStub();
        final Allure lifecycle = new Allure(results);
        StepsAspects.setAllure(lifecycle);
        adapter = new AllureTestNg(lifecycle);
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

    @Test
    public void parallelDataProvider() {
        runTestNgSuites("suites/parallel-data-provider.xml");
        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> containers = results.getTestContainers();
        assertThat(testResult).as("Not all test case results have been written").hasSize(2000);
        assertThat(containers).as("Not all test containers have been written").hasSize(2);
    }

    @Test
    public void singleTest() {
        final String testName = "testWithOneStep";
        runTestNgSuites("suites/single-test.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected passed test property")
                .hasFieldOrPropertyWithValue("status", Status.PASSED)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(1)
                .flatExtracting(StepResult::getStatus)
                .contains(Status.PASSED);
    }

    @Test
    public void failingByAssertion() {
        String testName = "failingByAssertion";
        runTestNgSuites("suites/failing-by-assertion.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected failed test property")
                .hasFieldOrPropertyWithValue("status", Status.FAILED)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .flatExtracting(StepResult::getStatus)
                .contains(Status.PASSED, Status.FAILED);
    }

    @Test
    public void brokenTest() {
        String testName = "brokenTest";
        runTestNgSuites("suites/broken.xml");
        List<TestResult> testResult = results.getTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected broken test property")
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .flatExtracting(StepResult::getStatus)
                .contains(Status.PASSED, Status.BROKEN);
    }

    @Test
    public void beforeFixturesCombination() {
        String suiteName = "Test suite 3";
        String testTagName = "Test tag 3";
        String beforeMethodName = "io.qameta.allure.samples.BeforeFixturesCombination.beforeMethod";
        String beforeMethodStep = "stepThree";
        String beforeTestStep = "stepTwo";
        String beforeSuite2Step = "beforeSuiteTwoStep";
        String beforeSuite1Step = "beforeSuiteOneStep";

        runTestNgSuites("suites/before-fixtures-combination.xml");
        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();

        assertThat(testResult).as("Unexpected quantity of test case results has been written").hasSize(1);
        assertThat(testContainers).as("Unexpected quantity of test containers has been written")
                .hasSize(3)
                .extracting(TestResultContainer::getName)
                .contains(beforeMethodName, testTagName, suiteName);

        TestResultContainer beforeMethod = testContainers.get(0);
        assertThat(beforeMethod.getBefores()).as("Before method container should have a before and a step")
                .hasSize(1)
                .flatExtracting(FixtureResult::getSteps).hasSize(1)
                .flatExtracting(StepResult::getName).containsExactly(beforeMethodStep);

        TestResultContainer beforeTest = testContainers.get(1);
        assertThat(beforeTest.getBefores()).as("Before test container should have a before and a step")
                .hasSize(1)
                .flatExtracting(FixtureResult::getSteps).hasSize(1)
                .flatExtracting(StepResult::getName).containsExactly(beforeTestStep);

        TestResultContainer beforeSuite = testContainers.get(2);
        assertThat(beforeSuite.getBefores()).as("Before suite container should have 2 befores and 2 steps")
                .hasSize(2)
                .flatExtracting(FixtureResult::getSteps).hasSize(2)
                .flatExtracting(StepResult::getName).containsExactly(beforeSuite2Step, beforeSuite1Step);
    }

    @Test
    public void afterFixturesCombination() {
        String suiteName = "Test suite 1";
        String testTagName = "Test tag 1";
        String afterMethodName = "io.qameta.allure.samples.AfterFixturesCombination.afterMethod";
        String afterMethodStep = "stepThree";
        String afterTestStep = "stepTwo";
        String afterSuite2Step = "afterSuiteTwoStep";
        String afterSuite1Step = "afterSuiteOneStep";

        runTestNgSuites("suites/after-fixtures-combination.xml");
        List<TestResult> testResult = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();

        assertThat(testResult).as("Unexpected quantity of test case results has been written").hasSize(1);
        assertThat(testContainers).as("Unexpected quantity of test containers has been written")
                .hasSize(3)
                .extracting(TestResultContainer::getName)
                .contains(afterMethodName, testTagName, suiteName);

        TestResultContainer afterMethod = testContainers.get(0);
        assertThat(afterMethod.getAfters()).as("After method container should have an after and a step")
                .hasSize(1)
                .flatExtracting(FixtureResult::getSteps).hasSize(1)
                .first().hasFieldOrPropertyWithValue("name", afterMethodStep);

        TestResultContainer afterTest = testContainers.get(1);
        assertThat(afterTest.getAfters()).as("After test container should have an after and a step")
                .hasSize(1)
                .flatExtracting(FixtureResult::getSteps).hasSize(1)
                .flatExtracting(StepResult::getName).containsExactly(afterTestStep);

        TestResultContainer afterSuite = testContainers.get(2);
        assertThat(afterSuite.getAfters()).as("After suite container should have 2 afters and 2 steps")
                .hasSize(2)
                .flatExtracting(FixtureResult::getSteps).hasSize(2)
                .flatExtracting(StepResult::getName).containsExactly(afterSuite2Step, afterSuite1Step);
    }

    @Test
    public void skippedSuiteTest() {
        final Condition<StepResult> skipReason = new Condition<>(step ->
                step.getStatusDetails().getTrace().startsWith("java.lang.RuntimeException: Skip all"),
                "Suite should be skipped because of an exception in before suite");

        runTestNgSuites("suites/skipped-suite.xml");
        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();
        assertThat(testResults).as("Unexpected quantity of test case results has been written")
                .hasSize(2)
                .flatExtracting(TestResult::getStatus).contains(Status.SKIPPED, Status.SKIPPED);
        assertThat(testContainers).as("Unexpected quantity of test containers has been written").hasSize(2);

        assertThat(testContainers.get(1).getBefores())
                .as("Before suite container should have a before method with one step")
                .hasSize(1)
                .flatExtracting(FixtureResult::getSteps)
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .has(skipReason);
    }

    @Test
    public void multipleSuites() {
        String beforeMethodName = "io.qameta.allure.samples.ParameterizedTest.beforeMethod";
        String firstSuiteName = "Test suite 6";
        String firstTagName = "Test tag 6";
        String secondSuiteName = "Test suite 7";
        String secondTagName = "Test tag 7";

        runTestNgSuites("suites/parameterized-test.xml", "suites/single-test.xml");

        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> testContainers = results.getTestContainers();

        assertThat(testResults).as("Unexpected quantity of test case results has been written")
                .hasSize(3);
        List<String> uids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());
        assertThat(testContainers).as("Unexpected quantity of test containers has been written")
                .hasSize(6).extracting(TestResultContainer::getName)
                .contains(beforeMethodName, beforeMethodName, firstTagName, firstSuiteName, secondTagName,
                        secondSuiteName);

        final List<String> firstSuite = uids.subList(0, 2);
        assertContainersChildren(beforeMethodName, testContainers, firstSuite);
        assertContainersChildren(firstTagName, testContainers, firstSuite);
        assertContainersChildren(firstSuiteName, testContainers, getUidsByName(testContainers, firstTagName));
        final List<String> secondSuite = Collections.singletonList(uids.get(2));
        assertContainersChildren(secondTagName, testContainers, secondSuite);
        assertContainersChildren(secondSuiteName, testContainers, getUidsByName(testContainers, secondTagName));
    }

    @Test
    public void parallelMethods() {
        String before1 = "io.qameta.allure.samples.ParallelMethods.beforeMethod";
        String before2 = "io.qameta.allure.samples.ParallelMethods.beforeMethod2";
        String after = "io.qameta.allure.samples.ParallelMethods.afterMethod";
        String testTag = "Test tag 9";

        runTestNgSuites("suites/parallel-methods.xml");
        List<TestResult> testResults = results.getTestResults();
        List<String> uids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toList());
        List<TestResultContainer> testContainers = results.getTestContainers();
        assertThat(testResults).as("Unexpected quantity of test case results has been written")
                .hasSize(2001);
        assertThat(testContainers).as("Unexpected quantity of test containers has been written")
                .hasSize(6005);

        assertContainersPerMethod(before1, testContainers, uids);
        assertContainersPerMethod(before2, testContainers, uids);
        assertContainersPerMethod(after, testContainers, uids);
        assertContainersChildren(testTag, testContainers, uids);
    }

    private static void assertContainersPerMethod(String name, List<TestResultContainer> containersList,
                                                  List<String> uids) {
        final Condition<List<? extends TestResultContainer>> singlyMapped = new Condition<>(containers ->
                containers.stream().allMatch(c -> c.getChildren().size() == 1),
                format("All containers for per-method fixture %s should be linked to only one test result", name));

        assertThat(containersList)
                .filteredOn("name", name)
                .is(singlyMapped)
                .flatExtracting(TestResultContainer::getChildren)
                .as("Unexpected children for per-method fixtures " + name)
                .containsOnlyElementsOf(uids);
    }

    @Test
    public void nestedSteps() {
        String beforeMethod = "io.qameta.allure.samples.NestedSteps.beforeMethod";
        String nestedStep = "nestedStep";
        String stepInBefore = "stepTwo";
        String stepInTest = "stepThree";
        final Condition<StepResult> substep = new Condition<>(step ->
                step.getSteps().get(0).getName().equals(nestedStep),
                "Given step should have a substep with name " + nestedStep);

        runTestNgSuites("suites/nested-steps.xml");
        List<TestResult> testResults = results.getTestResults();
        List<TestResultContainer> containers = results.getTestContainers();
        assertThat(testResults).as("Unexpected quantity of test case results has been written")
                .hasSize(1);
        assertThat(containers).as("Unexpected quantity of test containers has been written")
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

    @Test
    public void flakyTests() throws Exception {
        runTestNgSuites("suites/flaky.xml");

        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(9)
                .filteredOn(flakyPredicate())
                .extracting(TestResult::getFullName)
                .hasSize(7)
                .containsExactly(
                        "io.qameta.allure.samples.FlakyMethods.flakyTest",
                        "io.qameta.allure.samples.FlakyMethods.flakyTest",
                        "io.qameta.allure.samples.FlakyTestClass.flakyAsWell",
                        "io.qameta.allure.samples.FlakyTestClass.flakyTest",
                        "io.qameta.allure.samples.FlakyTestClass.flakyAsWell",
                        "io.qameta.allure.samples.FlakyTestClass.flakyTest",
                        "io.qameta.allure.samples.FlakyTestClassInherited.flakyInherited"
                );
    }

    private Predicate<TestResult> flakyPredicate() {
        return testResult -> Objects.nonNull(testResult.getStatusDetails())
                && testResult.getStatusDetails().isFlaky();
    }

    @Test
    public void mutedTests() throws Exception {
        runTestNgSuites("suites/muted.xml");

        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(9)
                .filteredOn(mutedPredicate())
                .extracting(TestResult::getFullName)
                .hasSize(7)
                .containsExactly(
                        "io.qameta.allure.samples.MutedMethods.mutedTest",
                        "io.qameta.allure.samples.MutedMethods.mutedTest",
                        "io.qameta.allure.samples.MutedTestClass.mutedAsWell",
                        "io.qameta.allure.samples.MutedTestClass.mutedTest",
                        "io.qameta.allure.samples.MutedTestClass.mutedAsWell",
                        "io.qameta.allure.samples.MutedTestClass.mutedTest",
                        "io.qameta.allure.samples.MutedTestClassInherited.mutedInherited"
                );
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
                .as("Unexpected children for test container " + name)
                .containsOnlyElementsOf(uids);
    }
}
