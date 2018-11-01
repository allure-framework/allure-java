package io.qameta.allure.junitplatform;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.junitplatform.features.BrokenTests;
import io.qameta.allure.junitplatform.features.DisabledTests;
import io.qameta.allure.junitplatform.features.DynamicTests;
import io.qameta.allure.junitplatform.features.FailedTests;
import io.qameta.allure.junitplatform.features.OneTest;
import io.qameta.allure.junitplatform.features.ParameterisedTests;
import io.qameta.allure.junitplatform.features.PassedTests;
import io.qameta.allure.junitplatform.features.SkippedTests;
import io.qameta.allure.junitplatform.features.TaggedTests;
import io.qameta.allure.junitplatform.features.TestsClassWithDisplayNameAnnotation;
import io.qameta.allure.junitplatform.features.TestsClassWithoutDisplayNameAnnotation;
import io.qameta.allure.junitplatform.features.TestsWithDescriptions;
import io.qameta.allure.junitplatform.features.TestsWithDisplayName;
import io.qameta.allure.junitplatform.features.TestsWithSteps;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;
import java.util.stream.Stream;

import static io.qameta.allure.junitplatform.features.TaggedTests.CLASS_TAG;
import static io.qameta.allure.junitplatform.features.TaggedTests.METHOD_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unchecked")
@Epic("Allure Junit Platform Integration")
public class AllureJunitPlatformTest {

    private AllureResultsWriterStub results;

    private AllureLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        this.results = new AllureResultsWriterStub();
        this.lifecycle = new AllureLifecycle(results);
    }

    @Test
    void shouldSetFullName() {
        runClasses(PassedTests.class);
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
    void shouldSetExecutionLabels() {
        runClasses(OneTest.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .contains("host", "thread");
    }

    @Test
    void shouldSetSourceLabels() {
        runClasses(OneTest.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("methodName", "single"),
                        tuple("className", "io.qameta.allure.junitplatform.features.OneTest")
                );
    }

    @Test
    void shouldProcessPassedTests() {
        runClasses(PassedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
                .filteredOn(testResult -> Status.PASSED.equals(testResult.getStatus()))
                .flatExtracting(TestResult::getName)
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
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("testA", "testB", "testC");
    }

    @Test
    void shouldProcessParametrisedTests() {
        runClasses(ParameterisedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .filteredOn(testResult -> Status.PASSED.equals(testResult.getStatus()))
                .flatExtracting(TestResult::getName)
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
                .flatExtracting(StepResult::getName)
                .containsExactly("first", "second", "third");

    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldAddTags() {
        runClasses(TaggedTests.class);

        final List<TestResult> testResults = results.getTestResults();

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(testResults)
                .hasSize(1);

        softly.assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "tag".equals(label.getName()))
                .flatExtracting(Label::getValue)
                .containsExactlyInAnyOrder(CLASS_TAG, METHOD_TAG);

        softly.assertAll();
    }

    @Test
    void shouldProcessTestClassDisplayNameByAnnotation() {
        runClasses(TestsClassWithDisplayNameAnnotation.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final List<Label> testResultLabels = testResults.get(0).getLabels();
        assertThat(testResultLabels)
                .filteredOn(label -> "suite".equals(label.getName()))
                .hasSize(1)
                .flatExtracting(Label::getValue)
                .contains("Display name of test class");
    }

    @Test
    void shouldProcessDefaultTestClassDisplayName() {
        runClasses(TestsClassWithoutDisplayNameAnnotation.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final List<Label> testResultLabels = testResults.get(0).getLabels();
        assertThat(testResultLabels)
                .filteredOn(label -> "suite".equals(label.getName()))
                .hasSize(1)
                .flatExtracting(Label::getValue)
                .contains("io.qameta.allure.junitplatform.features.TestsClassWithoutDisplayNameAnnotation");
    }

    @Test
    void shouldProcessJunit5Description() {
        runClasses(TestsWithDescriptions.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getDescription)
                .contains("Test description");
    }

    @Test
    void shouldProcessDisabledTests() {
        runClasses(DisabledTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("status", Status.SKIPPED);
    }

    private void runClasses(Class<?>... classes) {
        final ClassSelector[] classSelectors = Stream.of(classes)
                .map(DiscoverySelectors::selectClass)
                .toArray(ClassSelector[]::new);
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(classSelectors)
                .build();

        final LauncherConfig config = LauncherConfig.builder()
                .enableTestExecutionListenerAutoRegistration(false)
                .addTestExecutionListeners(new AllureJunitPlatform(lifecycle))
                .build();
        final Launcher launcher = LauncherFactory.create(config);

        final AllureLifecycle defaultLifecycle = Allure.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            StepsAspects.setLifecycle(lifecycle);
            AttachmentsAspects.setLifecycle(lifecycle);
            launcher.execute(request);
        } finally {
            Allure.setLifecycle(defaultLifecycle);
            StepsAspects.setLifecycle(defaultLifecycle);
            AttachmentsAspects.setLifecycle(defaultLifecycle);
        }
    }
}
