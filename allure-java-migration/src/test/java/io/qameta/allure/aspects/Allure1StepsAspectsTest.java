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
package io.qameta.allure.aspects;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Test;
import ru.yandex.qatools.allure.annotations.Step;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class Allure1StepsAspectsTest {

    @Test
    void shouldSetupStepTitleFromAnnotation() {
        final AllureResults results = runWithinTestContext(
                () -> stepWithTitleAndWithParameter("parameter value"),
                Allure1StepsAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("step with title and parameter [parameter value]");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .extracting("name", "value")
                .containsExactly(tuple("parameter", "parameter value"));
    }

    @Test
    void shouldSetupStepTitleFromMethodSignature() {
        final AllureResults results = runWithinTestContext(
                () -> stepWithoutTitleAndWithParameter("parameter value"),
                Allure1StepsAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("stepWithoutTitleAndWithParameter[parameter value]");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .extracting("name", "value")
                .containsExactly(tuple("parameter", "parameter value"));
    }

    @SuppressWarnings("all")
    @Step
    void stepWithoutTitleAndWithParameter(String parameter) {

    }

    @SuppressWarnings("all")
    @Step("step with title and parameter [{0}]")
    void stepWithTitleAndWithParameter(String parameter) {
    }

}
