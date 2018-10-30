package io.qameta.allure;

import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResultsWriterStub;
import io.qameta.allure.testdata.DummyCard;
import io.qameta.allure.testdata.DummyEmail;
import io.qameta.allure.testdata.DummyUser;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author sskorol (Sergey Korol)
 */
class StepsTest {

    @Test
    void shouldTransformPlaceholdersToPropertyValues() {
        final AllureResultsWriterStub results = runStep(() -> {
            final DummyEmail[] emails = new DummyEmail[]{
                    new DummyEmail("test1@email.com", asList("txt", "png")),
                    new DummyEmail("test2@email.com", asList("jpg", "mp4")),
                    null
            };
            final DummyCard card = new DummyCard("1111222233334444");

            loginWith(new DummyUser(emails, "12345678", card), true);
        });

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("\"[test1@email.com, test2@email.com, null]\"," +
                        " \"[{address='test1@email.com', attachments='[txt, png]'}," +
                        " {address='test2@email.com', attachments='[jpg, mp4]'}," +
                        " null]\"," +
                        " \"[[txt, png], [jpg, mp4], null]\"," +
                        " \"12345678\", \"{}\","
                        + " \"1111222233334444\", \"{missing}\", true");
    }

    @Test
    void shouldNotFailOnSpecialSymbolsInNameString() {
        final AllureResultsWriterStub results = runStep(() -> checkData("$abc"));
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("TestData = $abc");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldSupportArrayParameters() {
        final AllureResultsWriterStub results = runStep(() -> step("a", "b"));

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("parameters", "[a, b]")
                );
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldSupportParallelStepsRun() {
        final AllureResultsWriterStub results = runStep(() -> {
            Thread[] threads = {
                    new Thread(this::outerStep),
                    new Thread(this::outerStep),
                    new Thread(this::outerStep)
            };
            for (Thread thread : threads) {
                thread.start();
            }
            try {
                for (Thread thread : threads) {
                    thread.join();
                }
            } catch (InterruptedException ignored) {
            }
        });

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(
                        StepResult::getName,
                        step -> step.getSteps().stream().map(StepResult::getName).collect(Collectors.toList())
                )
                .containsOnly(
                        tuple("outerStep", asList("innerStep", "innerStep", "innerStep"))
                );
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    @Step("\"{user.emails.address}\", \"{user.emails}\", \"{user.emails.attachments}\", \"{user.password}\", \"{}\"," +
            " \"{user.card.number}\", \"{missing}\", {staySignedIn}")
    private void loginWith(final DummyUser user, final boolean staySignedIn) {
    }

    @Step("TestData = {value}")
    public void checkData(@SuppressWarnings("unused") final String value) {
    }

    @Step
    public void step(@SuppressWarnings("unused") final String... parameters) {
    }

    @Step
    private void outerStep() {
        for (int i = 0; i < 3; i++) {
            innerStep();
        }
    }

    @Step
    private void innerStep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }

    public static AllureResultsWriterStub runStep(final Runnable runnable) {
        final AllureResultsWriterStub results = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(results);
        StepsAspects.setLifecycle(lifecycle);
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        try {
            runnable.run();
        } finally {
            lifecycle.stopTestCase(uuid);
            lifecycle.writeTestCase(uuid);
            StepsAspects.setLifecycle(Allure.getLifecycle());
        }
        return results;
    }
}
