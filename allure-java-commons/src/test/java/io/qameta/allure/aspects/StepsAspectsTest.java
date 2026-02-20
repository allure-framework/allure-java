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
package io.qameta.allure.aspects;

import io.qameta.allure.Issue;
import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.testdata.DummyCard;
import io.qameta.allure.testdata.DummyEmail;
import io.qameta.allure.testdata.DummyUser;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static io.qameta.allure.test.TestData.randomString;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings({"Convert2MethodRef", "SameParameterValue", "unused"})
class StepsAspectsTest {

    @Test
    void shouldCreateSteps() {
        final AllureResults results = runWithinTestContext(() -> {
            simpleStep();
            simpleStep();
        });

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("Simple step", "Simple step");
    }

    @Test
    void shouldAllowMethodReferences() {
        final AllureResults results = runWithinTestContext(this::simpleStep);

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("Simple step");
    }

    @Test
    void shouldUseMethodName() {
        final AllureResults results = runWithinTestContext(() -> stepWithDefaultName());

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("stepWithDefaultName");
    }

    @Issue("123")
    @Test
    void shouldProcessEnumPrams() {
        final AllureResults results = runWithinTestContext(() -> paramSteps(FirstParam.PARAM_1, SecondParam.PARAM_2));

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("paramSteps");
    }

    @Test
    void shouldUseMethodPlaceholder() {
        final AllureResults results = runWithinTestContext(this::stepWithMethodPlaceholder);

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("Method stepWithMethodPlaceholder");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldAddParams() {
        final AllureResults results = runWithinTestContext(() -> stepWithParams("first", "second"));

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("a", "first"),
                        tuple("b", "second")
                );
    }

    @Test
    void shouldUseOldStyleParams() {
        final AllureResults results = runWithinTestContext(() -> stepWithOldStylePattern("hello", "world"));

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("go to hello then to world");
    }

    @Test
    void shouldUseNewStyleParams() {
        final AllureResults results = runWithinTestContext(() -> stepWithOldStylePattern("hello", "world"));

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("go to hello then to world");
    }

    @Test
    void shouldOverrideMethodPlaceholderByParameter() {
        final AllureResults results = runWithinTestContext(() -> stepWithMethodParameter("brain"));

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("use brain");
    }

    @Test
    void shouldCatchErrorsInToString() {
        final AllureResults results = runWithinTestContext(() -> stepWithBadParameter(new NpeToString()));

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("npe param <NPE>");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldProcessExceptions() {
        final AllureResults results = runWithinTestContext(() -> stepWithException());

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(
                        StepResult::getName,
                        StepResult::getStatus,
                        step -> step.getStatusDetails().getMessage()
                )
                .containsExactly(
                        tuple("stepWithException", Status.BROKEN, "some exception")
                );
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldProcessAssertions() {
        final AllureResults results = runWithinTestContext(() -> stepWithAssertion());

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(
                        StepResult::getName,
                        StepResult::getStatus,
                        step -> step.getStatusDetails().getMessage()
                )
                .containsExactly(
                        tuple("stepWithAssertion", Status.FAILED, "some assertion")
                );
    }

    @Test
    void shouldSupportVarargs() {
        final AllureResults results = runWithinTestContext(() -> stepWithVarargs("first", "second", "third"));

        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("Step [first, second, third]");
    }

    @Test
    void shouldTransformPlaceholdersToPropertyValues() {
        final AllureResults results = runWithinTestContext(() -> {
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
        final AllureResults results = runWithinTestContext(() -> checkData("$abc"));
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("TestData = $abc");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldSupportArrayParameters() {
        final AllureResults results = runWithinTestContext(() -> step("a", "b"));

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
        final AllureResults results = runWithinTestContext(() -> {
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

    @Test
    void shouldProcessParamAnnotation() {
        final String p1 = randomString(10);
        final String p2 = randomString(10);
        final String p3 = randomString(10);
        final String p4 = randomString(10);
        final AllureResults results = runWithinTestContext(() -> stepWithParamAnnotation(p1, p2, p3, p4));

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded, Parameter::getMode)
                .containsExactlyInAnyOrder(
                        tuple("Named", p1, false, Parameter.Mode.DEFAULT),
                        tuple("Excluded", p2, true, Parameter.Mode.DEFAULT),
                        tuple("Masked", p3, false, Parameter.Mode.MASKED),
                        tuple("Masked", p4, false, Parameter.Mode.HIDDEN)
                );

    }

    @Step
    void stepWithParamAnnotation(
            @Param("Named") final String named,
            @Param(name = "Excluded", excluded = true) final String excluded,
            @Param(name = "Masked", mode = Parameter.Mode.MASKED) final String masked,
            @Param(name = "Masked", mode = Parameter.Mode.HIDDEN) final String hidden) {
    }

    @Step
    void stepWithDefaultName() {
    }

    @Step("Simple step")
    void simpleStep() {
    }

    @Step("Method {method}")
    void stepWithMethodPlaceholder() {
    }

    @Step
    void stepWithParams(final String a, final String b) {
    }

    @Step("go to {0} then to {1}")
    void stepWithOldStylePattern(final String first, final String second) {
    }

    @Step("go to {first} then to {second}")
    void stepWithNewStylePattern(final String first, final String second) {
    }

    @Step("use {method}")
    void stepWithMethodParameter(final String method) {
    }

    @Step("npe param {0}")
    void stepWithBadParameter(final NpeToString npe) {
    }

    @Step
    void stepWithException() {
        throw new RuntimeException("some exception");
    }

    @Step
    void stepWithAssertion() {
        throw new AssertionError("some assertion");
    }

    @Step("Step {values}")
    void stepWithVarargs(final String... values) {
    }

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

    @SuppressWarnings("unused")
    @SafeVarargs
    @Step
    public final <T extends Params> void paramSteps(T... param) {
    }

    enum FirstParam implements Params {
        PARAM_1
    }

    enum SecondParam implements Params {
        PARAM_2
    }

    interface Params {
    }

    class NpeToString {
        @Override
        public String toString() {
            throw new NullPointerException("hey");
        }
    }


}
