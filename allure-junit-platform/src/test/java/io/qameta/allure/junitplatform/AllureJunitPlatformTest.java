package io.qameta.allure.junitplatform;

import io.github.glytching.junit.extension.system.SystemProperty;
import io.github.glytching.junit.extension.system.SystemPropertyExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.junitplatform.features.BrokenTests;
import io.qameta.allure.junitplatform.features.DescriptionJavadocTest;
import io.qameta.allure.junitplatform.features.DisabledRepeatedTests;
import io.qameta.allure.junitplatform.features.DisabledTests;
import io.qameta.allure.junitplatform.features.DynamicTests;
import io.qameta.allure.junitplatform.features.FailedTests;
import io.qameta.allure.junitplatform.features.OneTest;
import io.qameta.allure.junitplatform.features.OwnerTest;
import io.qameta.allure.junitplatform.features.ParallelTests;
import io.qameta.allure.junitplatform.features.ParameterisedTests;
import io.qameta.allure.junitplatform.features.PassedTests;
import io.qameta.allure.junitplatform.features.RepeatedTests;
import io.qameta.allure.junitplatform.features.SeverityTest;
import io.qameta.allure.junitplatform.features.SkippedTests;
import io.qameta.allure.junitplatform.features.TaggedTests;
import io.qameta.allure.junitplatform.features.TestClassDisabled;
import io.qameta.allure.junitplatform.features.TestClassWithDisplayNameAnnotation;
import io.qameta.allure.junitplatform.features.TestClassWithoutDisplayNameAnnotation;
import io.qameta.allure.junitplatform.features.TestWithClassLabels;
import io.qameta.allure.junitplatform.features.TestWithClassLinks;
import io.qameta.allure.junitplatform.features.TestWithDescription;
import io.qameta.allure.junitplatform.features.TestWithDisplayName;
import io.qameta.allure.junitplatform.features.TestWithMethodLabels;
import io.qameta.allure.junitplatform.features.TestWithMethodLinks;
import io.qameta.allure.junitplatform.features.TestWithSteps;
import io.qameta.allure.junitplatform.features.TestWithSystemErr;
import io.qameta.allure.junitplatform.features.TestWithSystemOut;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.junitplatform.features.TaggedTests.CLASS_TAG;
import static io.qameta.allure.junitplatform.features.TaggedTests.METHOD_TAG;
import static io.qameta.allure.test.AllurePredicates.hasLabel;
import static io.qameta.allure.test.AllurePredicates.hasStatus;
import static io.qameta.allure.util.ResultsUtils.FEATURE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.HOST_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.OWNER_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.PACKAGE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.SEVERITY_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.SUITE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TAG_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TEST_CLASS_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.TEST_METHOD_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.THREAD_LABEL_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unchecked")
@ExtendWith(SystemPropertyExtension.class)
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
                .contains(HOST_LABEL_NAME, THREAD_LABEL_NAME);
    }

    @Test
    void shouldProcessPassedTests() {
        runClasses(PassedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
                .filteredOn(hasStatus(Status.PASSED))
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
        runClasses(TestWithDisplayName.class);

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
                .filteredOn(hasStatus(Status.PASSED))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("testA", "testB", "testC");
    }

    @Test
    void shouldProcessParametrisedTests() {
        runClasses(ParameterisedTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2)
                .filteredOn(hasStatus(Status.PASSED))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("[1] Hello", "[2] World");
    }

    @Test
    void shouldAddSteps() {
        runClasses(TestWithSteps.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult.getSteps())
                .hasSize(3)
                .flatExtracting(StepResult::getName)
                .containsExactly("first", "second", "third");

    }

    @Test
    void shouldAddTags() {
        runClasses(TaggedTests.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(TAG_LABEL_NAME, CLASS_TAG),
                        tuple(TAG_LABEL_NAME, METHOD_TAG)
                );
    }

    @Test
    void shouldProcessDefaultTestClassDisplayName() {
        runClasses(TestClassWithoutDisplayNameAnnotation.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", SUITE_LABEL_NAME)
                .extracting(Label::getValue)
                .contains("io.qameta.allure.junitplatform.features.TestClassWithoutDisplayNameAnnotation");
    }

    @Test
    void shouldProcessJunit5Description() {
        runClasses(TestWithDescription.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .flatExtracting(TestResult::getDescription)
                .contains("Test description");
    }

    @Test
    void shouldProcessDisabledTests() {
        runClasses(DisabledTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactly(Status.SKIPPED);
    }

    @Test
    void shouldProcessMethodLabels() {
        runClasses(TestWithMethodLabels.class);
        final List<TestResult> testResults = results.getTestResults();
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
    void shouldProcessClassLabels() {
        runClasses(TestWithClassLabels.class);
        final List<TestResult> testResults = results.getTestResults();
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

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @SystemProperty(name = "allure.link.custom.pattern", value = "https://example.org/custom/{}")
    @Test
    void shouldProcessMethodLinks() {
        runClasses(TestWithMethodLinks.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType, Link::getUrl)
                .contains(
                        tuple("LINK-1", "custom", "https://example.org/custom/LINK-1"),
                        tuple("LINK-2", "custom", "https://example.org/link/2"),
                        tuple("", "custom", "https://example.org/some-custom-link"),
                        tuple("ISSUE-1", "issue", "https://example.org/issue/ISSUE-1"),
                        tuple("ISSUE-2", "issue", "https://example.org/issue/ISSUE-2"),
                        tuple("ISSUE-3", "issue", "https://example.org/issue/ISSUE-3"),
                        tuple("TMS-1", "tms", "https://example.org/tms/TMS-1"),
                        tuple("TMS-2", "tms", "https://example.org/tms/TMS-2"),
                        tuple("TMS-3", "tms", "https://example.org/tms/TMS-3")
                );
    }

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @SystemProperty(name = "allure.link.custom.pattern", value = "https://example.org/custom/{}")
    @Test
    void shouldProcessClassLinks() {
        runClasses(TestWithClassLinks.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType, Link::getUrl)
                .contains(
                        tuple("LINK-1", "custom", "https://example.org/custom/LINK-1"),
                        tuple("LINK-2", "custom", "https://example.org/link/2"),
                        tuple("", "custom", "https://example.org/some-custom-link"),
                        tuple("ISSUE-1", "issue", "https://example.org/issue/ISSUE-1"),
                        tuple("ISSUE-2", "issue", "https://example.org/issue/ISSUE-2"),
                        tuple("ISSUE-3", "issue", "https://example.org/issue/ISSUE-3"),
                        tuple("TMS-1", "tms", "https://example.org/tms/TMS-1"),
                        tuple("TMS-2", "tms", "https://example.org/tms/TMS-2"),
                        tuple("TMS-3", "tms", "https://example.org/tms/TMS-3")
                );
    }

    @Issue("189")
    @Test
    void shouldProcessDynamicTestLabels() {
        runClasses(DynamicTests.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(3)
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
    void shouldCommonLabels() {
        runClasses(OneTest.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(PACKAGE_LABEL_NAME, "io.qameta.allure.junitplatform.features.OneTest"),
                        tuple(SUITE_LABEL_NAME, "io.qameta.allure.junitplatform.features.OneTest"),
                        tuple(TEST_CLASS_LABEL_NAME, "io.qameta.allure.junitplatform.features.OneTest"),
                        tuple(TEST_METHOD_LABEL_NAME, "single")
                );
    }

    @Issue("180")
    @Test
    void shouldSetSuiteNameFromDisplayNameAnnotation() {
        runClasses(TestClassWithDisplayNameAnnotation.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(SUITE_LABEL_NAME, "Display name of test class")
                );
    }

    @Test
    void shouldSetSeverity() {
        runClasses(SeverityTest.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .filteredOn("name", "criticalSeverityTest()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", SEVERITY_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactly("critical");

        assertThat(testResults)
                .filteredOn("name", "defaultSeverityTest()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", SEVERITY_LABEL_NAME)
                .extracting(Label::getValue)
                .contains("trivial");
    }

    @Test
    void shouldSetOwner() {
        runClasses(OwnerTest.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .filteredOn("name", "secondOwnerTest()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", OWNER_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactly("second");

        assertThat(testResults)
                .filteredOn("name", "defaultOwnerTest()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", OWNER_LABEL_NAME)
                .extracting(Label::getValue)
                .contains("first");
    }

    @Disabled("Fails when run using IDEA")
    @Test
    void shouldSetJavadocDescription() {
        runClasses(DescriptionJavadocTest.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getDescriptionHtml)
                .contains(" Test javadoc description.\n");
    }

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @SystemProperty(name = "junit.platform.output.capture.stdout", value = "true")
    @Test
    void shouldCaptureSystemOut() {
        runClasses(TestWithSystemOut.class);

        final List<Attachment> attachments = results.getTestResults().stream()
                .map(TestResult::getAttachments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .contains(
                        tuple("Stdout", "text/plain")
                );

        final Attachment found = attachments.stream()
                .filter(attachment -> "Stdout".equals(attachment.getName()))
                .findAny()
                .get();


        final Map<String, byte[]> attachmentFiles = results.getAttachments();
        assertThat(attachmentFiles)
                .containsKeys(found.getSource());

        final byte[] bytes = attachmentFiles.get(found.getSource());
        final String attachmentContent = new String(bytes, StandardCharsets.UTF_8);

        assertThat(attachmentContent)
                .isEqualTo("SYS OUT CONTENT\n");

    }

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @SystemProperty(name = "junit.platform.output.capture.stderr", value = "true")
    @Test
    void shouldCaptureSystemErr() {
        runClasses(TestWithSystemErr.class);

        final List<Attachment> attachments = results.getTestResults().stream()
                .map(TestResult::getAttachments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .contains(
                        tuple("Stderr", "text/plain")
                );

        final Attachment found = attachments.stream()
                .filter(attachment -> "Stderr".equals(attachment.getName()))
                .findAny()
                .get();


        final Map<String, byte[]> attachmentFiles = results.getAttachments();
        assertThat(attachmentFiles)
                .containsKeys(found.getSource());

        final byte[] bytes = attachmentFiles.get(found.getSource());
        final String attachmentContent = new String(bytes, StandardCharsets.UTF_8);

        assertThat(attachmentContent)
                .isEqualTo("SYS ERR CONTENT\n");

    }

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SystemProperty(name = "junit.jupiter.execution.parallel.enabled", value = "true")
    @Test
    void shouldRunTestsInParallel() {
        runClasses(ParallelTests.class);

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "first()",
                        "second()"
                );
    }

    @Test
    void shouldSupportDisabledTestClasses() {
        runClasses(TestClassDisabled.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "First",
                        "second()",
                        "third()"
                );

        assertThat(testResults)
                .filteredOn("name", "second()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", FEATURE_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("A");

        assertThat(testResults)
                .filteredOn("name", "third()")
                .flatExtracting(TestResult::getLabels)
                .filteredOn("name", OWNER_LABEL_NAME)
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("me");
    }

    @Test
    void shouldSupportRepeatedTests() {
        runClasses(RepeatedTests.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(5)
                .allMatch(hasStatus(Status.PASSED))
                .allMatch(hasLabel(OWNER_LABEL_NAME, "me"));

    }

    @Test
    void shouldSupportDisabledRepeatedTests() {
        runClasses(DisabledRepeatedTests.class);
        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1)
                .allMatch(hasStatus(Status.SKIPPED))
                .allMatch(hasLabel(OWNER_LABEL_NAME, "other guy"));

    }

    @Step("Run classes {classes}")
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
