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
package io.qameta.allure.junit4;

import io.qameta.allure.junit4.samples.MutedPlanTests;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import io.qameta.allure.testfilter.TestPlanV1_0;
import org.junit.jupiter.api.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IsolatedLifecycle
class AllureJunit4MutedTest {

    @Test
    void shouldExcludeMutedBodiesAndResults() {
        final TestPlanV1_0 plan = new TestPlanV1_0().setTests(List.of()).setExcludedTests(
                List.of(
                        new TestPlanV1_0.TestCase().setId("muted")
                )
        );
        final AllureResults results = run(plan, 1);

        assertThat(results.getTestResults()).singleElement()
                .satisfies(result -> assertThat(result.getName()).isEqualTo("control"));
    }

    @Test
    void shouldCompleteAllMutedRunSuccessfully() {
        final TestPlanV1_0 plan = new TestPlanV1_0().setExcludedTests(
                List.of(
                        new TestPlanV1_0.TestCase().setId("muted"),
                        new TestPlanV1_0.TestCase().setSelector(MutedPlanTests.class.getName() + ".control")
                )
        );
        final AllureResults results = run(plan, 0);

        assertThat(results.getTestResults()).isEmpty();
    }

    private AllureResults run(final TestPlanV1_0 plan, final int expectedCount) {
        return RunUtils.runTests(lifecycle -> {
            final JUnitCore core = new JUnitCore();
            core.addListener(new AllureJunit4(lifecycle));
            final Result result = core.run(
                    Request.aClass(MutedPlanTests.class)
                            .filterWith(new AllureJunit4Filter(plan))
            );
            assertThat(result.getFailures()).isEmpty();
            assertThat(result.getRunCount()).isEqualTo(expectedCount);
        });
    }
}
