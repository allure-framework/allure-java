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
package io.qameta.allure.jupiter;

import io.qameta.allure.Allure;
import io.qameta.allure.Param;
import io.qameta.allure.junitplatform.AllureJunitPlatform;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Tag("junit6-compat")
@SuppressWarnings("unused")
class AllureJupiterJunit6CompatibilityTest {

    @ExtendWith(AllureJupiter.class)
    static class CompatParametersTest {

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {"a", "b"})
        void paramTest(@Param("id") final String value) {
        }
    }

    @Nested
    @ExtendWith(AllureJupiter.class)
    class CompatFixtures {

        @BeforeEach
        void setUp() {
            Allure.step("before step");
        }

        @Test
        void testBody() {
            Allure.step("test step");
        }

        @AfterEach
        void tearDown() {
            Allure.step("after step");
        }
    }

    @Test
    void shouldCaptureParametersWithParamAnnotationOnJunit6() {
        final AllureResults results = runWithLauncher(CompatParametersTest.class);

        assertThat(results.getTestResults()).isNotEmpty();

        final List<Parameter> allParams = results.getTestResults().stream()
                .flatMap(tr -> tr.getParameters().stream())
                .toList();

        assertThat(allParams)
                .filteredOn(parameter -> "id".equals(parameter.getName()))
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("id", "a"),
                        tuple("id", "b")
                );
    }

    @Test
    void shouldCaptureFixturesAndStepsOnJunit6() {
        final AllureResults results = runWithLauncher(CompatFixtures.class);

        assertThat(results.getTestResults()).hasSize(1);
        final TestResult testResult = results.getTestResults().get(0);

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getChildren)
                .contains(testResult.getUuid());

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(
                        tuple("setUp", Status.PASSED)
                );

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(
                        tuple("tearDown", Status.PASSED)
                );

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .flatExtracting(FixtureResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("before step");

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("test step");

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .flatExtracting(FixtureResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("after step");
    }

    @io.qameta.allure.Step("Run classes {classes}")
    private AllureResults runWithLauncher(final Class<?>... classes) {
        return RunUtils.runTests(lifecycle -> {
            final ClassSelector[] selectors = Stream.of(classes)
                    .map(DiscoverySelectors::selectClass)
                    .toArray(ClassSelector[]::new);

            final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
                    .selectors(selectors)
                    .build();

            final LauncherConfig config = LauncherConfig.builder()
                    .enableTestExecutionListenerAutoRegistration(false)
                    .addTestExecutionListeners(new AllureJunitPlatform(lifecycle))
                    .build();

            final Launcher launcher = LauncherFactory.create(config);
            launcher.execute(request);
        });
    }
}
