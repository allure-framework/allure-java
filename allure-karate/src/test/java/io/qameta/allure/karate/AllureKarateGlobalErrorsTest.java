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
package io.qameta.allure.karate;

import io.karatelabs.core.ScenarioResult;
import io.qameta.allure.Description;
import io.qameta.allure.model.GlobalError;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.qameta.allure.model.Status.BROKEN;
import static io.qameta.allure.model.Status.FAILED;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
class AllureKarateGlobalErrorsTest extends TestRunner {

    /**
     * A feature cleanup failure produces a global error identifying the feature, exception, and occurrence time.
     */
    @Test
    @Description
    void shouldReportAfterFeatureFailureAsGlobalError() {
        final long started = System.currentTimeMillis();
        final AllureResults results = run("classpath:testdata/global-errors/after-feature-failure.feature");

        final List<GlobalError> errors = globalErrors(results);
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage()).contains("after-feature-failure.feature", "feature cleanup failed");
        assertThat(error.getTrace()).contains("feature cleanup failed");
        assertThat(error.getTimestamp()).isBetween(started, System.currentTimeMillis());
    }

    /**
     * A scenario failure does not prevent the feature's cleanup failure from being reported as a global error.
     */
    @Test
    @Description
    void shouldReportFeatureCleanupAfterScenarioFailure() {
        final AllureResults results = run("classpath:testdata/global-errors/after-failed-scenario.feature");

        final List<GlobalError> errors = globalErrors(results);
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage()).contains("after-failed-scenario.feature", "cleanup after failure");
        assertThat(error.getTrace()).contains("cleanup after failure");
    }

    /**
     * Concurrently executed features each report their own cleanup error once, with the correct feature identity.
     */
    @Test
    @Description
    void shouldKeepParallelFeatureErrorsIsolated() {
        final AllureResults results = run(
                4,
                "classpath:testdata/global-errors/after-feature-failure.feature",
                "classpath:testdata/global-errors/after-failed-scenario.feature"
        );

        final List<GlobalError> errors = globalErrors(results);
        assertThat(errors).hasSize(2);
        assertThat(errors).anySatisfy(
                error -> assertThat(error.getMessage()).contains("after-feature-failure.feature", "feature cleanup failed")
        );
        assertThat(errors).anySatisfy(
                error -> assertThat(error.getMessage()).contains("after-failed-scenario.feature", "cleanup after failure")
        );
    }

    /**
     * Scenario-outline cleanup and feature cleanup failures are both reported as global errors.
     */
    @Test
    @Description
    void shouldReportEachSharedHookFailure() {
        final AllureResults results = run("classpath:testdata/global-errors/shared-hooks-failure.feature");

        final List<GlobalError> errors = globalErrors(results);
        assertThat(errors).hasSize(2);
        assertThat(errors).allSatisfy(error -> {
            assertThat(error.getMessage()).contains("shared-hooks-failure.feature");
            assertThat(error.getTimestamp()).isPositive();
        });
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.getMessage()).contains("outline cleanup failed");
            assertThat(error.getTrace()).contains("outline cleanup failed");
        });
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.getMessage()).contains("feature cleanup failed");
            assertThat(error.getTrace()).contains("feature cleanup failed");
        });
    }

    /**
     * Feature cleanup errors honor report suppression: safe feature identity remains, but exception details are hidden.
     */
    @Test
    @Description
    void shouldRedactSuppressedFeatureCleanupFailure() {
        final AllureResults results = run("classpath:testdata/global-errors/suppressed-cleanup-failure.feature");

        final List<GlobalError> errors = globalErrors(results);
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage())
                .contains("suppressed-cleanup-failure.feature", ScenarioResult.SUPPRESSED_FAILURE_MESSAGE)
                .doesNotContain("private-cleanup-value");
        assertThat(error.getTrace()).isNull();
        assertThat(error.getActual()).isNull();
        assertThat(error.getExpected()).isNull();
        assertThat(error.getTimestamp()).isPositive();
    }

    /**
     * A visible caller with a suppressed callee failure keeps its global cleanup error details redacted.
     */
    @Test
    @Description
    void shouldRedactFeatureCleanupAfterSuppressedCalledFailure() {
        final AllureResults results = run("classpath:testdata/global-errors/suppressed-called-cleanup-failure.feature");

        final List<GlobalError> errors = globalErrors(results);
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage())
                .contains("suppressed-called-cleanup-failure.feature", ScenarioResult.SUPPRESSED_FAILURE_MESSAGE)
                .doesNotContain("private-caller-cleanup-value");
        assertThat(error.getTrace()).isNull();
        assertThat(error.getActual()).isNull();
        assertThat(error.getExpected()).isNull();
    }

    /**
     * Example preparation failures produce a global error with the feature identity and original exception details.
     */
    @Test
    @Description
    void shouldReportFeaturePreparationFailureAsGlobalError() {
        final AllureResults results = run("classpath:testdata/global-errors/example-preparation-failure.feature");

        final List<GlobalError> errors = globalErrors(results);
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage()).contains("example-preparation-failure.feature", "example preparation failed");
        assertThat(error.getTrace()).contains("example preparation failed");
        assertThat(error.getTimestamp()).isPositive();
    }

    /**
     * Report suppression also protects errors raised while preparing examples before scenario execution begins.
     */
    @Test
    @Description
    void shouldRedactSuppressedFeaturePreparationFailure() {
        final AllureResults results = run("classpath:testdata/global-errors/suppressed-preparation-failure.feature");

        final List<GlobalError> errors = globalErrors(results);
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage())
                .contains("suppressed-preparation-failure.feature", ScenarioResult.SUPPRESSED_FAILURE_MESSAGE)
                .doesNotContain("private-preparation-value");
        assertThat(error.getTrace()).isNull();
        assertThat(error.getActual()).isNull();
        assertThat(error.getExpected()).isNull();
    }

    /**
     * Failures in callonce, callSingle, and setup scenarios remain scenario failures, not global errors.
     */
    @Test
    @Description
    void shouldKeepSharedSetupFailuresOnScenarioResults() {
        final AllureResults results = run("classpath:testdata/global-errors/shared-setup-failures.feature");

        final TestResult callonce = results.getTestResultByName("Callonce setup failure");
        final TestResult callSingle = results.getTestResultByName("CallSingle setup failure");
        final TestResult setup = results.getTestResultByName("Prepare shared data");
        assertThat(List.of(callonce, callSingle, setup)).allSatisfy(result -> {
            assertThat(result.getStatus()).isIn(FAILED, BROKEN);
            assertThat(result.getStatusDetails().getMessage()).contains("shared setup failed");
        });
        assertThat(results.getGlobals()).isEmpty();
    }

    /**
     * A feature cleanup error is global even when scenario cleanup also fails; per-scenario hooks are not global errors.
     */
    @Test
    @Description
    void shouldReportOnlyFeatureHookFailuresAsGlobals() {
        final AllureResults results = run("classpath:testdata/global-errors/scenario-and-feature-hooks-failure.feature");

        final List<GlobalError> errors = globalErrors(results);
        assertThat(errors).hasSize(1);
        final GlobalError error = errors.get(0);
        assertThat(error.getMessage()).contains("scenario-and-feature-hooks-failure.feature", "feature cleanup failed");
        assertThat(error.getTrace()).contains("feature cleanup failed");
    }

    private List<GlobalError> globalErrors(final AllureResults results) {
        return results.getGlobals().stream().flatMap(globals -> globals.getErrors().stream()).toList();
    }
}
