package io.qameta.allure.spock

import io.qameta.allure.model.Stage
import io.qameta.allure.model.Status
import io.qameta.allure.model.TestResult
import io.qameta.allure.spock.samples.BrokenTest
import io.qameta.allure.spock.samples.FailedTest
import io.qameta.allure.spock.samples.OneTest
import io.qameta.allure.spock.samples.TestWithAnnotations
import io.qameta.allure.spock.samples.TestWithSteps
import io.qameta.allure.testdata.AllureSpockRunner
import org.junit.Test

import java.util.function.Predicate

import static org.assertj.core.api.Java6Assertions.assertThat

/**
 * Created on 14.06.2017
 *
 * @author Yuri Kudryavtsev
 *         skype: yuri.kudryavtsev.indeed
 *         email: yuri.kudryavtsev@indeed-id.com
 */

class AllureSpockTest {

    @Test
    void shouldSetTestStart() {
        List<TestResult> testResults = AllureSpockRunner.run(OneTest)
        assertThat(testResults)
                .hasSize(1)
                .extracting({it.start})
                .isNotNull()
    }

    @Test
    void shouldSetTestStop() {
        List<TestResult> testResults = AllureSpockRunner.run(OneTest)
        assertThat(testResults)
                .hasSize(1)
                .extracting({it.stop})
                .isNotNull()
    }

    @Test
    void shouldSetTestFullName() {
        List<TestResult> testResults = AllureSpockRunner.run(OneTest)
        assertThat(testResults)
                .hasSize(1)
                .extracting({it.fullName})
                .containsExactly("io.qameta.allure.spock.samples.OneTest.Simple Test")
    }

    @Test
    void shouldSetStageFinished() {
        List<TestResult> testResults = AllureSpockRunner.run(OneTest)
        assertThat(testResults)
                .hasSize(1)
                .extracting({it.stage})
                .containsExactly(Stage.FINISHED)
    }

    @Test
    void shouldProcessFailedTest() {
        List<TestResult> testResults = AllureSpockRunner.run(FailedTest)
        assertThat(testResults)
                .hasSize(1)
                .extracting({it.status})
                .containsExactly(Status.FAILED)
    }

    @Test
    void shouldProcessBrokenTest() {
        List<TestResult> testResults = AllureSpockRunner.run(BrokenTest)
        assertThat(testResults)
                .hasSize(1)
                .extracting({it.status})
                .containsExactly(Status.BROKEN)
    }

    @Test
    void shouldAddStepsToTest() {
        List<TestResult> testResults = AllureSpockRunner.run(TestWithSteps)
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting({it.steps})
                .hasSize(3)
                .extracting({it.name})
                .containsExactly("step1", "step2", "step3")
    }

    @Test
    void shouldProcessMethodAnnotations() {
        List<TestResult> testResults = AllureSpockRunner.run(TestWithAnnotations.class)
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting({it.labels})
                .extracting({it.value})
                .contains(
                "epic1", "epic2", "epic3",
                "feature1", "feature2", "feature3",
                "story1", "story2", "story3",
                "some-owner"
        )
    }

    @Test
    void shouldProcessFlakyAnnotation() {
        List<TestResult> testResults = AllureSpockRunner.run(TestWithAnnotations.class)
        assertThat(testResults)
                .hasSize(1)
                .filteredOn(flakyPredicate())
                .hasSize(1)
    }

    @Test
    void shouldProcessMutedAnnotation() {
        List<TestResult> testResults = AllureSpockRunner.run(TestWithAnnotations.class)
        assertThat(testResults)
                .hasSize(1)
                .filteredOn(mutedPredicate())
                .hasSize(1)
    }

    @Test
    void shouldSetDisplayName() {
        List<TestResult> testResults = AllureSpockRunner.run(OneTest.class)
        assertThat(testResults)
                .hasSize(1)
                .extracting({it.name})
                .containsExactly("Simple Test")
    }

    @Test
    void shouldSetLinks() {
        List<TestResult> testResults = AllureSpockRunner.run(FailedTest.class)
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting({it.links})
                .extracting({it.name})
                .containsExactly("link-1", "link-2", "issue-1", "issue-2", "tms-1", "tms-2")
    }

    private static Predicate<TestResult> flakyPredicate() {
        return {
            testResult -> (Objects.nonNull(testResult.getStatusDetails())
                    && testResult.getStatusDetails().isFlaky())
        }
    }

    private static Predicate<TestResult> mutedPredicate() {
        return {
            testResult -> (Objects.nonNull(testResult.getStatusDetails())
                    && testResult.getStatusDetails().isMuted())
        }
    }

}
