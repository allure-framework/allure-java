/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.spock;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.spock.samples.BrokenTest;
import io.qameta.allure.spock.samples.DataDrivenTest;
import io.qameta.allure.spock.samples.FailedTest;
import io.qameta.allure.spock.samples.OneTest;
import io.qameta.allure.spock.samples.ParametersTest;
import io.qameta.allure.spock.samples.TestWithAnnotations;
import io.qameta.allure.spock.samples.TestWithSteps;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.Test;
import org.junit.runner.notification.RunNotifier;
import org.spockframework.runtime.JUnitDescriptionGenerator;
import org.spockframework.runtime.RunContext;
import org.spockframework.runtime.SpecInfoBuilder;
import org.spockframework.runtime.model.SpecInfo;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unchecked")
class AllureSpockTest {

    @Test
    void shouldStoreTestsInformation() {
        final AllureResults results = run(OneTest.class);
        assertThat(results.getTestResults())
                .hasSize(1);
    }

    @Test
    void shouldSetTestStart() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = run(OneTest.class);

        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetTestStop() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = run(OneTest.class);
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetTestFullName() {
        final AllureResults results = run(OneTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getFullName)
                .containsExactly("io.qameta.allure.spock.samples.OneTest.Simple Test");
    }

    @Test
    void shouldSetStageFinished() {
        final AllureResults results = run(OneTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStage)
                .containsExactly(Stage.FINISHED);
    }

    @Test
    void shouldProcessFailedTest() {
        final AllureResults results = run(FailedTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactly(Status.FAILED);
    }

    @Test
    void shouldProcessBrokenTest() {
        final AllureResults results = run(BrokenTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactly(Status.BROKEN);
    }

    @Test
    void shouldAddStepsToTest() {
        final AllureResults results = run(TestWithSteps.class);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("step1", "step2", "step3");
    }

    @Test
    void shouldProcessMethodAnnotations() {
        final AllureResults results = run(TestWithAnnotations.class);
        assertThat(results.getTestResults())
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
    void shouldProcessClassAnnotations() {
        final AllureResults results = run(TestWithAnnotations.class);
        assertThat(results.getTestResults())
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
    void shouldProcessFlakyAnnotation() {
        final AllureResults results = run(TestWithAnnotations.class);
        assertThat(results.getTestResults())
                .filteredOn(flakyPredicate())
                .hasSize(1);
    }

    @Test
    void shouldProcessMutedAnnotation() {
        final AllureResults results = run(TestWithAnnotations.class);
        assertThat(results.getTestResults())
                .filteredOn(mutedPredicate())
                .hasSize(1);
    }

    @Test
    void shouldSetDisplayName() {
        final AllureResults results = run(OneTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactly("Simple Test");
    }

    @Test
    void shouldSetLinks() {
        final AllureResults results = run(FailedTest.class);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .containsExactlyInAnyOrder("link-1", "link-2", "issue-1", "issue-2", "tms-1", "tms-2");
    }

    @Test
    void shouldSetParameters() {
        final AllureResults results = run(ParametersTest.class);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("a", "1"),
                        tuple("b", "3"),
                        tuple("c", "3")
                );
    }

    @Test
    void shouldSupportDataDrivenTests() {
        final AllureResults results = run(DataDrivenTest.class);
        assertThat(results.getTestResults())
                .hasSize(3);
    }

    protected AllureResults run(final Class<?> clazz) {
        final AllureResultsWriterStub results = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(results);

        final RunNotifier notifier = new RunNotifier();
        final SpecInfo spec = new SpecInfoBuilder(clazz).build();
        spec.addListener(new AllureSpock(lifecycle));

        new JUnitDescriptionGenerator(spec).describeSpecMethods();
        new JUnitDescriptionGenerator(spec).describeSpec();

        final AllureLifecycle cached = Allure.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            StepsAspects.setLifecycle(lifecycle);
            AttachmentsAspects.setLifecycle(lifecycle);

            RunContext.get().createSpecRunner(spec, notifier).run();
        } catch (Exception e) {
            throw new RuntimeException("could not execute sample", e);
        } finally {
            Allure.setLifecycle(cached);
            StepsAspects.setLifecycle(cached);
            AttachmentsAspects.setLifecycle(cached);
        }

        return results;
    }

    private static Predicate<TestResult> mutedPredicate() {
        return testResult -> Optional.of(testResult)
                .map(TestResult::getStatusDetails)
                .map(StatusDetails::isMuted)
                .filter(m -> m)
                .isPresent();
    }

    private static Predicate<TestResult> flakyPredicate() {
        return testResult -> Optional.of(testResult)
                .map(TestResult::getStatusDetails)
                .map(StatusDetails::isFlaky)
                .filter(m -> m)
                .isPresent();
    }

}
