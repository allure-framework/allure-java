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

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureAspectJTest {

    private AllureResultsWriterStub results;

    private AllureLifecycle lifecycle;

    @BeforeEach
    public void initLifecycle() {
        results = new AllureResultsWriterStub();
        lifecycle = new AllureLifecycle(results);
        AllureAspectJ.setLifecycle(lifecycle);
    }

    @AllureFeatures.Steps
    @Test
    void shouldCreateStepsForAsserts() {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);


        assertThat("Data")
                .hasSize(4);

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assertThat 'Data'", "hasSize '4'");
    }

    @AllureFeatures.Steps
    @Test
    public void shouldHandleNullableObject() {
        assertThat((Object) null).as("Nullable object").isNull();
    }
}
