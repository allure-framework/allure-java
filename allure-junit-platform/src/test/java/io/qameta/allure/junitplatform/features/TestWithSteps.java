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
package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * @author charlie (Dmitry Baev).
 */
public class TestWithSteps {

    @Test
    void testWithSteps() {
        step("first");
        step("second");
        step("third");
    }

    protected final void step(final String stepName) {
        final String uuid = UUID.randomUUID().toString();
        try {
            Allure.getLifecycle().startStep(uuid, new StepResult()
                    .setName(stepName)
                    .setStatus(Status.PASSED)
            );
        } finally {
            Allure.getLifecycle().stopStep(uuid);
        }
    }
}
