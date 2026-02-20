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

import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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

    @TestFactory
    Stream<DynamicNode> bothAwaitilityStepsShouldAppearTest() {
        final List<TestResult> testResult = runWithinTestContext(() -> {
                    await().with()
                            .alias("First waiting")
                            .until(() -> true);
                    await().with()
                            .alias("Second waiting")
                            .until(() -> true);
                },
                AllureAwaitilityListener::setLifecycle
        ).getTestResults();

        return Stream.of(
                DynamicTest.dynamicTest("Exactly 2 top level step for 2 awaitility condition", () ->
                        assertThat(testResult.get(0).getSteps())
                                .describedAs("Allure TestResult contains exactly 2 top level step for 2 awaitility condition")
                                .hasSize(2)
                ),
                DynamicTest.dynamicTest("All top level step for all awaitility condition has PASSED", () ->
                        assertThat(testResult.get(0).getSteps())
                                .describedAs("Allure TestResult contains all top level step for all awaitility with PASSED condition")
                                .allMatch(step -> Status.PASSED.equals(step.getStatus()))
                )
        );
    }

}
