package io.qameta.allure.aspects;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.aspects.testdata.AllureResultsWriterStub;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class Allure1StepsAspectsTest {

    private AllureResultsWriterStub results;

    private AllureLifecycle lifecycle;

    @Before
    public void initLifecycle() {
        results = new AllureResultsWriterStub();
        lifecycle = new AllureLifecycle(results);
        Allure1StepsAspects.setLifecycle(lifecycle);
    }

    @Test
    public void shouldSetupStepTitleFromAnnotation() {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        stepWithTitleAndWithParameter("parameter value");

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

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
    public void shouldSetupStepTitleFromMethodSignature() {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        stepWithoutTitleAndWithParameter("parameter value");

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

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

    @Step
    public void stepWithoutTitleAndWithParameter(String parameter) {

    }

    @Step("step with title and parameter [{0}]")
    public void stepWithTitleAndWithParameter(String parameter) {
    }

}
