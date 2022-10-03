/*
 *  Copyright 2022 Qameta Software OÜ
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

import io.qameta.allure.awaitility.AllureAwaitilityListener;
import io.qameta.allure.model.Status;
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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GlobalSettingsNegativeTest {

    @AfterEach
    void reset() {
        Awaitility.reset();
    }

    @BeforeEach
    void setup() {
        Awaitility.setDefaultConditionEvaluationListener(new AllureAwaitilityListener());
    }

    /**
     * Negative test to check proper allure steps generation.
     * <p>
     * Precondition: static settings, await without alias
     * <p>
     * Test should check that:
     * <li>1. Top level step has broken status</li>
     */
    @Test
    void globalSettingsAwaitWoAliasCheckTopLevelBrokenStep() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
                    final AtomicInteger atomicInteger = new AtomicInteger(0);
                    await().with()
                            .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                            .pollInterval(Duration.of(500, ChronoUnit.MILLIS))
                            .until(atomicInteger::getAndIncrement, is(3));
                },
                AllureAwaitilityListener::setLifecycle
        ).getTestResults();
        assertEquals(
                Status.FAILED, testResult.get(0).getSteps().get(0).getStatus(),
                "Top level step has broken status"
        );
    }

    /**
     * Positive test to check proper allure steps generation.
     * <p>
     * Precondition: static settings, await without alias
     * <p>
     * Test should check that:
     * <li>1. Allure has exactly 2 second-level steps for condition with 2 polls iteration</li>
     * <li>2. All second-level steps should have passed or broken status for successful and timeout evaluations</li>
     * <li>3. All second-level steps should have information about polling intervals and evaluation</li>
     */
    @Test
    void globalSettingsCheckAwaitWoAliasSecondLevelTimeoutStep() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
                    final AtomicInteger atomicInteger = new AtomicInteger(0);
                    await().with()
                            .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                            .pollInterval(Duration.of(500, ChronoUnit.MILLIS))
                            .until(atomicInteger::getAndIncrement, is(3));
                },
                AllureAwaitilityListener::setLifecycle
        ).getTestResults();
        assertAll(
                () -> assertEquals(
                        2, testResult.get(0).getSteps().get(0).getSteps().size(),
                        "Exactly 2 second level steps for 2 polling iterations"
                ),
                () -> assertThat(testResult.get(0).getSteps().get(0).getSteps().get(0).getName())
                        .contains("Lambda expression in io.qameta.allure.awaitility.GlobalSettingsNegativeTest")
                        .contains("that uses java.util.concurrent.atomic.AtomicInteger:")
                        .contains("expected <3> but was <0>")
                        .contains("elapsed time")
                        .contains("remaining time")
                        .contains("last poll interval was"),
                () -> assertThat(testResult.get(0).getSteps().get(0).getSteps().get(0).getStatus())
                        .isEqualTo(Status.PASSED),
                () -> assertThat(testResult.get(0).getSteps().get(0).getSteps().get(1).getName())
                        .contains("Condition timeout.")
                        .contains("Lambda expression in io.qameta.allure.awaitility.GlobalSettingsNegativeTest")
                        .contains("expected <3> but was <0> within 1 seconds"),
                () -> assertThat(testResult.get(0).getSteps().get(0).getSteps().get(1).getStatus())
                        .isEqualTo(Status.BROKEN)
        );
    }

}
