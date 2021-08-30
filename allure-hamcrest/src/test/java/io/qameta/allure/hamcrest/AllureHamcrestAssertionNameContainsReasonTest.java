package io.qameta.allure.hamcrest;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;

/**
 * This tests should cover cases when reason string exists in assertion.
 */
@SuppressWarnings("all")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class AllureHamcrestAssertionNameContainsReasonTest {

    @Test
    void hamcrestAssertNameWithComment() {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(
                        "Business always likes something weird",
                        "TheBiscuit",
                        equalToIgnoringCase("thebiscuit")
                ),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly("assert \"TheBiscuit\" a string equal to \"thebiscuit\" ignoring case | Business always likes something weird");
    }

    @Test
    void hamcrestAssertNameWoComment() {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(
                        "TheBiscuit",
                        equalToIgnoringCase("thebiscuit")
                ),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly("assert \"TheBiscuit\" a string equal to \"thebiscuit\" ignoring case");
    }
}