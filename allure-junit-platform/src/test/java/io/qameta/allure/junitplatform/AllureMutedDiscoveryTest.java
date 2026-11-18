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
package io.qameta.allure.junitplatform;

import io.qameta.allure.junitplatform.features.MutedPlanTests;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import io.qameta.allure.testfilter.TestPlanV1_0;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IsolatedLifecycle
class AllureMutedDiscoveryTest {

    @Test
    void shouldExcludeMutedBodiesAndResults() {
        final TestPlanV1_0 plan = new TestPlanV1_0().setTests(List.of()).setMutedTests(
                List.of(
                        new TestPlanV1_0.TestCase().setId("muted")
                )
        );
        final AllureResults results = run(plan, 1);

        assertThat(results.getTestResults()).singleElement()
                .satisfies(result -> assertThat(result.getName()).isEqualTo("control()"));
    }

    @Test
    void shouldCompleteAllMutedRunSuccessfully() {
        final TestPlanV1_0 plan = new TestPlanV1_0().setMutedTests(
                List.of(
                        new TestPlanV1_0.TestCase().setId("muted"),
                        new TestPlanV1_0.TestCase().setSelector(MutedPlanTests.class.getName() + ".control")
                )
        );
        final AllureResults results = run(plan, 0);

        assertThat(results.getTestResults()).isEmpty();
    }

    @Test
    void shouldMatchUniqueIdWithUnmatchedExplicitId() {
        final String uniqueId = "[engine:junit-jupiter]/[class:" + MutedPlanTests.class.getName()
                + "]/[method:muted()]";
        final TestPlanV1_0 plan = new TestPlanV1_0().setMutedTests(
                List.of(
                        new TestPlanV1_0.TestCase().setId("other").setSelector(uniqueId)
                )
        );
        final AllureResults results = run(plan, 1);

        assertThat(results.getTestResults()).hasSize(1);
    }

    private AllureResults run(final TestPlanV1_0 plan, final int expectedCount) {
        return RunUtils.runTests(lifecycle -> {
            final SummaryGeneratingListener summary = new SummaryGeneratingListener();
            final LauncherConfig config = LauncherConfig.builder()
                    .enableTestExecutionListenerAutoRegistration(false)
                    .enablePostDiscoveryFilterAutoRegistration(false)
                    .addTestExecutionListeners(new AllureJunitPlatform(lifecycle), summary).build();
            LauncherFactory.create(config).execute(
                    LauncherDiscoveryRequestBuilder.request()
                            .selectors(DiscoverySelectors.selectClass(MutedPlanTests.class))
                            .filters(new AllurePostDiscoveryFilter(plan)).build()
            );
            assertThat(summary.getSummary().getFailures()).isEmpty();
            assertThat(summary.getSummary().getTestsStartedCount()).isEqualTo(expectedCount);
        });
    }
}
