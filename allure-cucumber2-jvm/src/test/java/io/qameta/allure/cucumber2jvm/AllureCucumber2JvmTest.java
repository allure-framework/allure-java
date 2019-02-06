package io.qameta.allure.cucumber2jvm;

import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
import gherkin.AstBuilder;
import gherkin.Parser;
import gherkin.TokenMatcher;
import gherkin.ast.GherkinDocument;
import io.github.glytching.junit.extension.system.SystemProperty;
import io.github.glytching.junit.extension.system.SystemPropertyExtension;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Issue;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Thread.currentThread;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unchecked")
class AllureCucumber2JvmTest {

    @AllureFeatures.Base
    @Test
    void shouldSetName() {
        final AllureResults results = runFeature("features/simple.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Add a to b");

    }

    @AllureFeatures.PassedTests
    @Test
    void shouldSetStatus() {
        final AllureResults results = runFeature("features/simple.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.PASSED);
    }

    @AllureFeatures.FailedTests
    @Test
    void shouldSetFailedStatus() {
        final AllureResults results = runFeature("features/failed.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED);
    }

    @AllureFeatures.FailedTests
    @Test
    void shouldSetStatusDetails() {
        final AllureResults results = runFeature("features/failed.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactlyInAnyOrder("expected: <15> but was: <123>");
    }

    @AllureFeatures.BrokenTests
    @Test
    void shouldSetBrokenStatus() {
        final AllureResults results = runFeature("features/broken.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.BROKEN);
    }

    @AllureFeatures.Stages
    @Test
    void shouldSetStage() {
        final AllureResults results = runFeature("features/simple.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStage)
                .containsExactlyInAnyOrder(Stage.FINISHED);
    }

    @AllureFeatures.Timeline
    @Test
    void shouldSetStart() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runFeature("features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.Timings
    @Test
    void shouldSetStop() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runFeature("features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.FullName
    @Test
    void shouldSetFullName() {
        final AllureResults results = runFeature("features/simple.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getFullName)
                .containsExactlyInAnyOrder("Simple feature: Add a to b");
    }

    @AllureFeatures.Descriptions
    @Test
    void shouldSetDescription() {
        final AllureResults results = runFeature("features/description.feature");

        final String expected = "This is description for current feature.\n"
                + "It should appear on each scenario in report";

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getDescription)
                .containsExactlyInAnyOrder(
                        expected,
                        expected
                );
    }

    @AllureFeatures.Attachments
    @Test
    void shouldAddDataTableAttachment() {
        final AllureResults results = runFeature("features/datatable.feature");

        final List<Attachment> attachments = results.getTestResults().stream()
                .map(TestResult::getSteps)
                .flatMap(Collection::stream)
                .map(StepResult::getAttachments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(attachments)
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactlyInAnyOrder(
                        tuple("Data table", "text/tab-separated-values")
                );

        final Attachment dataTableAttachment = attachments.iterator().next();
        final Map<String, byte[]> attachmentFiles = results.getAttachments();
        assertThat(attachmentFiles)
                .containsKeys(dataTableAttachment.getSource());

        final byte[] bytes = attachmentFiles.get(dataTableAttachment.getSource());
        final String attachmentContent = new String(bytes, StandardCharsets.UTF_8);

        assertThat(attachmentContent)
                .isEqualTo("name\tlogin\temail\n" +
                        "Viktor\tclicman\tclicman@ya.ru\n" +
                        "Viktor2\tclicman2\tclicman2@ya.ru\n"
                );

    }

    @AllureFeatures.Steps
    @Test
    void shouldAddBackgroundSteps() {
        final AllureResults results = runFeature("features/background.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "Before",
                        "Given  cat is sad",
                        "And  cat is murmur",
                        "When  Pet the cat",
                        "Then  Cat is happy",
                        "After"
                );
    }

    @AllureFeatures.Parameters
    @Test
    void shouldAddParametersFromExamples() {
        final AllureResults results = runFeature("features/examples.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .hasSize(2);

        assertThat(testResults)
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("a", "1"), tuple("b", "3"), tuple("result", "4"),
                        tuple("a", "2"), tuple("b", "4"), tuple("result", "6")
                );

    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldAddTags() {
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("tag", "FeatureTag"),
                        tuple("tag", "good")
                );
    }

    @AllureFeatures.Links
    @ExtendWith(SystemPropertyExtension.class)
    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @Test
    void shouldAddLinks() {
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType, Link::getUrl)
                .contains(
                        tuple("OAT-4444", "tms", "https://example.org/tms/OAT-4444"),
                        tuple("BUG-22400", "issue", "https://example.org/issue/BUG-22400")
                );
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldAddBddLabels() {
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("feature", "Test Simple Scenarios"),
                        tuple("story", "Add a to b")
                );
    }

    @AllureFeatures.Timeline
    @Test
    void shouldThreadHostLabels() {
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .contains("host", "thread");
    }

    @AllureFeatures.MarkerAnnotations
    @Test
    void shouldCommonLabels() {
        final AllureResults results = runFeature("features/tags.feature");

        final List<TestResult> testResults = results.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("package", "Test Simple Scenarios"),
                        tuple("suite", "Test Simple Scenarios"),
                        tuple("testClass", "Add a to b")
                );
    }

    @AllureFeatures.NotImplementedTests
    @Test
    void shouldProcessNotImplementedScenario() {
        final AllureResults results = runFeature("features/undefined.feature");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder((Status) null);
    }

    @AllureFeatures.Base
    @Test
    void shouldSupportDryRun() {
        final AllureResults results = runFeature("features/simple.feature", "--dry-run");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Add a to b", Status.SKIPPED)
                );
    }

    @AllureFeatures.Base
    @Issue("173")
    @Issue("164")
    @Test
    void shouldUseUuid() {
        final AllureResults results = runFeature("features/simple.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getUuid)
                .allMatch(uuid -> nonNull(uuid) && uuid.matches("[\\-a-z0-9]+"), "UUID");
    }

    private AllureResults runFeature(final String featureResource,
                                     final String... moreOptions) {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writer);
        final AllureCucumber2Jvm cucumber2Jvm = new AllureCucumber2Jvm(lifecycle);
        final ClassLoader classLoader = currentThread().getContextClassLoader();
        final ResourceLoader resourceLoader = new MultiLoader(classLoader);
        final ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        final List<String> opts = new ArrayList<>(Arrays.asList(
                "--glue", "io.qameta.allure.cucumber2jvm.samples",
                "--plugin", "null"
        ));
        opts.addAll(Arrays.asList(moreOptions));
        final RuntimeOptions options = new RuntimeOptions(opts);
        final Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, options);

        options.addPlugin(cucumber2Jvm);

        final String gherkin = readResource(featureResource);
        Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        TokenMatcher matcher = new TokenMatcher();
        GherkinDocument gherkinDocument = parser.parse(gherkin, matcher);
        CucumberFeature feature = new CucumberFeature(gherkinDocument, featureResource, gherkin);

        feature.sendTestSourceRead(runtime.getEventBus());
        runtime.runFeature(feature);
        return writer;
    }

    private String readResource(final String resourceName) {
        try (InputStream is = currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Feature file not found " + resourceName);
        }
    }
}
