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
package io.qameta.allure.assertj;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatList;
import static org.assertj.core.api.MapAssert.assertThatMap;

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
                        "describedAs 'Byte array object'",
                        "isEqualTo '<BINARY>'"
                );
    }

    @AllureFeatures.Steps
    @Test
    void shouldHandleCollections() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat(Arrays.asList("a", "b"))
                .containsExactly("a", "b");
        }, AllureAspectJ::setLifecycle);

        assertThat(results.getTestResults())
            .flatExtracting(TestResult::getSteps)
            .extracting(StepResult::getName)
            .containsExactly(
                "assertThatList '[a, b]'",
                "containsExactly '[a, b]'"
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

    @Test
    void preventListAssertionsBubblingTest() {
        final AllureResults results = runWithinTestContext(
                () -> assertThatList(List.of("value1")).isEqualTo(List.of("value1", "value2")),
                AllureAspectJ::setLifecycle);

        assertThat(results.getTestResults()).isNotEmpty();
        assertThat(results.getTestResults().get(0).getSteps()).hasSize(2);
        assertThat(results.getTestResults().get(0).getSteps().get(1).getSteps()).isEmpty();
    }

    @Test
    void preventMapAssertionsBubblingTest() {
        final AllureResults results = runWithinTestContext(
                () -> assertThatMap(Map.of("key1", "value1"))
                        .isEqualTo(Map.of("key1", "value1", "key2", "value2")),
                AllureAspectJ::setLifecycle);

        assertThat(results.getTestResults()).isNotEmpty();
        assertThat(results.getTestResults().get(0).getSteps()).hasSize(2);
        assertThat(results.getTestResults().get(0).getSteps().get(1).getSteps()).isEmpty();
    }
}
