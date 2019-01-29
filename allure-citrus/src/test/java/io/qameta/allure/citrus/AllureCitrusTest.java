package io.qameta.allure.citrus;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCase;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.actions.FailAction;
import com.consol.citrus.config.CitrusSpringConfig;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.design.DefaultTestDesigner;
import com.consol.citrus.dsl.design.TestDesigner;
import com.consol.citrus.report.TestActionListeners;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unchecked")
class AllureCitrusTest {

    @AllureFeatures.Base
    @Test
    void shouldSetName() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");

        final AllureResults results = run(designer);
        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactly("Simple test");
    }

    @AllureFeatures.PassedTests
    @Test
    void shouldSetStatus() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");

        final AllureResults results = run(designer);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactly(Status.PASSED);
    }

    @AllureFeatures.BrokenTests
    @Test
    void shouldSetBrokenStatus() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");
        designer.action(new FailAction());

        final AllureResults results = run(designer);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactly(Status.BROKEN);
    }

    @AllureFeatures.FailedTests
    @Test
    void shouldSetFailedStatus() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");
        designer.action(new AbstractTestAction() {
            @Override
            public void doExecute(final TestContext context) {
                assertThat(true).isFalse();
            }
        });

        final AllureResults results = run(designer);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactly(Status.FAILED);
    }

    @AllureFeatures.FailedTests
    @Test
    void shouldSetStatusDetails() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");
        designer.action(new FailAction().setMessage("failed by design"));

        final AllureResults results = run(designer);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStatusDetails)
                .extracting(StatusDetails::getMessage)
                .containsExactly("failed by design");
    }

    @AllureFeatures.Steps
    @Test
    void shouldAddSteps() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");
        designer.echo("a");
        designer.echo("b");
        designer.echo("c");

        final AllureResults results = run(designer);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("echo", "echo", "echo");
    }

    @AllureFeatures.Steps
    @Test
    void shouldAddAllureSteps() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");
        designer.action(new AbstractTestAction() {
            @Override
            public void doExecute(final TestContext context) {
                Allure.step("a");
                Allure.step("b");
                Allure.step("c");
            }
        });

        final AllureResults results = run(designer);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("a", "b", "c");
    }

    @AllureFeatures.Timings
    @Test
    void shouldSetStart() {
        final long before = Instant.now().toEpochMilli();
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");

        final AllureResults results = run(designer);
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.Timings
    @Test
    void shouldSetStop() {
        final long before = Instant.now().toEpochMilli();
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");

        final AllureResults results = run(designer);
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @AllureFeatures.Parameters
    @Test
    void shouldSetParameters() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");
        designer.variable("a", "first");
        designer.variable("b", 123L);

        final AllureResults results = run(designer);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactly(
                        tuple("a", "first"),
                        tuple("b", "123")
                );
    }

    @Step("Run test case {testDesigner}")
    private AllureResults run(final TestDesigner testDesigner) {
        final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
                CitrusSpringConfig.class, AllureCitrusConfig.class
        );
        final Citrus citrus = Citrus.newInstance(applicationContext);
        final TestContext testContext = citrus.createTestContext();

        final TestActionListeners listeners = applicationContext.getBean(TestActionListeners.class);
        final AllureLifecycle defaultLifecycle = Allure.getLifecycle();
        final AllureLifecycle lifecycle = applicationContext.getBean(AllureLifecycle.class);
        try {
            Allure.setLifecycle(lifecycle);
            final TestCase testCase = testDesigner.getTestCase();
            testCase.setTestActionListeners(listeners);

            citrus.run(testCase, testContext);
        } catch (Exception ignored) {
        } finally {
            Allure.setLifecycle(defaultLifecycle);
        }

        return applicationContext.getBean(AllureResultsWriterStub.class);
    }

    @Configuration
    public static class AllureCitrusConfig {

        @Bean
        public AllureResultsWriterStub resultsWriterStub() {
            return new AllureResultsWriterStub();
        }

        @Bean
        public AllureLifecycle allureLifecycle(final AllureResultsWriterStub stub) {
            return new AllureLifecycle(stub);
        }

        @Bean
        public AllureCitrus allureCitrus(final AllureLifecycle lifecycle) {
            return new AllureCitrus(lifecycle);
        }

    }
}