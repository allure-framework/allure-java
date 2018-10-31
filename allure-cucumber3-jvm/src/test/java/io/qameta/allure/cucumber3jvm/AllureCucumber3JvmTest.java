package io.qameta.allure.cucumber3jvm;

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
import io.qameta.allure.Epic;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Thread.currentThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@Epic("CucumberJVM 3.0 integration")
@SuppressWarnings("unchecked")
class AllureCucumber3JvmTest {

    @Test
    void shouldSetName() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Add a to b");

    }

    @Test
    void shouldSetStatus() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.PASSED);
    }

    @Test
    void shouldSetFailedStatus() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/failed.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED);
    }

    @Test
    void shouldSetStatusDetails() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/failed.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactlyInAnyOrder("expected [123] but found [15]");
    }

    @Test
    void shouldSetBrokenStatus() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/broken.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.BROKEN);
    }

    @Test
    void shouldSetStage() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStage)
                .containsExactlyInAnyOrder(Stage.FINISHED);
    }

    @Test
    void shouldSetStart() throws IOException {
        final long before = Instant.now().toEpochMilli();
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetStop() throws IOException {
        final long before = Instant.now().toEpochMilli();
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");
        final long after = Instant.now().toEpochMilli();

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetFullName() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getFullName)
                .containsExactlyInAnyOrder("Simple feature: Add a to b");
    }

    @Test
    void shouldSetDescription() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/description.feature");

        final String expected = "This is description for current feature.\n"
                + "It should appear on each scenario in report";

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getDescription)
                .containsExactlyInAnyOrder(
                        expected,
                        expected
                );
    }

    @Test
    void shouldAddDataTableAttachment() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/datatable.feature");

        final List<Attachment> attachments = writer.getTestResults().stream()
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
        final Map<String, byte[]> attachmentFiles = writer.getAttachments();
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

    @Test
    void shouldAddBackgroundSteps() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/background.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "Given  cat is sad",
                        "And  cat is murmur",
                        "When  Pet the cat",
                        "Then  Cat is happy"
                );
    }

    @Test
    void shouldAddParametersFromExamples() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/examples.feature");

        final List<TestResult> testResults = writer.getTestResults();

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

    @Test
    void shouldAddTags() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("tag", "FeatureTag"),
                        tuple("tag", "good")
                );
    }

    @ExtendWith(SystemPropertyExtension.class)
    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @Test
    void shouldAddLinks() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType, Link::getUrl)
                .contains(
                        tuple("OAT-4444", "tms", "https://example.org/tms/OAT-4444"),
                        tuple("BUG-22400", "issue", "https://example.org/issue/BUG-22400")
                );
    }

    @Test
    void shouldAddBddLabels() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("feature", "Test Simple Scenarios"),
                        tuple("story", "Add a to b")
                );
    }

    @Test
    void shouldThreadHostLabels() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .contains("host", "thread");
    }

    @Test
    void shouldCommonLabels() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/tags.feature");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("package", "Test Simple Scenarios"),
                        tuple("suite", "Test Simple Scenarios"),
                        tuple("testClass", "Add a to b")
                );
    }

    @Test
    void shouldProcessNotImplementedScenario() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/undefined.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder((Status) null);
    }

    private void runFeature(final AllureResultsWriterStub writer,
                            final String featureResource) throws IOException {

        final AllureLifecycle lifecycle = new AllureLifecycle(writer);
        final AllureCucumber3Jvm cucumber3Jvm = new AllureCucumber3Jvm(lifecycle);
        final ClassLoader classLoader = currentThread().getContextClassLoader();
        final ResourceLoader resourceLoader = new MultiLoader(classLoader);
        final ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        final RuntimeOptions options = new RuntimeOptions(Arrays.asList(
                "--glue", "io.qameta.allure.cucumber3jvm.samples",
                "--plugin", "null"
        ));
        final Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, options);

        options.addPlugin(cucumber3Jvm);
        options.noSummaryPrinter();

        final String gherkin = readResource(featureResource);
        Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        TokenMatcher matcher = new TokenMatcher();
        GherkinDocument gherkinDocument = parser.parse(gherkin, matcher);
        CucumberFeature feature = new CucumberFeature(gherkinDocument, featureResource, gherkin);

        feature.sendTestSourceRead(runtime.getEventBus());
        runtime.runFeature(feature);
    }

    private String readResource(final String resourceName) throws IOException {
        try (InputStream is = currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
