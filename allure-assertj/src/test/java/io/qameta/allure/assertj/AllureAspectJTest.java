/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.assertj;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureAspectJTest {

    @AllureFeatures.Steps
    @Test
    void shouldCreateStepsForAsserts() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat("Data")
                    .hasSize(4);
        }, AllureAspectJ::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "assertThat 'Data'",
                        "hasSize '4'"
                );
    }

    @AllureFeatures.Steps
    @Test
    void shouldHandleNullableObject() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat((Object) null)
                    .as("Nullable object")
                    .isNull();
        }, AllureAspectJ::setLifecycle);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "assertThat 'null'",
                        "as 'Nullable object []'",
                        "isNull"
                );
    }

    @AllureFeatures.Steps
    @Test
    void shouldHandleByteArrayObject() {
        final String s = "some string";
        final AllureResults results = runWithinTestContext(() -> {
            assertThat(s.getBytes(StandardCharsets.UTF_8))
                    .as("Byte array object")
                    .isEqualTo(s.getBytes(StandardCharsets.UTF_8));
        }, AllureAspectJ::setLifecycle);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "assertThat '<BINARY>'",
                        "as 'Byte array object []'",
                        "isEqualTo '<BINARY>'"
                );
    }

    @AllureFeatures.Steps
    @Test
    void softAssertions() {
        final AllureResults results = runWithinTestContext(() -> {
            final SoftAssertions soft = new SoftAssertions();
            soft.assertThat(25)
                    .as("Test description")
                    .isEqualTo(26);
            soft.assertAll();
        }, AllureAspectJ::setLifecycle);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .contains("as 'Test description []'", "isEqualTo '26'");
    }
}
