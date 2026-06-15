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
package io.qameta.allure.test;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RunUtilsTest {

    @Test
    void shouldCaptureFailureStatusWithinSyntheticTestContext() {
        final AllureResults results = Allure.step("Execute a synthetic test context that raises an assertion error", () -> RunUtils.runWithinTestContext(() -> {
            throw new AssertionError("boom");
        })
        );

        Allure.step("Verify the captured synthetic test result is marked as failed", () -> {
            assertThat(results.getTestResults())
                    .hasSize(1);

            final TestResult testResult = results.getTestResults().get(0);
            assertThat(testResult.getStatus())
                    .isEqualTo(Status.FAILED);
            assertThat(testResult.getStatusDetails().getMessage())
                    .contains("boom");
        });
    }

    @Test
    void shouldAttachNestedRunArtifactsToOuterLifecycle() {
        final AllureResults results = Allure
                .step("Execute a nested synthetic run and capture its emitted attachments", () -> RunUtils.runWithinTestContext(() -> RunUtils.runWithinTestContext(() -> {
                })
                )
                );

        Allure.addAttachment("nested-attachment-keys", String.join("\n", results.getAttachments().keySet()));
        Allure.step("Verify the outer lifecycle receives serialized artifacts from the nested run", () -> {
            assertThat(results.getAttachments())
                    .isNotEmpty();
            assertThat(results.getAttachments().values())
                    .anySatisfy(
                            bytes -> assertThat(new String(bytes, StandardCharsets.UTF_8))
                                    .contains("\"uuid\"")
                    );
        });
    }
}
