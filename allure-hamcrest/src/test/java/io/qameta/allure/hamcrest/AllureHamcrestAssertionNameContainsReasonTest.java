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
package io.qameta.allure.hamcrest;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;

/**
 * This tests should cover cases when reason string exists in assertion.
 */
@SuppressWarnings("all")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class AllureHamcrestAssertionNameContainsReasonTest {

    @Test
    void hamcrestAssertNameWithComment() {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(
                        "Business always likes something weird",
                        "TheBiscuit",
                        equalToIgnoringCase("thebiscuit")
                ),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly("assert \"TheBiscuit\" a string equal to \"thebiscuit\" ignoring case | Business always likes something weird");
    }

    @Test
    void hamcrestAssertNameWoComment() {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(
                        "TheBiscuit",
                        equalToIgnoringCase("thebiscuit")
                ),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly("assert \"TheBiscuit\" a string equal to \"thebiscuit\" ignoring case");
    }
}
