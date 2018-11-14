package io.qameta.allure.aspects;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * eroshenkoam
 * 30.04.17
 */
class Allure1TestCaseAspectsTest {

    static Stream<SimpleTest> testClassesProvider() {
        return Stream.of(new JunitTest(), new TestNgTest());
    }

    @ParameterizedTest
    @MethodSource("testClassesProvider")
    void shouldProcessFeaturesAnnotation(final SimpleTest simpleTest) {
        final AllureResults results = runWithinTestContext(
                simpleTest::testSomething,
                Allure1TestCaseAspects::setLifecycle,
                Allure1ParametersAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> label.getName().equals("feature"))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("feature1", "feature2");
    }

    @ParameterizedTest
    @MethodSource("testClassesProvider")
    void shouldProcessStoriesAnnotation(final SimpleTest simpleTest) {
        final AllureResults results = runWithinTestContext(
                simpleTest::testSomething,
                Allure1TestCaseAspects::setLifecycle,
                Allure1ParametersAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> label.getName().equals("story"))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("story1", "story2");

    }

    @ParameterizedTest
    @MethodSource("testClassesProvider")
    void shouldProcessSeverityAnnotation(final SimpleTest simpleTest) {
        final AllureResults results = runWithinTestContext(
                simpleTest::testSomething,
                Allure1TestCaseAspects::setLifecycle,
                Allure1ParametersAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> label.getName().equals("severity"))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder(SeverityLevel.CRITICAL.value());

    }

    @ParameterizedTest
    @MethodSource("testClassesProvider")
    void shouldProcessIssuesAnnotation(final SimpleTest simpleTest) {
        final AllureResults results = runWithinTestContext(
                simpleTest::testSomething,
                Allure1TestCaseAspects::setLifecycle,
                Allure1ParametersAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .containsExactlyInAnyOrder("ISSUE-1", "ISSUE-11", "ISSUE-2", "ISSUE-22", "TEST-1");

    }

    @ParameterizedTest
    @MethodSource("testClassesProvider")
    void shouldProcessTitleAnnotation(final SimpleTest simpleTest) {
        final AllureResults results = runWithinTestContext(
                simpleTest::testSomething,
                Allure1TestCaseAspects::setLifecycle,
                Allure1ParametersAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("testcase");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> label.getName().equals("suite"))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("testsuite");
    }

    @ParameterizedTest
    @MethodSource("testClassesProvider")
    void shouldProcessDescriptionAnnotation(final SimpleTest simpleTest) {
        final AllureResults results = runWithinTestContext(
                simpleTest::testSomething,
                Allure1TestCaseAspects::setLifecycle,
                Allure1ParametersAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getDescription)
                .containsExactlyInAnyOrder("testcase description");

    }

    @SuppressWarnings("unchecked")
    @Issue("206")
    @ParameterizedTest
    @MethodSource("testClassesProvider")
    void shouldProcessParameterAnnotation(final SimpleTest simpleTest) {
        final AllureResults results = runWithinTestContext(
                simpleTest::testSomething,
                Allure1TestCaseAspects::setLifecycle,
                Allure1ParametersAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(io.qameta.allure.model.Parameter::getName, io.qameta.allure.model.Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("parameterWithoutName", "testValue1"),
                        tuple("customParameterName", "testValue2")
                );
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
        public String parameterWithoutName;
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
