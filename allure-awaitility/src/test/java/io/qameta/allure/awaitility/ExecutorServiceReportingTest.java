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
import io.qameta.allure.Issue;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.IsolatedLifecycle;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IsolatedLifecycle
class ExecutorServiceReportingTest {

    /**
     * Verifies that reusing an Awaitility polling thread cannot retain a finished wait as the parent of steps produced
     * later by the test, protecting the report hierarchy regression described in issue 891.
     */
    @Description
    @Issue("891")
    @Test
    void shouldKeepFollowingStepsAtTestLevelWhenExecutorIsReused() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> {
                // Start the reusable worker before an Allure test context exists.
            }).get(5, SECONDS);

            final AllureAwaitilityListener listener = new AllureAwaitilityListener();
            final List<TestResult> testResults = runWithinTestContext(() -> {
                await("first condition").with()
                        .pollExecutorService(executor)
                        .conditionEvaluationListener(listener)
                        .atMost(Duration.ofSeconds(1))
                        .until(() -> true);
                Allure.step("step after first condition");

                await("second condition").with()
                        .pollExecutorService(executor)
                        .conditionEvaluationListener(listener)
                        .atMost(Duration.ofSeconds(1))
                        .until(() -> true);
                Allure.step("step after second condition");
            }).getTestResults();

            final TestResult testResult = testResults.get(0);
            final List<StepResult> topLevelSteps = testResult.getSteps();

            assertThat(topLevelSteps)
                    .extracting(StepResult::getName)
                    .containsExactly(
                            "Awaitility: first condition",
                            "step after first condition",
                            "Awaitility: second condition",
                            "step after second condition"
                    );
        } finally {
            executor.shutdownNow();
        }
    }

}
