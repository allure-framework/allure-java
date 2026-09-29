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
import com.consol.citrus.TestCaseMetaInfo;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.actions.FailAction;
import com.consol.citrus.container.SequenceAfterSuite;
import com.consol.citrus.container.SequenceBeforeSuite;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.design.DefaultTestDesigner;
import com.consol.citrus.dsl.design.TestDesigner;
import com.consol.citrus.report.TestReporters;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.model.GlobalError;
import io.qameta.allure.model.Globals;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static io.qameta.allure.test.AllureTestCommonsUtils.attach;
import static io.qameta.allure.util.ResultsUtils.md5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
@SuppressWarnings("unchecked")
@IsolatedLifecycle
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

    @AllureFeatures.Base
    @Test
    void shouldSetTestClassLabelForClassBackedTests() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Simple test");

        final AllureResults results = run(designer);
        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(tuple("testClass", "com.consol.citrus.dsl.design.DefaultTestDesigner"));

        // the test method is not a known code location for citrus test cases
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName)
                .doesNotContain("testMethod");
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
        assertThat(results.getGlobals()).isEmpty();
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
        assertThat(results.getGlobals()).isEmpty();
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
        assertThat(results.getGlobals()).isEmpty();
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

    @AllureFeatures.History
    @AllureFeatures.Parameters
    @Test
    void shouldCalculateIdsFromFinalNativeAndRuntimeParameters() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Runtime parameters");
        designer.variable("native", "example");
        designer.action(new AbstractTestAction() {
            @Override
            public void doExecute(final TestContext context) {
                Allure.parameter("runtime", "value");
                Allure.parameter("excluded", "ignored", true);
            }
        });

        final AllureResults results = run(designer);
        final TestResult testResult = results.getTestResults().get(0);
        assertThat(testResult.getParameters())
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded)
                .containsExactlyInAnyOrder(
                        tuple("native", "example", null),
                        tuple("runtime", "value", null),
                        tuple("excluded", "ignored", true)
                );

        final String fullName = DefaultTestDesigner.class.getName() + ".Runtime parameters";
        assertThat(testResult.getTestCaseId())
                .isEqualTo(md5(fullName));
        assertThat(testResult.getHistoryId())
                .isEqualTo(md5(md5(fullName) + "native" + "example" + "runtime" + "value"));
    }

    @AllureFeatures.SkippedTests
    @AllureFeatures.History
    @Test
    void shouldReportDisabledTestsWithIds() {
        final DefaultTestDesigner designer = new DefaultTestDesigner();
        designer.name("Disabled test");
        designer.variable("native", "value");
        designer.status(TestCaseMetaInfo.Status.DISABLED);

        final AllureResults results = run(designer);
        final String fullName = DefaultTestDesigner.class.getName() + ".Disabled test";
        final String testCaseId = md5(fullName);
        assertThat(results.getTestResults())
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getStatus()).isEqualTo(Status.SKIPPED);
                    assertThat(result.getStage()).isEqualTo(Stage.FINISHED);
                    assertThat(result.getTestCaseId()).isEqualTo(testCaseId);
                    assertThat(result.getHistoryId()).isEqualTo(md5(testCaseId + "native" + "value"));
                });
    }

    /**
     * A suite setup failure is reported as a global error with its phase, exception details, and timestamp.
     */
    @Test
    @Description
    void shouldReportBeforeSuiteFailureAsGlobalError() {
        final CitrusContext context = suiteContext()
                .beforeSuite(
                        SequenceBeforeSuite.Builder.beforeSuite()
                                .actions(FailAction.Builder.fail("setup action failed"))
                                .build()
                )
                .build();
        final long started = System.currentTimeMillis();

        final AllureResults results = runSuite(
                context, citrus -> assertThatThrownBy(() -> citrus.beforeSuite("suite"))
                        .isInstanceOf(AssertionError.class)
                        .hasRootCauseMessage("setup action failed")
        );

        final List<Globals> globals = results.getGlobals();
        assertThat(globals).hasSize(1);
        final List<GlobalError> errors = globals.get(0).getErrors();
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage()).contains("Citrus suite setup failed", "setup action failed");
        assertThat(error.getTrace()).contains("setup action failed", "com.consol.citrus.actions.FailAction");
        assertThat(error.getTimestamp()).isBetween(started, System.currentTimeMillis());
    }

    /**
     * A cleanup sequence failure is reported as a global error with the aggregate exception supplied by Citrus.
     */
    @Test
    @Description
    void shouldReportAfterSuiteFailureAsGlobalError() {
        final CitrusContext context = suiteContext()
                .afterSuite(
                        SequenceAfterSuite.Builder.afterSuite()
                                .actions(FailAction.Builder.fail("cleanup action failed"))
                                .build()
                )
                .build();

        final AllureResults results = runSuite(context, citrus -> {
            citrus.beforeSuite("suite");
            assertThatThrownBy(() -> citrus.afterSuite("suite"))
                    .isInstanceOf(AssertionError.class)
                    .hasRootCauseMessage("Error in after suite");
        });

        final List<Globals> globals = results.getGlobals();
        assertThat(globals).hasSize(1);
        final List<GlobalError> errors = globals.get(0).getErrors();
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage()).contains("Citrus suite teardown failed", "Error in after suite");
        assertThat(error.getTrace()).contains("Error in after suite", "com.consol.citrus.container.SequenceAfterSuite");
        assertThat(error.getTimestamp()).isPositive();
    }

    /**
     * Setup and teardown failures in the same suite are reported as separate global errors identifying each phase.
     */
    @Test
    @Description
    void shouldReportBothSuiteFailuresAsGlobalErrors() {
        final CitrusContext context = suiteContext()
                .beforeSuite(
                        SequenceBeforeSuite.Builder.beforeSuite()
                                .actions(FailAction.Builder.fail("setup action failed"))
                                .build()
                )
                .afterSuite(
                        SequenceAfterSuite.Builder.afterSuite()
                                .actions(FailAction.Builder.fail("cleanup action failed"))
                                .build()
                )
                .build();

        final AllureResults results = runSuite(
                context, citrus -> assertThatThrownBy(() -> citrus.beforeSuite("suite"))
                        .isInstanceOf(AssertionError.class)
                        .hasRootCauseMessage("Error in after suite")
        );

        final List<Globals> globals = results.getGlobals();
        assertThat(globals).hasSize(2);
        final List<GlobalError> errors = globals.stream().flatMap(value -> value.getErrors().stream()).toList();
        assertThat(errors).hasSize(2);
        final GlobalError setupError = errors.get(0);
        final GlobalError teardownError = errors.get(1);
        assertThat(setupError.getMessage()).contains("Citrus suite setup failed", "setup action failed");
        assertThat(teardownError.getMessage()).contains("Citrus suite teardown failed", "Error in after suite");
    }

    /**
     * Suite failure callbacks with no throwable report their phase and timestamp on the supplied lifecycle.
     */
    @Test
    @Description
    void shouldReportSuiteFailuresWithoutThrowable() {
        final AllureResults results = reportSuiteFailures(listener -> {
            listener.onStartFailure(null);
            listener.onFinishFailure(null);
        });

        final List<Globals> globals = results.getGlobals();
        assertThat(globals).hasSize(2);
        final List<GlobalError> errors = globals.stream().flatMap(value -> value.getErrors().stream()).toList();
        assertThat(errors).extracting(GlobalError::getMessage).containsExactly(
                "Citrus suite setup failed",
                "Citrus suite teardown failed"
        );
        assertThat(errors).allSatisfy(error -> {
            assertThat(error.getTrace()).isNull();
            assertThat(error.getTimestamp()).isPositive();
        });
    }

    /**
     * Suite failure callbacks report exception traces and comparison values on the supplied lifecycle.
     */
    @Test
    @Description
    void shouldReportSuiteFailureComparisonDetails() {
        final AssertionFailedError cause = new AssertionFailedError(
                "suite comparison failed", "expected value", "actual value"
        );

        final AllureResults results = reportSuiteFailures(listener -> listener.onFinishFailure(cause));

        final List<Globals> globals = results.getGlobals();
        assertThat(globals).hasSize(1);
        final List<GlobalError> errors = globals.get(0).getErrors();
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage()).contains("Citrus suite teardown failed", "suite comparison failed");
        assertThat(error.getTrace()).contains("org.opentest4j.AssertionFailedError: suite comparison failed");
        assertThat(error.getExpected()).isEqualTo(cause.getExpected().toString());
        assertThat(error.getActual()).isEqualTo(cause.getActual().toString());
    }

    private CitrusContext.Builder suiteContext() {
        // Only Allure output is needed; disable Citrus HTML and JUnit report generation.
        return new CitrusContext.Builder().testReporters(new TestReporters());
    }

    @Step("Run Citrus suite lifecycle")
    private AllureResults runSuite(final CitrusContext context, final Consumer<Citrus> execution) {
        return RunUtils.runTests(lifecycle -> {
            final Citrus citrus = Citrus.newInstance(() -> context);
            citrus.addTestSuiteListener(new AllureCitrus(lifecycle));
            try {
                execution.accept(citrus);
            } finally {
                citrus.close();
            }
        });
    }

    @Step("Report Citrus suite failure callbacks")
    private AllureResults reportSuiteFailures(final Consumer<AllureCitrus> notification) {
        final AllureResultsWriterStub results = new AllureResultsWriterStub();
        final AllureCitrus listener = new AllureCitrus(new AllureLifecycle(results));
        try {
            notification.accept(listener);
        } finally {
            attach(results);
        }
        return results;
    }

    @Step("Run test case {testDesigner}")
    private AllureResults run(final TestDesigner testDesigner) {
        // a failing citrus test is a valid outcome under test — only fail the harness when
        // the failure prevented Allure from receiving any test events at all
        final AllureResultsWriterStub[] writerRef = new AllureResultsWriterStub[1];
        return RunUtils.runTests(
                writer -> {
                    writerRef[0] = (AllureResultsWriterStub) writer;
                    return new AllureLifecycle(writer);
                },
                lifecycle -> {
                    final CitrusContext citrusContext = CitrusContext.create();
                    final AllureCitrus allureCitrus = new AllureCitrus(lifecycle);
                    final Citrus citrus = Citrus.newInstance(() -> citrusContext);
                    final TestContext testContext = citrusContext.createTestContext();
                    testContext.getTestListeners().addTestListener(allureCitrus);
                    testContext.getTestActionListeners().addTestActionListener(allureCitrus);
                    try {
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
                        if (failure != null && writerRef[0].getTestResults().isEmpty()) {
                            throw new IllegalStateException(
                                    "Citrus test execution failed before Allure received test events", failure
                            );
                        }
                    } catch (Exception e) {
                        if (writerRef[0].getTestResults().isEmpty()) {
                            throw new IllegalStateException(
                                    "Citrus test execution failed before Allure received test events", e
                            );
                        }
                    } finally {
                        citrus.close();
                    }
                }
        );
    }
}
