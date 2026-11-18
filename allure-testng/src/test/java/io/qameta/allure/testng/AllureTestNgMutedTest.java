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
package io.qameta.allure.testng;

import io.qameta.allure.model.Status;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import io.qameta.allure.testfilter.TestPlanV1_0;
import io.qameta.allure.testng.samples.MutedPlanAfterFailure;
import io.qameta.allure.testng.samples.MutedPlanDependency;
import io.qameta.allure.testng.samples.MutedPlanTests;
import org.junit.jupiter.api.Test;
import org.testng.TestNG;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IsolatedLifecycle
class AllureTestNgMutedTest {
    private TestPlanV1_0 plan() {
        return new TestPlanV1_0().setMutedTests(
                List.of(
                        new TestPlanV1_0.TestCase().setId("muted"),
                        new TestPlanV1_0.TestCase().setId("unmatched")
                                .setSelector(MutedPlanTests.class.getCanonicalName() + ".repeated")
                )
        );
    }

    @Test
    void shouldSkipRealInvocationsWithoutExecutingBodies() {
        final AllureResults results = run(plan().setTests(List.of()));

        assertThat(results.getTestResults()).hasSize(6);
        assertThat(results.getTestResults()).filteredOn(r -> r.getStatusDetails().isMuted()).hasSize(4)
                .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(Status.SKIPPED));
        assertThat(results.getTestResults()).filteredOn("name", "parameterized").hasSize(2)
                .allSatisfy(r -> assertThat(r.getParameters()).hasSize(1));
        assertThat(results.getTestResults()).filteredOn("name", "ordinarySkip").singleElement()
                .satisfies(r -> assertThat(r.getStatusDetails().isMuted()).isFalse());
        assertThat(results.getTestResults()).filteredOn("name", "control").singleElement()
                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(Status.PASSED));
    }

    @Test
    void shouldCompleteAllMutedScopeWithoutFailures() {
        final TestPlanV1_0 plan = plan().setTests(
                List.of(
                        new TestPlanV1_0.TestCase().setId("muted"),
                        new TestPlanV1_0.TestCase().setId("repeated")
                )
        );

        final AllureResults results = run(plan);

        assertThat(results.getTestResults()).hasSize(4)
                .allSatisfy(r -> {
                    assertThat(r.getStatus()).isEqualTo(Status.SKIPPED);
                    assertThat(r.getStatusDetails().isMuted()).isTrue();
                });
    }

    @Test
    void shouldPreserveDependencySkips() {
        final AllureResults results = run(plan(), false, MutedPlanDependency.class);

        assertThat(results.getTestResults()).hasSize(2)
                .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(Status.SKIPPED));
        assertThat(results.getTestResults()).filteredOn("name", "dependent").singleElement()
                .satisfies(r -> assertThat(r.getStatusDetails().isMuted()).isFalse());
    }

    @Test
    void shouldNotHideRealCleanupFailure() {
        final AllureResults results = run(plan(), true, MutedPlanAfterFailure.class);

        assertThat(results.getTestResultContainers()).flatExtracting(c -> c.getAfters())
                .anySatisfy(f -> assertThat(f.getStatus()).isEqualTo(Status.FAILED));
    }

    private AllureResults run(final TestPlanV1_0 plan) {
        return run(plan, false, MutedPlanTests.class);
    }

    private AllureResults run(final TestPlanV1_0 plan, final boolean hasFailure, final Class<?>... classes) {
        return RunUtils.runTests(lifecycle -> {
            final TestNG runner = new TestNG(false);
            runner.addListener(new AllureTestNg(lifecycle, new AllureTestNgTestFilter(plan)));
            runner.setTestClasses(classes);
            runner.setOutputDirectory("build/test-output");
            runner.run();
            assertThat(runner.hasFailure()).isEqualTo(hasFailure);
        });
    }
}
