/*
 *  Copyright 2016-2026 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.jbehave5;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.jbehave5.samples.BrokenAfterScenarioSteps;
import io.qameta.allure.jbehave5.samples.BrokenAfterStoriesSteps;
import io.qameta.allure.jbehave5.samples.BrokenAfterStorySteps;
import io.qameta.allure.jbehave5.samples.BrokenBeforeGivenStorySteps;
import io.qameta.allure.jbehave5.samples.BrokenBeforeScenarioSteps;
import io.qameta.allure.jbehave5.samples.BrokenBeforeStoriesSteps;
import io.qameta.allure.jbehave5.samples.BrokenBeforeStorySteps;
import io.qameta.allure.jbehave5.samples.BrokenLifecycleStorySteps;
import io.qameta.allure.jbehave5.samples.BrokenStorySteps;
import io.qameta.allure.jbehave5.samples.RuntimeApiSteps;
import io.qameta.allure.jbehave5.samples.SimpleStorySteps;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.GlobalError;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
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
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.qameta.allure.Allure.step;
import static io.qameta.allure.util.ResultsUtils.md5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IsolatedLifecycle
class AllureJbehave5Test {

    @TempDir
    Path temp;

    @Test
    void shouldSetName() {
        final AllureResults results = runStories("stories/simple.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Add a to b");

    }

    @Test
    void shouldAddNotPerformedSteps() {
        final AllureResults results = runStories("stories/long.story");

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
                        tuple("Then result is 21", null),
                        tuple("Then result is 22", null),
                        tuple("Then result is 23", null),
                        tuple("When I add a to b", null),
                        tuple("Then result is 25", null)
                );

    }

    @Test
    void shouldSetStatus() {
        final AllureResults results = runStories("stories/simple.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.PASSED);
    }

    @Test
    void shouldSetFailedStatus() {
        final AllureResults results = runStories("stories/failed.story");

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED);
    }

    @Test
    void shouldSetStatusDetails() {
        final AllureResults results = runStories("stories/failed.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactlyInAnyOrder("\nexpected: 123\n but was: 15");
    }

    @Test
    void shouldSetBrokenStatus() {
        final AllureResults results = runStories("stories/broken.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactlyInAnyOrder(Status.BROKEN);
    }

    /**
     * A failed {@code @BeforeStories} method belongs to the entire JBehave run and is preserved as a global error.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldReportBrokenBeforeStoriesAsGlobalError() {
        final AllureResults results = runStoriesWithSteps(
                new BrokenBeforeStoriesSteps(),
                "stories/simple.story"
        );

        assertThat(getGlobalErrors(results))
                .extracting(GlobalError::getMessage)
                .singleElement(InstanceOfAssertFactories.STRING)
                .startsWith("JBehave before-stories lifecycle step failed:")
                .contains("Exception in @BeforeStories");
    }

    /**
     * A failed {@code @AfterStories} method is preserved as a global error after completed scenarios keep their
     * results.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldReportBrokenAfterStoriesAsGlobalError() {
        final AllureResults results = runStoriesWithSteps(
                new BrokenAfterStoriesSteps(),
                "stories/simple.story"
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Add a to b", Status.PASSED));
        assertThat(getGlobalErrors(results))
                .extracting(GlobalError::getMessage)
                .singleElement(InstanceOfAssertFactories.STRING)
                .startsWith("JBehave after-stories lifecycle step failed:")
                .contains("Exception in @AfterStories");
    }

    /**
     * A failed {@code @BeforeStory} method has no scenario owner and is preserved as a story-scoped global error.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldReportBrokenBeforeStoryAsGlobalError() {
        final AllureResults results = runStoriesWithSteps(
                new BrokenBeforeStorySteps(),
                "stories/simple.story"
        );

        assertThat(getGlobalErrors(results))
                .extracting(GlobalError::getMessage)
                .singleElement(InstanceOfAssertFactories.STRING)
                .startsWith("JBehave before-story lifecycle step failed for simple.story:")
                .contains("Exception in @BeforeStory");
    }

    /**
     * A failed {@code @AfterStory} method has no scenario owner and is preserved as a story-scoped global error
     * without replacing the completed scenario result.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldReportBrokenAfterStoryAsGlobalError() {
        final AllureResults results = runStoriesWithSteps(
                new BrokenAfterStorySteps(),
                "stories/simple.story"
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Add a to b", Status.PASSED));
        assertThat(getGlobalErrors(results))
                .extracting(GlobalError::getMessage)
                .singleElement(InstanceOfAssertFactories.STRING)
                .startsWith("JBehave after-story lifecycle step failed for simple.story:")
                .contains("Exception in @AfterStory");
    }

    /**
     * Declarative story setup uses the same global-error path as an annotation-based before-story method.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldReportBrokenDeclarativeBeforeStoryAsGlobalError() {
        final AllureResults results = runStoriesWithSteps(
                new BrokenLifecycleStorySteps(),
                "stories/broken-before-story-lifecycle.story"
        );

        assertThat(getGlobalErrors(results))
                .extracting(GlobalError::getMessage)
                .containsExactly(
                        "JBehave before-story lifecycle step failed for broken-before-story-lifecycle.story: "
                                + "Exception in declarative before-story lifecycle"
                );
    }

    /**
     * Declarative story teardown uses the same global-error path as an annotation-based after-story method.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldReportBrokenDeclarativeAfterStoryAsGlobalError() {
        final AllureResults results = runStoriesWithSteps(
                new BrokenLifecycleStorySteps(),
                "stories/broken-after-story-lifecycle.story"
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Add a to b", Status.PASSED));
        assertThat(getGlobalErrors(results))
                .extracting(GlobalError::getMessage)
                .containsExactly(
                        "JBehave after-story lifecycle step failed for broken-after-story-lifecycle.story: "
                                + "Exception in declarative after-story lifecycle"
                );
    }

    /**
     * A failed {@code @BeforeScenario} method remains owned by its scenario and is not duplicated globally.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldKeepBrokenBeforeScenarioOnScenario() {
        final AllureResults results = runStoriesWithSteps(
                new BrokenBeforeScenarioSteps(),
                "stories/simple.story"
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Add a to b", Status.BROKEN));
        assertThat(getGlobalErrors(results)).isEmpty();
    }

    /**
     * A failed {@code @AfterScenario} method remains owned by its scenario and is not duplicated globally.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldKeepBrokenAfterScenarioOnScenario() {
        final AllureResults results = runStoriesWithSteps(
                new BrokenAfterScenarioSteps(),
                "stories/simple.story"
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Add a to b", Status.BROKEN));
        assertThat(getGlobalErrors(results)).isEmpty();
    }

    /**
     * A story hook around scenario-level {@code GivenStories} executes inside the parent scenario and remains owned
     * by that scenario instead of becoming a story-scoped global error.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldKeepBrokenBeforeGivenStoryOnParentScenario() {
        final AllureResults results = runStoriesWithSteps(
                new BrokenBeforeGivenStorySteps(),
                "stories/given.story"
        );

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Add a to b", Status.BROKEN));
        assertThat(getGlobalErrors(results)).isEmpty();
    }

    /**
     * A failed step in story-level {@code GivenStories} has no scenario owner and is preserved as a global error.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldReportBrokenStoryLevelGivenStoryAsGlobalError() {
        final AllureResults results = runStories("stories/story-level-broken-given.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Story with broken GivenStories", Status.PASSED));
        assertThat(getGlobalErrors(results))
                .extracting(GlobalError::getMessage)
                .containsExactly(
                        "JBehave story-level GivenStories step failed for broken.story: Oops"
                );
    }

    /**
     * A failed step in scenario-level {@code GivenStories} remains owned by its parent scenario.
     */
    @Test
    @AllureFeatures.Fixtures
    @AllureFeatures.BrokenTests
    @Description
    void shouldKeepBrokenScenarioLevelGivenStoryOnParentScenario() {
        final AllureResults results = runStories("stories/scenario-level-broken-given.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Scenario with broken GivenStories", Status.BROKEN));
        assertThat(getGlobalErrors(results)).isEmpty();
    }

    @Test
    void shouldSetStage() {
        final AllureResults results = runStories("stories/simple.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getStage)
                .containsExactlyInAnyOrder(Stage.FINISHED);
    }

    @Test
    void shouldSetStart() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runStories("stories/simple.story");
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetStop() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runStories("stories/simple.story");
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetFullName() {
        final AllureResults results = runStories("stories/simple.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getFullName)
                .containsExactlyInAnyOrder("simple.story: Add a to b");
        assertThat(results.getTestResults().get(0).getTitlePath())
                .containsExactly("stories", "simple.story");
    }

    @Test
    void shouldSetDescription() {
        final AllureResults results = runStories("stories/description.story");

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
        final AllureResults results = runStories("stories/comment.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Add a to b", Status.PASSED)
                );

    }

    @Test
    void shouldProcessNotImplementedScenario() {
        final AllureResults results = runStories("stories/undefined.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("Step is not implemented", null)
                );
    }

    @Issue("145")
    @Test
    void shouldAddParametersFromExamples() {
        final AllureResults results = runStories("stories/examples.story");

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
        final AllureResults results = runStories("stories/multiply.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("First", Status.PASSED),
                        tuple("Second", Status.PASSED),
                        tuple("Third", Status.PASSED)
                );

    }

    @Issue("163")
    @Test
    void shouldNotFailIfGivenStoriesSpecified() {
        final AllureResults results = runStories("stories/given.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Add a to b", Status.PASSED));

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "Given a is 5",
                        "Given b is 10",
                        "When I add a to b",
                        "Then result is 15"
                );

    }

    @Test
    void shouldSupportRuntimeApiInSteps() {
        final AllureResults results = runStories("stories/runtimeapi.story");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactly(tuple("Runtime API", Status.PASSED));

        assertThat(results.getTestResults())
                .filteredOn("name", "Runtime API")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("jbehave-test-label", "some-value")
                );

        assertThat(results.getTestResults())
                .filteredOn("name", "Runtime API")
                .flatExtracting(TestResult::getSteps)
                .filteredOn("name", "Given runtime api")
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactlyInAnyOrder("sub step 1", "sub step 2", "some attachment");

        assertThat(results.getTestResults())
                .filteredOn("name", "Runtime API")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded)
                .containsExactlyInAnyOrder(
                        tuple("test param", "param value", null),
                        tuple("excluded param", "excluded value", true)
                );

        final TestResult testResult = results.getTestResults().get(0);
        final String fullName = "runtimeapi.story: Runtime API";
        assertThat(testResult.getTestCaseId())
                .isEqualTo(md5(fullName));
        assertThat(testResult.getHistoryId())
                .isEqualTo(md5(md5(fullName) + "test param" + "param value"));

        assertThat(results.getTestResults())
                .filteredOn("name", "Runtime API")
                .flatExtracting(TestResult::getSteps)
                .filteredOn("name", "Given runtime api")
                .flatExtracting(StepResult::getSteps)
                .filteredOn("name", "some attachment")
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .containsExactlyInAnyOrder("some attachment");
    }

    private AllureResults runStories(final String... storyResources) {
        return runStories(List.of(), storyResources);
    }

    private AllureResults runStoriesWithSteps(final Object steps, final String... storyResources) {
        return runStories(List.of(steps), storyResources);
    }

    private AllureResults runStories(final List<Object> additionalSteps, final String... storyResources) {
        return step("Run JBehave stories and collect Allure results", () -> RunUtils.runTests(lifecycle -> {
            final Embedder embedder = new Embedder();
            embedder.useEmbedderMonitor(new NullEmbedderMonitor());
            embedder.useEmbedderControls(
                    new EmbedderControls()
                            .doGenerateViewAfterStories(false)
                            .doFailOnStoryTimeout(false)
                            .doBatch(false)
                            .doIgnoreFailureInStories(true)
                            .doIgnoreFailureInView(true)
                            .doVerboseFailures(false)
                            .doVerboseFiltering(false)
            );
            final AllureJbehave5 allureJbehave5 = new AllureJbehave5(lifecycle);
            embedder.useConfiguration(
                    new MostUsefulConfiguration()
                            .useStoryLoader(new LoadFromClasspath(this.getClass()))
                            .useStoryReporterBuilder(
                                    new ReportlessStoryReporterBuilder(temp.toFile())
                                            .withReporters(allureJbehave5)
                            )
                            .useDefaultStoryReporter(new NullStoryReporter())
            );
            final List<Object> stepInstances = new ArrayList<>(
                    Arrays.asList(
                            new SimpleStorySteps(),
                            new BrokenStorySteps(),
                            new RuntimeApiSteps()
                    )
            );
            stepInstances.addAll(additionalSteps);
            final InjectableStepsFactory stepsFactory = new InstanceStepsFactory(
                    embedder.configuration(),
                    stepInstances.toArray()
            );
            embedder.useStepsFactory(stepsFactory);
            embedder.runStoriesAsPaths(Arrays.asList(storyResources));
        }));
    }

    private static List<GlobalError> getGlobalErrors(final AllureResults results) {
        return results.getGlobals().stream()
                .flatMap(globals -> globals.getErrors().stream())
                .collect(Collectors.toList());
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
