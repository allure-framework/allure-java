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
