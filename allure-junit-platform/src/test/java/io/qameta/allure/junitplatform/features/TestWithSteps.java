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