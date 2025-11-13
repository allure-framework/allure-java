/*
 *  Copyright 2016-2024 Qameta Software Inc
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
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GlobalSettingsNegativeTest {

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
    @TestFactory
    Stream<DynamicNode> globalSettingsCheckAwaitWoAliasSecondLevelTimeoutStep() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
                    final AtomicInteger atomicInteger = new AtomicInteger(0);
                    await().with()
                            .atMost(Duration.of(1000, ChronoUnit.MILLIS))
                            .pollInterval(Duration.of(500, ChronoUnit.MILLIS))
                            .until(atomicInteger::getAndIncrement, is(3));
                },
                AllureAwaitilityListener::setLifecycle
        ).getTestResults();

        return Stream.of(
                dynamicTest("Second level steps count", () ->
                        assertThat(testResult.get(0).getSteps().get(0).getSteps())
                                .as("Exactly 2 second level steps for 2 polling iterations")
                                .hasSize(2)),
                dynamicTest("Second level step 1 name", () ->
                        assertThat(testResult.get(0).getSteps().get(0).getSteps().get(0).getName())
                                .contains("io.qameta.allure.awaitility.GlobalSettingsNegativeTest")
                                .contains("expected <3> but was <0>")
                                .contains("elapsed time")
                                .contains("remaining time")
                                .contains("last poll interval was")),
                dynamicTest("Second level step 1 status", () ->
                        assertThat(testResult.get(0).getSteps().get(0).getSteps().get(0).getStatus())
                                .isEqualTo(Status.PASSED)),
                dynamicTest("Second level step 2 name", () ->
                        assertThat(testResult.get(0).getSteps().get(0).getSteps().get(1).getName())
                                .contains("Condition timeout.")
                                .contains("io.qameta.allure.awaitility.GlobalSettingsNegativeTest")),
                dynamicTest("Second level step 2 status", () ->
                        assertThat(testResult.get(0).getSteps().get(0).getSteps().get(1).getStatus())
                                .isEqualTo(Status.BROKEN))
        );
    }

}
