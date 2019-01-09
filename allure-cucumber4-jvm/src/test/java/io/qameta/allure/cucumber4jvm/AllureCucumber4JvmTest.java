package io.qameta.allure.cucumber4jvm;

import cucumber.runtime.ClassFinder;
import cucumber.runtime.FeatureSupplier;
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
import io.qameta.allure.model.*;
import io.qameta.allure.test.AllureResultsWriterStub;
import io.qameta.allure.util.ResultsUtils;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Thread.currentThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@Epic("CucumberJVM 4.0 integration")
@SuppressWarnings("unchecked")
class AllureCucumber4JvmTest {

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
    void shouldHandleMultipleExamplesPerOutline() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/multi-examples.feature");

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
    void shouldSupportTaggedExamplesBlocks() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/multi-examples.feature", "--tags", "@ExamplesTag2");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .hasSize(1);

        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("tag", "ExamplesTag2")
                );

        assertThat(testResults)
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
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
    void shouldProcessUndefinedSteps() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/undefined.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.SKIPPED);

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 5", Status.PASSED),
                        tuple("When  step is undefined", Status.SKIPPED),
                        tuple("Then  b is 10", Status.SKIPPED)
                );
    }

    @Test
    void shouldProcessPendingExceptionsInSteps() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/pending.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.SKIPPED);

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 5", Status.PASSED),
                        tuple("When  step is yet to be implemented", Status.SKIPPED),
                        tuple("Then  b is 10", Status.SKIPPED)
                );
    }

    @Test
    void shouldSupportDryRunForSimpleFeatures() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature", "--dry-run");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Add a to b", Status.SKIPPED)
                );

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 5", Status.SKIPPED),
                        tuple("And  b is 10", Status.SKIPPED),
                        tuple("When  I add a to b", Status.SKIPPED),
                        tuple("Then  result is 15", Status.SKIPPED)
                );
    }

    @Test
    void shouldSupportDryRunForHooks() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/hooks.feature", "--dry-run");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .startsWith(
                        tuple("Simple scenario with Before and After hooks", Status.SKIPPED)
                );

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("before: HookSteps.beforeHook()", Status.SKIPPED),
                        tuple("Given  a is 7", Status.SKIPPED),
                        tuple("And  b is 8", Status.SKIPPED),
                        tuple("When  I add a to b", Status.SKIPPED),
                        tuple("Then  result is 15", Status.SKIPPED),
                        tuple("after: HookSteps.afterHook()", Status.SKIPPED)
                );
    }

    @Test
    void shouldPersistHistoryIdForScenarios() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/simple.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults.get(0).getHistoryId())
                .isEqualTo("2f9965e6cc23d5c5f9c5268a2ff6f921");
    }

    @Test
    void shouldPersistHistoryIdForExamples() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/examples.feature", "--threads", "2");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getHistoryId)
                .containsExactlyInAnyOrder("16d71185f704fe21cbc98f9eaa3292bb","d64467f9921e0209deae4286f5599f5d");
    }

    private Comparator<TestResult> byHistoryId =
            Comparator.comparing(TestResult::getHistoryId);

    @Test
    void shouldProcessScenariosInParallelMode() throws IOException {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/parallel.feature", "--threads", "3");

        final List<TestResult> testResults = writer.getTestResults();

        assertThat(testResults)
                .hasSize(3);

        List<TestResult> sortedTestResults = testResults.stream().sorted(byHistoryId).collect(Collectors.toList());

        assertThat(sortedTestResults.get(0).getSteps())
                .extracting(StepResult::getName)
                .containsExactly(
                        "Given  a is 1",
                        "And  b is 3",
                        "When  I add a to b",
                        "Then  result is 4"
                );

        assertThat(sortedTestResults.get(1).getSteps())
                .extracting(StepResult::getName)
                .containsExactly(
                        "Given  a is 2",
                        "And  b is 4",
                        "When  I add a to b",
                        "Then  result is 6"
                );

        assertThat(sortedTestResults.get(2).getSteps())
                .extracting(StepResult::getName)
                .containsExactly(
                        "Given  a is 7",
                        "And  b is 8",
                        "When  I add a to b",
                        "Then  result is 15"
                );
    }

    @Test
    void shouldDisplayHooksAsSteps(){
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/hooks.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Simple scenario with Before and After hooks", Status.PASSED),
                        tuple("Simple scenario with Before hook with Exception", Status.FAILED),
                        tuple("Simple scenario with After hook with Exception", Status.FAILED)
                );

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("before: HookSteps.beforeHook()", Status.PASSED),
                        tuple("Given  a is 7", Status.PASSED),
                        tuple("And  b is 8", Status.PASSED),
                        tuple("When  I add a to b", Status.PASSED),
                        tuple("Then  result is 15", Status.PASSED),
                        tuple("after: HookSteps.afterHook()", Status.PASSED)
                );

        assertThat(testResults.get(1).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("before: HookSteps.beforeHookWithException()", Status.FAILED),
                        tuple("Given  a is 7", Status.SKIPPED),
                        tuple("And  b is 8", Status.SKIPPED),
                        tuple("When  I add a to b", Status.SKIPPED),
                        tuple("Then  result is 15", Status.SKIPPED)
                );

        assertThat(testResults.get(2).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Given  a is 7", Status.PASSED),
                        tuple("And  b is 8", Status.PASSED),
                        tuple("When  I add a to b", Status.PASSED),
                        tuple("Then  result is 15", Status.PASSED),
                        tuple("after: HookSteps.afterHookWithException()", Status.FAILED)
                );
    }

    @Test
    void shouldHandleAmbigiousStepsExceptions(){
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        runFeature(writer, "features/ambigious.feature");

        final List<TestResult> testResults = writer.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Simple scenario with ambigious steps", Status.BROKEN)
                );

        assertThat(testResults.get(0).getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("When  ambigious step present", Status.BROKEN),
                        tuple("Then  something bad should happen", Status.SKIPPED)
                );
    }

    private byte runFeature(final AllureResultsWriterStub writer,
                            final String featureResource,
                            final String... moreOptions) {

        final AllureLifecycle lifecycle = new AllureLifecycle(writer);
        final AllureCucumber4Jvm cucumber4Jvm = new AllureCucumber4Jvm(lifecycle);
        final ClassLoader classLoader = currentThread().getContextClassLoader();
        final ResourceLoader resourceLoader = new MultiLoader(classLoader);
        final ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        final List<String> opts = new ArrayList<>(Arrays.asList(
                "--glue", "io.qameta.allure.cucumber4jvm.samples",
                "--plugin", "null_summary"
        ));
        opts.addAll(Arrays.asList(moreOptions));
        final RuntimeOptions options = new RuntimeOptions(opts);
        boolean mt = options.isMultiThreaded();

        FeatureSupplier featureSupplier = () -> {
            try{
                final String gherkin = readResource(featureResource);
                Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
                TokenMatcher matcher = new TokenMatcher();
                GherkinDocument gherkinDocument = parser.parse(gherkin, matcher);
                CucumberFeature feature = new CucumberFeature(gherkinDocument, featureResource, gherkin);

                return Collections.singletonList(feature);
            } catch (IOException e) {
                return Collections.EMPTY_LIST;
            }
        };
        final Runtime runtime = Runtime.builder()
                .withResourceLoader(resourceLoader)
                .withClassFinder(classFinder)
                .withClassLoader(classLoader)
                .withRuntimeOptions(options)
                .withAdditionalPlugins(cucumber4Jvm)
                .withFeatureSupplier(featureSupplier)
                .build();

        runtime.run();
        return runtime.exitStatus();
    }

    private String readResource(final String resourceName) throws IOException {
        try (InputStream is = currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
