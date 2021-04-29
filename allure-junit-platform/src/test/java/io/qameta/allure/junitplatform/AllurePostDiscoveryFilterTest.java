/*
 *  Copyright 2020 Qameta Software OÜ
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

import io.qameta.allure.junitplatform.features.FilterParameterizedTests;
import io.qameta.allure.junitplatform.features.FilterSimpleTests;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanV1_0;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.qameta.allure.junitplatform.AllureJunitPlatformTestUtils.runClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllurePostDiscoveryFilterTest {

    @Test
    void shouldRunAllTestsIfNoTestPlanProvided() {
        final AllureResults results = runClasses(FilterSimpleTests.class);

        assertThat(results.getTestResults())
                .hasSize(3);
    }

    @Test
    void shouldRunAllParameterizedTestsIfNoTestPlanProvided() {
        final AllureResults results = runClasses(FilterParameterizedTests.class);

        assertThat(results.getTestResults())
                .hasSize(6);
    }

    @Test
    void shouldRunAllTestsIfEmptyTestPlanProvided() {
        final AllureResults results = runClasses(new TestPlanV1_0(), FilterSimpleTests.class);
        assertThat(results.getTestResults())
                .hasSize(3);
    }

    @Test
    void shouldRunAllParameterizedTestsIfEmptyTestPlanProvided() {
        final AllureResults results = runClasses(new TestPlanV1_0(), FilterParameterizedTests.class);
        assertThat(results.getTestResults())
                .hasSize(6);
    }

    @Test
    void shouldFilterTestCasesByFullName() {
        final TestPlan testPlan = new TestPlanV1_0().setTests(Arrays.asList(
                new TestPlanV1_0.TestCase()
                        .setSelector(String.format("%s.second", FilterSimpleTests.class.getCanonicalName())),
                new TestPlanV1_0.TestCase()
                        .setSelector(String.format("%s.first", FilterSimpleTests.class.getCanonicalName())))
        );

        final AllureResults results = runClasses(testPlan, FilterSimpleTests.class);
        assertThat(results.getTestResults())
                .hasSize(2);
    }

    @Test
    void shouldFilterParameterizedTestCasesByFullName() {
        final TestPlan testPlan = new TestPlanV1_0().setTests(Arrays.asList(
                new TestPlanV1_0.TestCase()
                        .setSelector(String.format("%s.second", FilterParameterizedTests.class.getCanonicalName())),
                new TestPlanV1_0.TestCase()
                        .setSelector(String.format("%s.first", FilterParameterizedTests.class.getCanonicalName()))
        ));

        final AllureResults results = runClasses(testPlan, FilterParameterizedTests.class);
        assertThat(results.getTestResults())
                .hasSize(4);
    }

    @Test
    void shouldFilterTestCasesByUniqueId() {
        final TestPlan testPlan = new TestPlanV1_0().setTests(Arrays.asList(
                new TestPlanV1_0.TestCase()
                        .setSelector(testId(FilterSimpleTests.class, "first")),
                new TestPlanV1_0.TestCase()
                        .setSelector(testId(FilterSimpleTests.class, "third"))
        ));

        final AllureResults results = runClasses(testPlan, FilterSimpleTests.class);
        assertThat(results.getTestResults())
                .hasSize(2);
    }

    @Test
    void shouldFilterTestCasesByAllureId() {
        final TestPlan testPlan = new TestPlanV1_0().setTests(Arrays.asList(
                new TestPlanV1_0.TestCase()
                        .setId("10"),
                new TestPlanV1_0.TestCase()
                        .setId("20"),
                new TestPlanV1_0.TestCase()
                        .setId("30")
        ));

        final AllureResults results = runClasses(testPlan, FilterSimpleTests.class);
        assertThat(results.getTestResults())
                .hasSize(2);
    }

    @Test
    void shouldFilterParameterizedTestCasesByAllureId() {
        final TestPlan testPlan = new TestPlanV1_0().setTests(Arrays.asList(
                new TestPlanV1_0.TestCase()
                        .setId("10"),
                new TestPlanV1_0.TestCase()
                        .setId("20")
        ));

        final AllureResults results = runClasses(testPlan, FilterParameterizedTests.class);
        assertThat(results.getTestResults())
                .hasSize(4);
    }


    private String testId(final Class<?> testClass, String method) {
        return String.format("[engine:%s]/[class:%s]/[method:%s()]",
                "junit-jupiter",
                testClass.getCanonicalName(),
                method);
    }


}
