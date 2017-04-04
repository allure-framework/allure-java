package io.qameta.allure.junit5;

import io.qameta.allure.AllureJunit5;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.junit5.features.BrokenTests;
import io.qameta.allure.junit5.features.DynamicTests;
import io.qameta.allure.junit5.features.FailedTests;
import io.qameta.allure.junit5.features.ParameterisedTests;
import io.qameta.allure.junit5.features.PassedTests;
import io.qameta.allure.junit5.features.SkippedTests;
import io.qameta.allure.junit5.features.TestsWithDisplayName;
import io.qameta.allure.junit5.features.TestsWithSteps;
import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.testdata.AllureResultsWriterStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureJunit5Test {

    private AllureResultsWriterStub results;

    private AllureLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        this.results = new AllureResultsWriterStub();
        this.lifecycle = new AllureLifecycle(results);
        StepsAspects.setLifecycle(lifecycle);
        AttachmentsAspects.setLifecycle(lifecycle);
    }

    @Test
    void shouldProcessPassedTests() {
        runClasses(PassedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
                .filteredOn(testResult -> Status.PASSED.equals(testResult.getStatus()))
                .flatExtracting(ExecutableItem::getName)
                .containsExactlyInAnyOrder("first()", "second()", "third()");
    }

    @Test
    void shouldProcessFailedTests() {
        runClasses(FailedTests.class);

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
    void shouldProcessBrokenTests() {
        runClasses(BrokenTests.class);

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
    void shouldProcessSkippedTests() {
        runClasses(SkippedTests.class);

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
    void shouldProcessDisplayName() {
        runClasses(TestsWithDisplayName.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrPropertyWithValue("name", "Some test with changed name");
    }

    @Test
    void shouldSetStartAndStopTimes() {
        runClasses(FailedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrProperty("start")
                .hasFieldOrProperty("stop");
    }

    @Test
    void shouldSetFinishedStage() {
        runClasses(FailedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED);
    }

    @Test
    void shouldProcessDynamicTests() {
        runClasses(DynamicTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
                .filteredOn(testResult -> Status.PASSED.equals(testResult.getStatus()))
                .flatExtracting(ExecutableItem::getName)
                .containsExactlyInAnyOrder("testA", "testB", "testC");
    }

    @Test
    void shouldProcessParametrisedTests() {
        runClasses(ParameterisedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .filteredOn(testResult -> Status.PASSED.equals(testResult.getStatus()))
                .flatExtracting(ExecutableItem::getName)
                .containsExactlyInAnyOrder("[1] Hello", "[2] World");
    }

    @Test
    void shouldAddSteps() {
        runClasses(TestsWithSteps.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult.getSteps())
                .hasSize(3)
                .flatExtracting(ExecutableItem::getName)
                .containsExactly("first", "second", "third");

    }

    private void runClasses(Class<?>... classes) {
        final ClassSelector[] classSelectors = Stream.of(classes)
                .map(DiscoverySelectors::selectClass)
                .toArray(ClassSelector[]::new);
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(classSelectors)
                .build();

        final Launcher launcher = LauncherFactory.create();
        launcher.execute(request, new AllureJunit5(lifecycle));
    }
}
