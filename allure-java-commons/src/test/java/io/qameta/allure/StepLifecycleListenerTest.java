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
package io.qameta.allure;

import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.listener.LifecycleNotifier;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final LifecycleNotifier notifier = new LifecycleNotifier(
                emptyList(),
                emptyList(),
                emptyList(),
                singletonList(listener)
        );
        final AllureLifecycle lifecycle = new AllureLifecycle(writer, notifier);

        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        final AllureLifecycle cached = Allure.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            StepsAspects.setLifecycle(lifecycle);

            lifecycle.scheduleTestCase(result);
            lifecycle.startTestCase(uuid);

            Stream.of(steps).forEach(step -> {
                final String stepUuid = UUID.randomUUID().toString();
                lifecycle.startStep(stepUuid, new StepResult().setName(step).setStatus(Status.PASSED));
                lifecycle.stopStep(stepUuid);
            });
        } catch (Throwable e) {
            lifecycle.updateTestCase(uuid, testResult -> {
                getStatus(e).ifPresent(testResult::setStatus);
                getStatusDetails(e).ifPresent(testResult::setStatusDetails);

            });
        } finally {
            lifecycle.stopTestCase(uuid);
            lifecycle.writeTestCase(uuid);

            Allure.setLifecycle(cached);
            StepsAspects.setLifecycle(cached);
        }

        return writer;
    }
}
