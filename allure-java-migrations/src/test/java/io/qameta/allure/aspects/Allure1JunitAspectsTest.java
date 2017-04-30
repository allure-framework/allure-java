package io.qameta.allure.aspects;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.aspects.testdata.AllureResultsWriterStub;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.model.SeverityLevel;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * eroshenkoam
 * 30.04.17
 */
public class Allure1JunitAspectsTest {

    private AllureResultsWriterStub results;

    private AllureLifecycle lifecycle;

    @Before
    public void initLifecycle() {
        results = new AllureResultsWriterStub();
        lifecycle = new AllureLifecycle(results);
        Allure1JunitAspects.setLifecycle(lifecycle);

        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        SimpleTest test = new SimpleTest();
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        test.testOutput();

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
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> label.getName().equals("issue"))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("ISSUE-1", "ISSUE-11", "ISSUE-2", "ISSUE-22", "TEST-1");

    }

    @Test
    public void shouldProcessTitleAnnotation() {
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("testcase");

    }

    @Test
    public void shouldProcessDescriptionAnnotation() {
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getDescription)
                .containsExactlyInAnyOrder("testcase description");

    }

    @Issue("ISSUE-1")
    @Issues(@Issue("ISSUE-11"))
    @Stories("story1")
    @Features("feature1")
    public class SimpleTest {

        @Test
        @Title("testcase")
        @Description("testcase description")
        @Issue("ISSUE-2")
        @Issues(@Issue("ISSUE-22"))
        @TestCaseId("TEST-1")
        @Stories("story2")
        @Features("feature2")
        @Severity(SeverityLevel.CRITICAL)
        public void testOutput() {

        }

    }
}
