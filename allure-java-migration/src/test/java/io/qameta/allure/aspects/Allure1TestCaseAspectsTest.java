package io.qameta.allure.aspects;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResultsWriterStub;

import org.assertj.core.api.Condition;
import org.assertj.core.data.Index;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.model.SeverityLevel;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;

/**
 * eroshenkoam
 * 30.04.17
 */
@RunWith(Parameterized.class)
public class Allure1TestCaseAspectsTest {

    private AllureResultsWriterStub results;

    private AllureLifecycle lifecycle;

    @Parameterized.Parameter
    public SimpleTest simpleTest;

    @Parameterized.Parameters
    public static Collection<Object[]> getTests() {
        return Arrays.asList(
                new Object[]{new JunitTest()},
                new Object[]{new TestNgTest()}
        );
    }

    @Before
    public void initLifecycle() {
        results = new AllureResultsWriterStub();
        lifecycle = new AllureLifecycle(results);
        Allure1TestCaseAspects.setLifecycle(lifecycle);

        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        simpleTest.testSomething();

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);
    }

    @Test
    public void shouldProcessFeaturesAnnotation() {
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> label.getName().equals("feature"))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("feature1", "feature2");
    }

    @Test
    public void shouldProcessStoriesAnnotation() {
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> label.getName().equals("story"))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("story1", "story2");

    }

    @Test
    public void shouldProcessSeverityAnnotation() {
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> label.getName().equals("severity"))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder(SeverityLevel.CRITICAL.value());

    }

    @Test
    public void shouldProcessIssuesAnnotation() {
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .containsExactlyInAnyOrder("ISSUE-1", "ISSUE-11", "ISSUE-2", "ISSUE-22", "TEST-1");

    }

    @Test
    public void shouldProcessTitleAnnotation() {
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("testcase");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> label.getName().equals("suite"))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("testsuite");
    }

    @Test
    public void shouldProcessDescriptionAnnotation() {
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getDescription)
                .containsExactlyInAnyOrder("testcase description");

    }

    @Test
    public void shouldProcessParameterAnnotation() {
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .has(new Condition<>(p -> p.getName().equals("parameterWithoutName") && p.getValue().equals("testValue1"), ""), atIndex(0))
                .has(new Condition<>(p -> p.getName().equals("customParameterName") && p.getValue().equals("testValue2"), ""), atIndex(1))
                .extracting(io.qameta.allure.model.Parameter::getName)
                .doesNotContain("uninitializedParameter");
    }

    public interface SimpleTest {

        void testSomething();

    }

    @Title("testsuite")
    @Issue("ISSUE-1")
    @Issues(@Issue("ISSUE-11"))
    @Stories("story1")
    @Features("feature1")
    public static class TestNgTest implements SimpleTest {

        @Parameter
        private String parameterWithoutName;

        @Parameter("customParameterName")
        private String parameterWithName;

        @Parameter
        private String uninitializedParameter;

        @Title("testcase")
        @Description("testcase description")
        @Issue("ISSUE-2")
        @Issues(@Issue("ISSUE-22"))
        @TestCaseId("TEST-1")
        @Stories("story2")
        @Features("feature2")
        @Severity(SeverityLevel.CRITICAL)
        @org.testng.annotations.Test
        public void testSomething() {
            parameterWithoutName = "testValue1";
            parameterWithName = "testValue2";
        }

    }

    @Title("testsuite")
    @Issue("ISSUE-1")
    @Issues(@Issue("ISSUE-11"))
    @Stories("story1")
    @Features("feature1")
    public static class JunitTest implements SimpleTest {

        @Parameter
        private String parameterWithoutName;

        @Parameter("customParameterName")
        private String parameterWithName;

        @Parameter
        private String uninitializedParameter;

        @Title("testcase")
        @Description("testcase description")
        @Issue("ISSUE-2")
        @Issues(@Issue("ISSUE-22"))
        @TestCaseId("TEST-1")
        @Stories("story2")
        @Features("feature2")
        @Severity(SeverityLevel.CRITICAL)
        @org.junit.Test
        public void testSomething() {
            parameterWithoutName = "testValue1";
            parameterWithName = "testValue2";
        }

    }
}
