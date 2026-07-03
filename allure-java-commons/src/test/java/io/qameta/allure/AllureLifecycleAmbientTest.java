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

import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static io.qameta.allure.test.TestData.randomName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;

class AllureLifecycleAmbientTest {

    private AllureResultsWriter writer;
    private AllureLifecycle lifecycle;
    

    @BeforeEach
    void setUp() {
        writer = Mockito.mock(AllureResultsWriter.class);
        lifecycle = new AllureLifecycle(writer);
        
    }

    private AllureExternalKey startTest() {
        final AllureExternalKey testKey = AllureExternalKey.of(AllureLifecycleAmbientTest.class, "test");
        lifecycle.scheduleTest(testKey, new TestResult().setName(randomName()));
        lifecycle.startTest(testKey);
        return testKey;
    }

    @Test
    void shouldReportPresenceAndCurrentExecutable() {
        assertThat(lifecycle.getCurrentExecutableKey().isPresent()).isFalse();

        final AllureExternalKey testKey = startTest();

        assertThat(lifecycle.getCurrentExecutableKey().isPresent()).isTrue();
        assertThat(lifecycle.getCurrentExecutableKey()).hasValue(testKey);
        assertThat(lifecycle.getCurrentRootKey()).hasValue(testKey);
    }

    @Test
    void shouldStartKeylessStepUnderCurrentExecutable() {
        final AllureExternalKey testKey = startTest();

        lifecycle.startStep(new StepResult().setName("ambient step"));
        lifecycle.updateStep(step -> step.setName("renamed step"));
        lifecycle.stopStep();

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer).write(captor.capture());
        assertThat(captor.getValue().getSteps())
                .extracting(StepResult::getName)
                .containsExactly("renamed step");
    }

    @Test
    void shouldSetAndClearCurrent() {
        final AllureExternalKey testKey = startTest();

        lifecycle.clearCurrent();
        assertThat(lifecycle.getCurrentExecutableKey().isPresent()).isFalse();

        lifecycle.setCurrent(testKey);
        assertThat(lifecycle.getCurrentExecutableKey().isPresent()).isTrue();
        assertThat(lifecycle.getCurrentExecutableKey()).hasValue(testKey);
    }

    @Test
    void shouldLogInstantStepUnderExplicitParent() {
        final AllureExternalKey testKey = startTest();
        // manual form: no thread state required, safe from any thread
        lifecycle.clearCurrent();

        lifecycle.logStep(testKey, new StepResult().setName("instant step").setStatus(Status.PASSED));

        lifecycle.setCurrent(testKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer).write(captor.capture());
        final StepResult step = captor.getValue().getSteps().get(0);
        assertThat(step.getName()).isEqualTo("instant step");
        assertThat(step.getStatus()).isEqualTo(Status.PASSED);
        assertThat(step.getStop()).isNotNull();
    }

    @Test
    void shouldAddAttachmentStepUnderExplicitParent() {
        final AllureExternalKey testKey = startTest();
        // manual form: no thread state required, safe from any thread
        lifecycle.clearCurrent();

        lifecycle.addAttachmentStep(
                testKey,
                "late artifact",
                "text/plain",
                new ByteArrayInputStream("body".getBytes(StandardCharsets.UTF_8)),
                AttachmentOptions.empty()
        );

        lifecycle.setCurrent(testKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer).write(captor.capture());
        final StepResult step = captor.getValue().getSteps().get(0);
        assertThat(step.getName()).isEqualTo("late artifact");
        assertThat(step.getStatus()).isEqualTo(Status.PASSED);
        assertThat(step.getAttachments())
                .extracting("name")
                .containsExactly("late artifact");
    }

    @Test
    void shouldApplyCapturedKeyAfterContextIsLost() {
        final AllureExternalKey testKey = startTest();

        // capture the owner identity while the test is current
        final AllureExternalKey owner = lifecycle.getCurrentExecutableKey().orElseThrow();

        // simulate the reporting/flush happening with no current thread context
        lifecycle.clearCurrent();
        assertThat(lifecycle.getCurrentExecutableKey().isPresent()).isFalse();

        lifecycle.addAttachment(
                owner,
                "late",
                "text/plain",
                new ByteArrayInputStream("body".getBytes(StandardCharsets.UTF_8)),
                AttachmentOptions.empty()
        );

        lifecycle.setCurrent(testKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer).write(captor.capture());
        assertThat(captor.getValue().getAttachments())
                .extracting("name")
                .containsExactly("late");
    }
}
