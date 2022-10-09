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

import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
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
class ConditionListenersPositiveTest {

    /**
     * Positive test to check proper allure steps generation.
     * <p>
     * Precondition: listener into condition declaration, await without alias
     * <p>
     * Test should check that:
     * <li>1. Allure has exactly 1 top-level step for 1 await condition</li>
     * <li>2. Top level step has passed status</li>
     * <li>3. Top level step has default name</li>
     */
    @Test
    void globalSettingsAwaitWoAliasCheckTopLevelPassedStep() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
                    final AtomicInteger atomicInteger = new AtomicInteger(0);
                    await().with()
                            .conditionEvaluationListener(new AllureAwaitilityListener())
                            .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                            .pollInterval(Duration.of(50, ChronoUnit.MILLIS))
                            .until(atomicInteger::getAndIncrement, is(3));
                },
                AllureAwaitilityListener::setLifecycle
        ).getTestResults();
        assertAll(
                () -> assertEquals(
                        1, testResult.get(0).getSteps().size(),
                        "Exactly 1 top level step for 1 awaitility condition"
                ),
                () -> assertEquals(
                        Status.PASSED, testResult.get(0).getSteps().get(0).getStatus(),
                        "Top level step has passed status"
                ),
                () -> assertEquals(
                        "Awaitility: Starting evaluation",
                        testResult.get(0).getSteps().get(0).getName(),
                        "Top level step has default name because await() wo alias"
                )
        );
    }

    /**
     * Positive test to check proper allure steps generation.
     * <p>
     * Precondition: listener into condition declaration, await with alias
     * <p>
     * Test should check that:
     * <li>1. Top level step has name with alias from await('alias')</li>
     */
    @Test
    void globalSettingsAwaitWithAliasCheckTopLevelPassedStep() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
                    final AtomicInteger atomicInteger = new AtomicInteger(0);
                    await("Counter should be at least 3").with()
                            .conditionEvaluationListener(new AllureAwaitilityListener())
                            .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                            .pollInterval(Duration.of(50, ChronoUnit.MILLIS))
                            .until(atomicInteger::getAndIncrement, is(3));
                },
                AllureAwaitilityListener::setLifecycle
        ).getTestResults();
        assertEquals(
                "Awaitility: Counter should be at least 3",
                testResult.get(0).getSteps().get(0).getName(),
                "Top level step has name with alias"
        );
    }

    /**
     * Positive test to check proper allure steps generation.
     * <p>
     * Precondition: listener into condition declaration, await without alias
     * <p>
     * Test should check that:
     * <li>1. Allure has exactly 4 second-level steps for condition with 4 polls iteration</li>
     * <li>2. All second-level steps should have passed status for successful condition evaluation</li>
     * <li>3. All second-level steps should have information about polling intervals and evaluation</li>
     */
    @Test
    void globalSettingsCheckAwaitWoAliasSecondLevelPassedSteps() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
                    final AtomicInteger atomicInteger = new AtomicInteger(0);
                    await().with()
                            .conditionEvaluationListener(new AllureAwaitilityListener())
                            .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                            .pollInterval(Duration.of(50, ChronoUnit.MILLIS))
                            .until(atomicInteger::getAndIncrement, is(3));
                },
                AllureAwaitilityListener::setLifecycle
        ).getTestResults();
        assertAll(
                () -> assertEquals(
                        4, testResult.get(0).getSteps().get(0).getSteps().size(),
                        "Exactly 4 second level steps for 4 polling iterations"
                ),
                () -> assertEquals(
                        4, (int) testResult.get(0).getSteps().get(0).getSteps().stream()
                                .filter(x -> x.getStatus().equals(Status.PASSED)).count(),
                        "All second level steps has passed statuses"
                ),
                () -> assertThat(testResult.get(0).getSteps().get(0).getSteps().get(0).getName())
                        .contains("Lambda expression in io.qameta.allure.awaitility.ConditionListenersPositiveTest")
                        .contains("that uses java.util.concurrent.atomic.AtomicInteger:")
                        .contains("expected <3> but was <0>")
                        .contains("elapsed time")
                        .contains("remaining time")
                        .contains("last poll interval was"),
                () -> assertThat(testResult.get(0).getSteps().get(0).getSteps().get(1).getName())
                        .contains("Lambda expression in io.qameta.allure.awaitility.ConditionListenersPositiveTest")
                        .contains("that uses java.util.concurrent.atomic.AtomicInteger:")
                        .contains("expected <3> but was <1>")
                        .contains("elapsed time")
                        .contains("remaining time")
                        .contains("last poll interval was"),
                () -> assertThat(testResult.get(0).getSteps().get(0).getSteps().get(2).getName())
                        .contains("Lambda expression in io.qameta.allure.awaitility.ConditionListenersPositiveTest")
                        .contains("that uses java.util.concurrent.atomic.AtomicInteger:")
                        .contains("expected <3> but was <2>")
                        .contains("elapsed time")
                        .contains("remaining time")
                        .contains("last poll interval was"),
                () -> assertThat(testResult.get(0).getSteps().get(0).getSteps().get(3).getName())
                        .contains("Lambda expression in io.qameta.allure.awaitility.ConditionListenersPositiveTest")
                        .contains("that uses java.util.concurrent.atomic.AtomicInteger:")
                        .contains("reached its end value of <3> after")
                        .contains("remaining time")
                        .contains("last poll interval was")
        );
    }

}
