package io.qameta.allure.junit4;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.junit4.samples.AssumptionFailedTest;
import io.qameta.allure.junit4.samples.BrokenTest;
import io.qameta.allure.junit4.samples.FailedTest;
import io.qameta.allure.junit4.samples.IgnoredClassTest;
import io.qameta.allure.junit4.samples.IgnoredTests;
import io.qameta.allure.junit4.samples.OneTest;
import io.qameta.allure.junit4.samples.TaggedTests;
import io.qameta.allure.junit4.samples.TestWithAnnotations;
import io.qameta.allure.junit4.samples.TestWithSteps;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

import java.util.List;

import static io.qameta.allure.junit4.samples.TaggedTests.CLASS_TAG1;
import static io.qameta.allure.junit4.samples.TaggedTests.CLASS_TAG2;
import static io.qameta.allure.junit4.samples.TaggedTests.METHOD_TAG1;
import static io.qameta.allure.junit4.samples.TaggedTests.METHOD_TAG2;

import static org.assertj.core.api.Assertions.assertThat;

public class FeatureCombinationsTest {

    private JUnitCore core;
    private AllureResultsWriterStub results;

    @Before
    public void prepare() {
        results = new AllureResultsWriterStub();
        AllureLifecycle lifecycle = new AllureLifecycle(results);
        StepsAspects.setLifecycle(lifecycle);
        AllureJunit4 listener = new AllureJunit4(lifecycle);
        core = new JUnitCore();
        core.addListener(listener);
    }

    @Test
    @DisplayName("Should set full name")
    public void shouldSetTestFullName() throws Exception {
        core.run(Request.aClass(OneTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getFullName)
                .containsExactly("io.qameta.allure.junit4.samples.OneTest.simpleTest");
    }

    @Test
    @DisplayName("Should set start time")
    public void shouldSetTestStart() throws Exception {
        core.run(Request.aClass(OneTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStart)
                .isNotNull();
    }

    @Test
    @DisplayName("Should set stop time")
    public void shouldSetTestStop() throws Exception {
        core.run(Request.aClass(OneTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStop)
                .isNotNull();
    }

    @Test
    @DisplayName("Should set finished stage")
    public void shouldSetStageFinished() throws Exception {
        core.run(Request.aClass(OneTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStage)
                .containsExactly(Stage.FINISHED);
    }

    @Test
    @DisplayName("Failed test")
    public void shouldProcessFailedTest() throws Exception {
        core.run(Request.aClass(FailedTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.FAILED);
    }

    @Test
    @DisplayName("Broken test")
    public void shouldProcessBrokenTest() throws Exception {
        core.run(Request.aClass(BrokenTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.BROKEN);
    }

    @Test
    @DisplayName("Skipped test")
    public void shouldProcessSkippedTest() throws Exception {
        core.run(Request.aClass(AssumptionFailedTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.SKIPPED);
    }

    @Test
    @DisplayName("Ignored tests")
    public void shouldProcessIgnoredTest() throws Exception {
        core.run(Request.aClass(IgnoredTests.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .flatExtracting(TestResult::getStatus)
                .containsExactly(Status.SKIPPED, Status.SKIPPED);
    }

    @Test
    @DisplayName("Ignored tests messages")
    public void shouldProcessIgnoredTestDescription() throws Exception {
        core.run(Request.aClass(IgnoredTests.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactlyInAnyOrder("Test ignored (without reason)!", "Ignored for some reason");
    }

    @Test
    @DisplayName("Test result for ignored class gets named by the class name")
    public void shouldSetNameForIgnoredClass() {
        core.run(Request.aClass(IgnoredClassTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactly("io.qameta.allure.junit4.samples.IgnoredClassTest");
    }

    @Test
    @DisplayName("Test with steps")
    public void shouldAddStepsToTest() throws Exception {
        core.run(Request.aClass(TestWithSteps.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .hasSize(3)
                .extracting(StepResult::getName)
                .containsExactly("step1", "step2", "step3");
    }

    @Test
    @DisplayName("Annotations on method")
    public void shouldProcessMethodAnnotations() throws Exception {
        core.run(Request.aClass(TestWithAnnotations.class));
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
    @DisplayName("Should set display name")
    public void shouldSetDisplayName() throws Exception {
        core.run(Request.aClass(OneTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getName)
                .containsExactly("Simple test");
    }

    @Test
    @DisplayName("Should set suite name")
    public void shouldSetSuiteName() throws Exception {
        core.run(Request.aClass(OneTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "suite".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactly("Should be overwritten by method annotation");
    }

    @Test
    @DisplayName("Should set description")
    public void shouldSetDescription() throws Exception {
        core.run(Request.aClass(OneTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .extracting(TestResult::getDescription)
                .containsExactly("Description here");
    }

    @Test
    @DisplayName("Should set links")
    public void shouldSetLinks() throws Exception {
        core.run(Request.aClass(FailedTest.class));
        List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .containsExactly("link-1", "link-2", "issue-1", "issue-2", "tms-1", "tms-2");
    }

    @Test
    @DisplayName("Should set tags")
    public void shouldSetTags() throws Exception {
        core.run(Request.aClass(TaggedTests.class));
        List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "tag".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder(CLASS_TAG1, CLASS_TAG2, METHOD_TAG1, METHOD_TAG2);
    }

    @Test
    @DisplayName("Should not throw exception processing test from default package")
    public void shouldProcessTestFromDefaultPackage() throws Exception {
        Class<?> testInDefaultPackage = Class.forName("SampleTestInDefaultPackage");
        Result result = core.run(Request.aClass(testInDefaultPackage));
        assertThat(result.wasSuccessful()).isTrue();
    }
}
