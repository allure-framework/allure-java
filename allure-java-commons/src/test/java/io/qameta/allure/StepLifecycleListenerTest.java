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

import io.qameta.allure.listener.LifecycleNotifier;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class StepLifecycleListenerTest {

    @Test
    void shouldExecuteBeforeStepStart() {
        final AtomicInteger executionCount = new AtomicInteger();
        final StepLifecycleListener listener = new StepLifecycleListener() {
            @Override
            public void beforeStepStart(final StepResult result) {
                executionCount.incrementAndGet();
            }
        };
        final AllureResults run = run(listener, "first", "second");

        assertThat(run.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("first", "second");

        assertThat(executionCount.get())
                .isEqualTo(2);
    }

    @Test
    void shouldExecuteAfterStepStart() {
        final AtomicInteger executionCount = new AtomicInteger();
        final StepLifecycleListener listener = new StepLifecycleListener() {
            @Override
            public void afterStepStart(final StepResult result) {
                executionCount.incrementAndGet();
                Allure.addAttachment("inner " + result.getName(), "some");
            }
        };
        final AllureResults run = run(listener, "first", "second");

        assertThat(run.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("first", "second");

        assertThat(run.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .filteredOn("name", "first")
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .containsExactly("inner first");

        assertThat(run.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .filteredOn("name", "second")
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .containsExactly("inner second");

        assertThat(executionCount.get())
                .isEqualTo(2);
    }

    @Issue("177")
    @Test
    void shouldExecuteBeforeStepStop() {
        final AtomicInteger executionCount = new AtomicInteger();
        final StepLifecycleListener listener = new StepLifecycleListener() {
            @Override
            public void beforeStepStop(final StepResult result) {
                executionCount.incrementAndGet();
                Allure.addAttachment("inner " + result.getName(), "some");
            }
        };
        final AllureResults run = run(listener, "first", "second");

        assertThat(run.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("first", "second");

        assertThat(run.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .filteredOn("name", "first")
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .containsExactly("inner first");

        assertThat(run.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .filteredOn("name", "second")
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .containsExactly("inner second");

        assertThat(executionCount.get())
                .isEqualTo(2);
    }

    protected AllureResults run(final StepLifecycleListener listener, final String... steps) {
        return RunUtils.runWithinTestContext(
                writer -> new AllureLifecycle(
                        writer,
                        new LifecycleNotifier(List.of(), List.of(), List.of(), List.of(listener))
                ),
                () -> Stream.of(steps).forEach(step -> Allure.step(step, Status.PASSED))
        );
    }
}
