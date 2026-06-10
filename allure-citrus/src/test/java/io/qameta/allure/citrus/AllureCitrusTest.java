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
package io.qameta.allure.citrus;

import com.consol.citrus.Citrus;
import com.consol.citrus.CitrusContext;
import com.consol.citrus.TestCase;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.actions.FailAction;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.design.DefaultTestDesigner;
import com.consol.citrus.dsl.design.TestDesigner;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
        assertThat(results.getTestResults().get(0).getTitlePath())
                .containsExactly("com", "consol", "citrus", "dsl", "design", "DefaultTestDesigner");
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
        designer.action(FailAction.Builder.fail("failed by design").build());

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
        designer.action(FailAction.Builder.fail("failed by design").build());

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
        final CitrusContext citrusContext = CitrusContext.create();
        final AllureResultsWriterStub resultsWriterStub = new AllureResultsWriterStub();
        final AllureLifecycle defaultLifecycle = Allure.getLifecycle();
        final AllureLifecycle lifecycle = new AllureLifecycle(resultsWriterStub);
        final AllureCitrus allureCitrus = new AllureCitrus(lifecycle);
        final Citrus citrus = Citrus.newInstance(() -> citrusContext);
        final TestContext testContext = citrusContext.createTestContext();
        testContext.getTestListeners().addTestListener(allureCitrus);
        testContext.getTestActionListeners().addTestActionListener(allureCitrus);
        try {
            Allure.setLifecycle(lifecycle);
            testDesigner.setTestContext(testContext);
            final TestCase testCase = testDesigner.getTestCase();

            Throwable failure = null;
            try {
                citrus.run(testCase, testContext);
            } catch (Exception | AssertionError e) {
                failure = e;
            }
            try {
                testCase.finish(testContext);
            } catch (Exception | AssertionError e) {
                if (failure == null) {
                    failure = e;
                }
            }
            if (failure != null && resultsWriterStub.getTestResults().isEmpty()) {
                throw new IllegalStateException("Citrus test execution failed before Allure received test events", failure);
            }
        } catch (Exception e) {
            if (resultsWriterStub.getTestResults().isEmpty()) {
                throw new IllegalStateException("Citrus test execution failed before Allure received test events", e);
            }
        } finally {
            Allure.setLifecycle(defaultLifecycle);
            citrus.close();
        }

        return resultsWriterStub;
    }
}
