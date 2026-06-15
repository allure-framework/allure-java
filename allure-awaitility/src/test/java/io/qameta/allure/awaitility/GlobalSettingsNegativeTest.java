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
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GlobalSettingsNegativeTest {

    private static final String AWAITILITY_EVALUATION_DESCRIPTION = "Awaitility condition satisfied or not, but awaiting still in progress";

    private static final String AWAITILITY_TIMEOUT_DESCRIPTION = "Awaitility condition timeout";

    @AfterEach
    void reset() {
        Awaitility.reset();
    }

    @BeforeEach
    void setup() {
        Awaitility.pollInSameThread();
        Awaitility.setDefaultConditionEvaluationListener(new AllureAwaitilityListener());
    }

    /**
     * Verifies that a timed-out Awaitility condition using the global Allure listener marks the top-level wait step as
     * failed.
     */
    @Description
    @Test
    void globalSettingsAwaitWoAliasCheckTopLevelFailedStep() {
        final StepResult step = timedOutAwaitTopLevelStep();

        assertThat(step.getStatus())
                .as("Top level step has failed status")
                .isEqualTo(Status.FAILED);
    }

    /**
     * Verifies that a timed-out Awaitility condition exposes both the failed condition-evaluation step and the timeout
     * event as child report steps.
     */
    @Description
    @Test
    void timedOutAwaitShouldCreateSecondLevelStepForFailedPollAndTimeout() {
        final List<StepResult> steps = timedOutAwaitPollSteps();

        assertThat(steps)
                .as("Exactly 2 second level steps for 2 polling iterations")
                .hasSize(2);
    }

    /**
     * Verifies that the failed poll before timeout records the evaluated condition, expected and actual values, and
     * timing context.
     */
    @Description
    @Test
    void timedOutAwaitShouldDescribeFailedPoll() {
        final StepResult step = failedPollStepBeforeTimeout();

        assertThat(step.getName())
                .contains("io.qameta.allure.awaitility.GlobalSettingsNegativeTest")
                .contains("expected: 3")
                .contains("but was: 0")
                .contains("elapsed time")
                .contains("remaining time")
                .contains("last poll interval was");
    }

    /**
     * Verifies that a failed poll that has not yet exhausted the wait timeout remains passed in the report.
     */
    @Description
    @Test
    void timedOutAwaitShouldMarkFailedPollPassed() {
        final StepResult step = failedPollStepBeforeTimeout();

        assertThat(step.getStatus())
                .isEqualTo(Status.PASSED);
    }

    /**
     * Verifies that the terminal poll for a timed-out wait clearly reports the timeout condition to report consumers.
     */
    @Description
    @Test
    void timedOutAwaitShouldDescribeTimeoutPoll() {
        final StepResult step = timeoutPollStep();

        assertThat(step.getName())
                .contains("Condition timeout.")
                .contains("io.qameta.allure.awaitility.GlobalSettingsNegativeTest");
    }

    /**
     * Verifies that the terminal timeout poll is marked broken so the report distinguishes timeout failure from normal
     * polling attempts.
     */
    @Description
    @Test
    void timedOutAwaitShouldMarkTimeoutPollBroken() {
        final StepResult step = timeoutPollStep();

        assertThat(step.getStatus())
                .isEqualTo(Status.BROKEN);
    }

    private List<StepResult> runTimedOutAwaitWithoutAliasTopLevelSteps() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
            final AtomicInteger atomicInteger = new AtomicInteger(0);
            await().with()
                    .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                    .pollInterval(Duration.of(500, ChronoUnit.MILLIS))
                    .untilAsserted(() -> assertThat(atomicInteger.getAndIncrement()).isEqualTo(3));
        },
                AllureAwaitilityListener::setLifecycle
        ).getTestResults();

        return testResult.get(0).getSteps();
    }

    private StepResult timedOutAwaitTopLevelStep() {
        return runTimedOutAwaitWithoutAliasTopLevelSteps().get(0);
    }

    private List<StepResult> timedOutAwaitPollSteps() {
        return timedOutAwaitTopLevelStep().getSteps().stream()
                .filter(GlobalSettingsNegativeTest::isAwaitilityEvaluationOrTimeoutStep)
                .toList();
    }

    private static boolean isAwaitilityEvaluationOrTimeoutStep(final StepResult step) {
        return AWAITILITY_EVALUATION_DESCRIPTION.equals(step.getDescription())
                || AWAITILITY_TIMEOUT_DESCRIPTION.equals(step.getDescription());
    }

    private StepResult failedPollStepBeforeTimeout() {
        return timedOutAwaitPollSteps().get(0);
    }

    private StepResult timeoutPollStep() {
        return timedOutAwaitPollSteps().get(1);
    }

}
