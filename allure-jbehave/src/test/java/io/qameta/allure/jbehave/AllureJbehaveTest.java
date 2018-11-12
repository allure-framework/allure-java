package io.qameta.allure.jbehave;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.jbehave.samples.BrokenStorySteps;
import io.qameta.allure.jbehave.samples.SimpleStorySteps;
import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.embedder.EmbedderControls;
import org.jbehave.core.embedder.NullEmbedderMonitor;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.reporters.NullStoryReporter;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@Epic("JBehave integration")
@ExtendWith(TempDirectory.class)
class AllureJbehaveTest {

    private final Path temp;

    public AllureJbehaveTest(@TempDirectory.TempDir final Path temp) {
        this.temp = temp;
    }

    @Test
    void shouldSetName() {
        final AllureResults results = runStory("stories/simple.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Add a to b");

    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldAddNotPerformedSteps() {
        final AllureResults results = runStory("stories/long.story");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("Given a is 5", Status.PASSED),
                        tuple("And b is 10", Status.PASSED),
                        tuple("When I add a to b", Status.PASSED),
                        tuple("Then result is 15", Status.PASSED),
                        tuple("Then result is 15", Status.PASSED),
                        tuple("When I add a to b", Status.PASSED),
                        tuple("Then result is 20", Status.FAILED),
                        tuple("Then result is ｟21｠", null),
                        tuple("Then result is ｟22｠", null),
                        tuple("Then result is ｟23｠", null),
                        tuple("When I add a to b", null),
                        tuple("Then result is ｟25｠", null)
                );

    }

    @Test
    void shouldSetStatus() {
        final AllureResults results = runStory("stories/simple.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.PASSED);
    }


    @Test
    void shouldSetFailedStatus() {
        final AllureResults results = runStory("stories/failed.story");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED);
    }

    @Test
    void shouldSetStatusDetails() {
        final AllureResults results = runStory("stories/failed.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactlyInAnyOrder("expected: <15> but was: <123>");
    }

    @Test
    void shouldSetBrokenStatus() {
        final AllureResults results = runStory("stories/broken.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.BROKEN);
    }

    @Test
    void shouldSetStage() {
        final AllureResults results = runStory("stories/simple.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStage)
                .containsExactlyInAnyOrder(Stage.FINISHED);
    }

    @Test
    void shouldSetStart() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runStory("stories/simple.story");
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetStop() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runStory("stories/simple.story");
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetFullName() {
        final AllureResults results = runStory("stories/simple.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getFullName)
                .containsExactlyInAnyOrder("simple.story: Add a to b");
    }

    @Test
    void shouldSetDescription() {
        final AllureResults results = runStory("stories/description.story");

        final String expected = "This is description for current story.\n"
                + "It should appear on each scenario in report";

        assertThat(results.getTestResults())
                .extracting(TestResult::getDescription)
                .containsExactlyInAnyOrder(
                        expected,
                        expected
                );
    }

    @Issue("238")
    @Test
    void shouldNotFailOnComments() {
        final AllureResults results = runStory("stories/comment.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Add a to b", Status.PASSED)
                );

    }

    @Test
    void shouldProcessNotImplementedScenario() {
        final AllureResults results = runStory("stories/undefined.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Step is not implemented", null)
                );
    }

    @Issue("145")
    @SuppressWarnings("unchecked")
    @Test
    void shouldAddParametersFromExamples() {
        final AllureResults results = runStory("stories/examples.story");

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

    @Test
    void shouldRunMultiplyScenarios() {
        final AllureResults results = runStory("stories/multiply.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("First", Status.PASSED),
                        tuple("Second", Status.PASSED),
                        tuple("Third", Status.PASSED)
                );

    }

    private AllureResults runStory(final String storyResource) {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writer);

        final Embedder embedder = new Embedder();
        embedder.useEmbedderMonitor(new NullEmbedderMonitor());
        embedder.useEmbedderControls(new EmbedderControls()
                .doGenerateViewAfterStories(false)
                .doFailOnStoryTimeout(false)
                .doBatch(false)
                .doIgnoreFailureInStories(true)
                .doIgnoreFailureInView(true)
                .doVerboseFailures(false)
                .doVerboseFiltering(false)
        );
        final AllureJbehave allureJbehave = new AllureJbehave(lifecycle);
        embedder.useConfiguration(new MostUsefulConfiguration()
                .useStoryLoader(new LoadFromClasspath(this.getClass()))
                .useStoryReporterBuilder(new ReportlessStoryReporterBuilder(temp.toFile())
                        .withReporters(allureJbehave)
                )
                .useDefaultStoryReporter(new NullStoryReporter())
        );
        final InjectableStepsFactory stepsFactory = new InstanceStepsFactory(
                embedder.configuration(),
                new SimpleStorySteps(),
                new BrokenStorySteps()
        );
        embedder.useCandidateSteps(stepsFactory.createCandidateSteps());
        final AllureLifecycle cached = Allure.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            embedder.runStoriesAsPaths(singletonList(storyResource));
        } finally {
            Allure.setLifecycle(cached);
        }
        return writer;
    }

    static class ReportlessStoryReporterBuilder extends StoryReporterBuilder {

        private final File outputDirectory;

        ReportlessStoryReporterBuilder(final File outputDirectory) {
            this.outputDirectory = outputDirectory;
        }

        @Override
        public File outputDirectory() {
            return outputDirectory;
        }
    }
}