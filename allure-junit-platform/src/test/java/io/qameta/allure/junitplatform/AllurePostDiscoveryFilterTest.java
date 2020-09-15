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

import io.qameta.allure.junitplatform.features.PassedTests;
import io.qameta.allure.junitplatform.features.TestsWithAllureId;
import io.qameta.allure.testfilter.TestPlanV1_0;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.Arrays;

import static io.qameta.allure.junitplatform.AllureJunitPlatformTestUtils.buildPlan;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllurePostDiscoveryFilterTest {

    @Test
    void shouldRunAllTestsIfNoTestPlanProvided() {
        final TestPlan testPlan = buildPlan(null, PassedTests.class);

        final long testsCount = testPlan.countTestIdentifiers(TestIdentifier::isTest);

        assertThat(testsCount)
                .isEqualTo(3);
    }

    @Test
    void shouldRunAllTestsIfEmptyTestPlanProvided() {
        final TestPlan testPlan = buildPlan(new TestPlanV1_0(), PassedTests.class);

        final long testsCount = testPlan.countTestIdentifiers(TestIdentifier::isTest);

        assertThat(testsCount)
                .isEqualTo(3);
    }

    @Test
    void shouldFilterTestCasesByFullName() {
        final TestPlan testPlan = buildPlan(
                new TestPlanV1_0().setTests(Arrays.asList(
                        new TestPlanV1_0.TestCase()
                                .setSelector("io.qameta.allure.junitplatform.features.PassedTests.second"),
                        new TestPlanV1_0.TestCase()
                                .setSelector("io.qameta.allure.junitplatform.features.PassedTests.first")
                )),
                PassedTests.class
        );

        final long testsCount = testPlan.countTestIdentifiers(TestIdentifier::isTest);

        assertThat(testsCount)
                .isEqualTo(2);
    }

    @Test
    void shouldFilterTestCasesByUniqueId() {
        final TestPlan testPlan = buildPlan(
                new TestPlanV1_0().setTests(Arrays.asList(
                        new TestPlanV1_0.TestCase()
                                .setSelector("[engine:junit-jupiter]/[class:io.qameta.allure.junitplatform.features.PassedTests]/[method:second()]"),
                        new TestPlanV1_0.TestCase()
                                .setSelector("[engine:junit-jupiter]/[class:io.qameta.allure.junitplatform.features.PassedTests]/[method:third()]")
                )),
                PassedTests.class
        );

        final long testsCount = testPlan.countTestIdentifiers(TestIdentifier::isTest);

        assertThat(testsCount)
                .isEqualTo(2);
    }

    @Test
    void shouldFilterTestCasesByAllureId() {
        final TestPlan testPlan = buildPlan(
                new TestPlanV1_0().setTests(Arrays.asList(
                        new TestPlanV1_0.TestCase()
                                .setId("32"),
                        new TestPlanV1_0.TestCase()
                                .setId("34"),
                        new TestPlanV1_0.TestCase()
                                .setId("41"),
                        new TestPlanV1_0.TestCase()
                                .setId("48")
                )),
                TestsWithAllureId.class
        );

        final long testsCount = testPlan.countTestIdentifiers(TestIdentifier::isTest);

        assertThat(testsCount)
                .isEqualTo(3);
    }
}
