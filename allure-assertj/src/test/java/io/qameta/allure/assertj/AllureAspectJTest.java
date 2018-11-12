package io.qameta.allure.assertj;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
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

    @Test
    public void shouldHandleNullableObject() {
        assertThat((Object) null).as("Nullable object").isNull();
    }
}