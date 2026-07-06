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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IsolatedLifecycle
public class MultipleConditionsTest {

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
     * Verifies that a test containing two Awaitility conditions reports a separate top-level step for each wait.
     */
    @Description
    @Test
    void shouldRecordTopLevelStepForEachAwaitilityCondition() {
        final List<StepResult> steps = runMultipleAwaitilityTopLevelSteps();

        assertThat(steps)
                .describedAs("Allure TestResult contains exactly 2 top level step for 2 awaitility condition")
                .hasSize(2);
    }

    /**
     * Verifies that every top-level step created for multiple successful Awaitility conditions is marked passed.
     */
    @Description
    @Test
    void shouldMarkEveryAwaitilityConditionStepPassed() {
        final List<StepResult> steps = runMultipleAwaitilityTopLevelSteps();

        assertThat(steps)
                .describedAs("Allure TestResult contains all top level step for all awaitility with PASSED condition")
                .allMatch(step -> Status.PASSED.equals(step.getStatus()));
    }

    private List<StepResult> runMultipleAwaitilityTopLevelSteps() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
            await().with()
                    .alias("First waiting")
                    .until(() -> true);
            await().with()
                    .alias("Second waiting")
                    .until(() -> true);
        }
        ).getTestResults();

        return testResult.get(0).getSteps();
    }

}
