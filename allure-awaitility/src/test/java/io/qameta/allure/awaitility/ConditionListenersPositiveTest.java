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
package io.qameta.allure.awaitility;

import io.qameta.allure.Description;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.IsolatedLifecycle;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IsolatedLifecycle
class ConditionListenersPositiveTest {

    private static final String AWAITILITY_EVALUATION_DESCRIPTION = "Awaitility condition satisfied or not, but awaiting still in progress";

    @BeforeAll
    static void setup() {
        Awaitility.pollInSameThread();
    }

    /**
     * Verifies that a successful Awaitility condition with an inline Allure listener is reported as one top-level step
     * when no await alias is provided.
     */
    @Description
    @Test
    void awaitWithoutAliasShouldCreateSingleTopLevelStep() {
        final List<StepResult> steps = runAwaitWithoutAliasTopLevelSteps();

        assertThat(steps)
                .as("Exactly 1 top level step for 1 awaitility condition")
                .hasSize(1);
    }

    /**
     * Verifies that the top-level report step for a successful Awaitility condition with an inline listener is marked
     * passed.
     */
    @Description
    @Test
    void awaitWithoutAliasShouldMarkTopLevelStepPassed() {
        final StepResult step = awaitWithoutAliasTopLevelStep();

        assertThat(step.getStatus())
                .as("Top level step has passed status")
                .isEqualTo(Status.PASSED);
    }

    /**
     * Verifies that a successful Awaitility condition without an alias keeps the default top-level step name in the
     * Allure report.
     */
    @Description
    @Test
    void awaitWithoutAliasShouldUseDefaultTopLevelStepName() {
        final StepResult step = awaitWithoutAliasTopLevelStep();

        assertThat(step.getName())
                .as("Top level step has default name because await() wo alias")
                .isEqualTo("Awaitility: Starting evaluation");
    }

    /**
     * Verifies that an Awaitility alias supplied on a condition with an inline Allure listener is visible in the
     * top-level report step name.
     */
    @Description
    @Test
    void globalSettingsAwaitWithAliasCheckTopLevelPassedStep() {
        final StepResult step = awaitWithAliasTopLevelStep();

        assertThat(step.getName())
                .as("Top level step has name with alias")
                .isEqualTo("Awaitility: Counter should be at least 3");
    }

    /**
     * Verifies that report consumers can see one condition-evaluation child step for each poll performed before a
     * successful Awaitility condition completes.
     */
    @Description
    @Test
    void awaitWithoutAliasShouldCreateSecondLevelStepForEachPoll() {
        final List<StepResult> steps = awaitWithoutAliasPollSteps();

        assertThat(steps)
                .as("Exactly 4 second level steps for 4 polling iterations")
                .hasSize(4);
    }

    /**
     * Verifies that all poll-level report steps for a successfully completed Awaitility condition are marked passed.
     */
    @Description
    @Test
    void awaitWithoutAliasShouldMarkAllSecondLevelStepsPassed() {
        final List<StepResult> steps = awaitWithoutAliasPollSteps();

        assertThat(steps)
                .as("All second level steps has passed statuses")
                .allMatch(x -> x.getStatus().equals(Status.PASSED));
    }

    /**
     * Verifies that the first failed poll records the evaluated condition, expected value, actual value, and timing
     * context in the report.
     */
    @Description
    @Test
    void awaitWithoutAliasShouldDescribeFirstFailedPoll() {
        final StepResult step = firstFailedPollStep();

        assertThat(step.getName())
                .contains("io.qameta.allure.awaitility.ConditionListenersPositiveTest")
                .contains("expected: 3")
                .contains("but was: 0")
                .contains("elapsed time")
                .contains("remaining time")
                .contains("last poll interval was");
    }

    /**
     * Verifies that the second failed poll records the next observed value together with the evaluated condition and
     * timing context.
     */
    @Description
    @Test
    void awaitWithoutAliasShouldDescribeSecondFailedPoll() {
        final StepResult step = secondFailedPollStep();

        assertThat(step.getName())
                .contains("io.qameta.allure.awaitility.ConditionListenersPositiveTest")
                .contains("expected: 3")
                .contains("but was: 1")
                .contains("elapsed time")
                .contains("remaining time")
                .contains("last poll interval was");
    }

    /**
     * Verifies that the third failed poll records the final unsuccessful value before the Awaitility condition succeeds.
     */
    @Description
    @Test
    void awaitWithoutAliasShouldDescribeThirdFailedPoll() {
        final StepResult step = thirdFailedPollStep();

        assertThat(step.getName())
                .contains("io.qameta.allure.awaitility.ConditionListenersPositiveTest")
                .contains("expected: 3")
                .contains("but was: 2")
                .contains("elapsed time")
                .contains("remaining time")
                .contains("last poll interval was");
    }

    /**
     * Verifies that the successful poll reports the reached value and timing context so consumers can identify why the
     * wait completed.
     */
    @Description
    @Test
    void awaitWithoutAliasShouldDescribeSuccessfulPoll() {
        final StepResult step = successfulPollStep();

        assertThat(step.getName())
                .contains("io.qameta.allure.awaitility.ConditionListenersPositiveTest")
                .contains("reached its end value after")
                .contains("remaining time")
                .contains("last poll interval was");
    }

    /**
     * Verifies that Awaitility 4.3's supplier-and-consumer {@code untilAsserted} overload reports every supplied value
     * under one successful Awaitility step.
     */
    @Description
    @Test
    void supplierConsumerUntilAssertedShouldReportPollsUnderSingleAwaitilityStep() {
        final List<TestResult> testResults = runWithinTestContext(() -> {
            final AtomicInteger counter = new AtomicInteger();
            await("supplied counter reaches 2").with()
                    .conditionEvaluationListener(new AllureAwaitilityListener())
                    .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                    .pollInterval(Duration.of(10, ChronoUnit.MILLIS))
                    .untilAsserted(counter::getAndIncrement, value -> assertThat(value).isEqualTo(2));
        }).getTestResults();

        final TestResult testResult = testResults.get(0);
        assertThat(testResult.getSteps())
                .singleElement()
                .satisfies(step -> {
                    assertThat(step.getName()).isEqualTo("Awaitility: supplied counter reaches 2");
                    assertThat(step.getStatus()).isEqualTo(Status.PASSED);
                    final List<StepResult> evaluationSteps = step.getSteps().stream()
                            .filter(ConditionListenersPositiveTest::isAwaitilityEvaluationStep)
                            .toList();
                    assertThat(evaluationSteps)
                            .hasSize(3);
                });
    }

    /**
     * Verifies that a nested wait with its own listener keeps the outer condition active and reports the inner
     * condition beneath it.
     */
    @Description
    @Test
    void nestedAwaitShouldKeepOuterConditionScope() {
        final List<TestResult> testResults = runWithinTestContext(
                () -> await("outer condition").with()
                        .pollInSameThread()
                        .conditionEvaluationListener(new AllureAwaitilityListener())
                        .atMost(Duration.ofSeconds(1))
                        .until(() -> {
                            await("inner condition").with()
                                    .pollInSameThread()
                                    .conditionEvaluationListener(new AllureAwaitilityListener())
                                    .atMost(Duration.ofSeconds(1))
                                    .until(() -> true);
                            return true;
                        })
        ).getTestResults();

        final List<StepResult> topLevelSteps = testResults.get(0).getSteps();
        assertThat(topLevelSteps)
                .extracting(StepResult::getName)
                .containsExactly("Awaitility: outer condition");

        final List<String> nestedStepNames = topLevelSteps.get(0).getSteps().stream()
                .map(StepResult::getName)
                .toList();
        assertThat(nestedStepNames)
                .as("nested condition step names")
                .contains("Awaitility: inner condition");
    }

    private List<StepResult> runAwaitWithoutAliasTopLevelSteps() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
            final AtomicInteger atomicInteger = new AtomicInteger(0);
            await().with()
                    .conditionEvaluationListener(new AllureAwaitilityListener())
                    .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                    .pollInterval(Duration.of(50, ChronoUnit.MILLIS))
                    .untilAsserted(() -> assertThat(atomicInteger.getAndIncrement()).isEqualTo(3));
        }).getTestResults();

        return testResult.get(0).getSteps();
    }

    private StepResult awaitWithoutAliasTopLevelStep() {
        return runAwaitWithoutAliasTopLevelSteps().get(0);
    }

    private StepResult awaitWithAliasTopLevelStep() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
            final AtomicInteger atomicInteger = new AtomicInteger(0);
            await("Counter should be at least 3").with()
                    .conditionEvaluationListener(new AllureAwaitilityListener())
                    .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                    .pollInterval(Duration.of(50, ChronoUnit.MILLIS))
                    .untilAsserted(() -> assertThat(atomicInteger.getAndIncrement()).isEqualTo(3));
        }).getTestResults();

        return testResult.get(0).getSteps().get(0);
    }

    private List<StepResult> awaitWithoutAliasPollSteps() {
        return awaitWithoutAliasTopLevelStep().getSteps().stream()
                .filter(ConditionListenersPositiveTest::isAwaitilityEvaluationStep)
                .toList();
    }

    private static boolean isAwaitilityEvaluationStep(final StepResult step) {
        return AWAITILITY_EVALUATION_DESCRIPTION.equals(step.getDescription());
    }

    private StepResult firstFailedPollStep() {
        return awaitWithoutAliasPollSteps().get(0);
    }

    private StepResult secondFailedPollStep() {
        return awaitWithoutAliasPollSteps().get(1);
    }

    private StepResult thirdFailedPollStep() {
        return awaitWithoutAliasPollSteps().get(2);
    }

    private StepResult successfulPollStep() {
        return awaitWithoutAliasPollSteps().get(3);
    }

}
