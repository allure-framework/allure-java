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

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.IsolatedLifecycle;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IsolatedLifecycle
class ExceptionReportingTest {

    /**
     * Verifies that reusing a listener after a propagated condition exception cannot retain the failed wait as the
     * parent of the next condition or later test steps, and that the aborted wait's step is finalized when the next
     * condition starts. Awaitility fires no callback for a propagated exception, so a wait aborted this way is
     * cleaned up when the same listener starts its next condition.
     */
    @Description
    @Test
    void shouldDiscardAbortedConditionBindingBeforeListenerReuse() {
        final IllegalStateException failure = new IllegalStateException("first condition aborted");
        final AtomicReference<IllegalStateException> caught = new AtomicReference<>();
        final AllureAwaitilityListener listener = new AllureAwaitilityListener();

        final List<TestResult> testResults = runWithinTestContext(() -> {
            try {
                await("aborting condition").with()
                        .pollInSameThread()
                        .conditionEvaluationListener(listener)
                        .atMost(Duration.ofSeconds(1))
                        .until(() -> {
                            throw failure;
                        });
            } catch (IllegalStateException exception) {
                caught.set(exception);
            }

            await("following condition").with()
                    .pollInSameThread()
                    .conditionEvaluationListener(listener)
                    .atMost(Duration.ofSeconds(1))
                    .until(() -> true);
            Allure.step("step after following condition");
        }).getTestResults();

        assertThat(caught.get())
                .isSameAs(failure);

        final List<StepResult> topLevelSteps = testResults.get(0).getSteps();
        assertThat(topLevelSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "Awaitility: aborting condition",
                        "Awaitility: following condition",
                        "step after following condition"
                );

        final StepResult abortedCondition = topLevelSteps.get(0);
        assertThat(abortedCondition.getStage())
                .as("aborted condition stage")
                .isEqualTo(Stage.FINISHED);
        assertThat(abortedCondition.getStop())
                .as("aborted condition stop time")
                .isGreaterThanOrEqualTo(abortedCondition.getStart());
    }

}
