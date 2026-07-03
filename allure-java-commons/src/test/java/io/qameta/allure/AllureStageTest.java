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
package io.qameta.allure;

import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stages are lightweight phase markers: a stage stays open until the next stage starts or the enclosing step,
 * test, or fixture ends, and the steps logged in that window nest under it.
 */
@IsolatedLifecycle
class AllureStageTest {

    @Test
    void shouldCloseStageOnNextStageAndNestStepsUnderActiveStage() {
        final AllureResults results = runWithinTestContext(() -> {
            Allure.stage("prepare data");
            Allure.step("create customer");

            Allure.stage("submit order");
            Allure.step("post order");
            Allure.step("read response");

            Allure.stage("verify result");
        });

        final List<StepResult> stages = results.getTestResults().get(0).getSteps();
        assertThat(stages)
                .extracting(StepResult::getName)
                .containsExactly("prepare data", "submit order", "verify result");
        assertThat(stages)
                .extracting(StepResult::getStatus)
                .containsOnly(Status.PASSED);
        assertThat(stages)
                .extracting(StepResult::getStage)
                .containsOnly(Stage.FINISHED);
        assertThat(stages)
                .extracting(StepResult::getStop)
                .doesNotContainNull();

        assertThat(stages.get(0).getSteps())
                .extracting(StepResult::getName)
                .containsExactly("create customer");
        assertThat(stages.get(1).getSteps())
                .extracting(StepResult::getName)
                .containsExactly("post order", "read response");
        assertThat(stages.get(2).getSteps()).isEmpty();
    }

    @Test
    void shouldCloseTrailingStageWhenTestStops() {
        final AllureResults results = runWithinTestContext(() -> Allure.stage("only stage"));

        final List<StepResult> steps = results.getTestResults().get(0).getSteps();
        assertThat(steps)
                .extracting(StepResult::getName, StepResult::getStatus, StepResult::getStage)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("only stage", Status.PASSED, Stage.FINISHED));
    }

    @Test
    void shouldNestStageUnderEnclosingStepAndCloseWithIt() {
        final AllureResults results = runWithinTestContext(() -> Allure.step("wrapper", () -> {
            Allure.stage("inner phase");
            Allure.step("inner action");
        }));

        final TestResult testResult = results.getTestResults().get(0);
        final StepResult wrapper = testResult.getSteps().get(0);
        assertThat(wrapper.getName()).isEqualTo("wrapper");

        assertThat(wrapper.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("inner phase");
        final StepResult innerPhase = wrapper.getSteps().get(0);
        assertThat(innerPhase.getStage()).isEqualTo(Stage.FINISHED);
        assertThat(innerPhase.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("inner action");
    }

    @Test
    void shouldCloseStageWhenEnclosingStepIsStoppedByKey() {
        final AllureResults results = runWithinTestContext(() -> {
            final AllureLifecycle lifecycle = Allure.getLifecycle();
            final AllureExternalKey stepKey = AllureExternalKey.random(AllureStageTest.class);
            // adapter-style step: ambient keyed start, keyed stop
            lifecycle.startStep(stepKey, new StepResult().setName("adapter step").setStatus(Status.PASSED));
            Allure.stage("phase inside adapter step");
            lifecycle.stopStep(stepKey);
        });

        final StepResult adapterStep = results.getTestResults().get(0).getSteps().get(0);
        assertThat(adapterStep.getName()).isEqualTo("adapter step");
        assertThat(adapterStep.getStage()).isEqualTo(Stage.FINISHED);

        final StepResult phase = adapterStep.getSteps().get(0);
        assertThat(phase.getName()).isEqualTo("phase inside adapter step");
        assertThat(phase.getStage()).isEqualTo(Stage.FINISHED);
        assertThat(phase.getStatus()).isEqualTo(Status.PASSED);
    }

    @Test
    void shouldSkipAmbientStepUpdateWhenStageIsCurrent() {
        final AllureResults results = runWithinTestContext(() -> {
            final AllureLifecycle lifecycle = Allure.getLifecycle();
            lifecycle.startStep(new StepResult().setName("outer step"));
            Allure.stage("open phase");
            // stages cannot be updated: the ambient update warns and does nothing
            lifecycle.updateStep(step -> step.setStatus(Status.FAILED));
            lifecycle.stopStep();
        });

        final StepResult outer = results.getTestResults().get(0).getSteps().get(0);
        assertThat(outer.getName()).isEqualTo("outer step");
        assertThat(outer.getStatus()).isNull();

        final StepResult phase = outer.getSteps().get(0);
        assertThat(phase.getName()).isEqualTo("open phase");
        // the stage closed gracefully, untouched by the skipped ambient update
        assertThat(phase.getStatus()).isEqualTo(Status.PASSED);
        assertThat(phase.getStage()).isEqualTo(Stage.FINISHED);
    }

    @Test
    void shouldUpdateOwnStepByKeyWhileStageIsOpen() {
        final AllureResults results = runWithinTestContext(() -> {
            final AllureLifecycle lifecycle = Allure.getLifecycle();
            final AllureExternalKey stepKey = AllureExternalKey.random(AllureStageTest.class);
            lifecycle.startStep(stepKey, new StepResult().setName("outer step"));
            Allure.stage("open phase");
            // a caller finishing its own step addresses it by key — stages cannot interfere
            lifecycle.updateStep(stepKey, step -> step.setStatus(Status.FAILED));
            lifecycle.stopStep(stepKey);
        });

        final StepResult outer = results.getTestResults().get(0).getSteps().get(0);
        assertThat(outer.getName()).isEqualTo("outer step");
        assertThat(outer.getStatus()).isEqualTo(Status.FAILED);

        final StepResult phase = outer.getSteps().get(0);
        assertThat(phase.getStatus()).isEqualTo(Status.PASSED);
        assertThat(phase.getStage()).isEqualTo(Stage.FINISHED);
    }
}
