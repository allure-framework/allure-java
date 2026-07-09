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

import io.qameta.allure.Step;
import io.qameta.allure.junitplatform.AllureJunitPlatform;
import io.qameta.allure.jupiter.features.BrokenBeforeEachTests;
import io.qameta.allure.jupiter.features.FixtureTests;
import io.qameta.allure.jupiter.features.NestedTemplatesTests;
import io.qameta.allure.jupiter.features.ParamAnnotationTests;
import io.qameta.allure.jupiter.features.ParameterizedClassTests;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IsolatedLifecycle
class AllureJupiterTest {

    @Test
    @AllureFeatures.Fixtures
    void shouldReportClassAndMethodFixtures() {
        final AllureResults results = runClasses(FixtureTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("firstTest()", Status.PASSED),
                        tuple("secondTest()", Status.PASSED)
                );

        final TestResult firstTest = findTest(testResults, "firstTest()");
        final TestResult secondTest = findTest(testResults, "secondTest()");
        final List<TestResultContainer> containers = results.getTestResultContainers();

        final TestResultContainer classScope = containers.stream()
                .filter(container -> container.getChildren().contains(firstTest.getUuid())
                        && container.getChildren().contains(secondTest.getUuid()))
                .findAny()
                .orElseThrow(() -> new AssertionError("no scope references both tests"));
        assertThat(classScope.getBefores())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(tuple("setUpAll", Status.PASSED));
        assertThat(classScope.getAfters())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(tuple("tearDownAll", Status.PASSED));

        final TestResultContainer firstScope = findMethodScope(containers, firstTest);
        assertThat(firstScope.getBefores())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(tuple("setUp", Status.PASSED));
        assertThat(firstScope.getAfters())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(tuple("tearDown", Status.PASSED));

        final TestResultContainer secondScope = findMethodScope(containers, secondTest);
        assertThat(secondScope.getBefores())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(tuple("setUp", Status.PASSED));
        assertThat(secondScope.getAfters())
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(tuple("tearDown", Status.PASSED));
    }

    @Test
    @AllureFeatures.Fixtures
    void shouldReportFailedBeforeEachFixture() {
        final AllureResults results = runClasses(BrokenBeforeEachTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrPropertyWithValue("name", "testWithBrokenBeforeEach()")
                .hasFieldOrPropertyWithValue("status", Status.BROKEN);

        final List<FixtureResult> befores = results.getTestResultContainers().stream()
                .flatMap(container -> container.getBefores().stream())
                .collect(Collectors.toList());
        assertThat(befores)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(tuple("setUp", Status.BROKEN));

        final FixtureResult setUp = befores.get(0);
        assertThat(setUp.getStatusDetails().getMessage())
                .isEqualTo("fail in beforeEach");
        assertThat(setUp.getStatusDetails().getTrace())
                .contains("IllegalStateException");

        final TestResultContainer methodScope = results.getTestResultContainers().stream()
                .filter(container -> !container.getBefores().isEmpty())
                .findAny()
                .orElseThrow(() -> new AssertionError("no scope with the broken fixture"));
        assertThat(methodScope.getChildren())
                .containsExactly(testResult.getUuid());
    }

    @Test
    @AllureFeatures.Parameters
    void shouldReportParamAnnotationNamesAndModes() {
        final AllureResults results = runClasses(ParamAnnotationTests.class);

        final List<Parameter> parameters = results.getTestResults().stream()
                .flatMap(testResult -> testResult.getParameters().stream())
                .filter(parameter -> !"UniqueId".equals(parameter.getName()))
                .collect(Collectors.toList());

        assertThat(parameters)
                .filteredOn(parameter -> !"plain value".equals(parameter.getValue()))
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .containsExactlyInAnyOrder(
                        tuple("custom name", "named value", Parameter.Mode.DEFAULT, false),
                        tuple("secret", "masked value", Parameter.Mode.MASKED, false),
                        tuple("history key", "excluded value", Parameter.Mode.DEFAULT, true)
                );

        final Parameter plain = parameters.stream()
                .filter(parameter -> "plain value".equals(parameter.getValue()))
                .findAny()
                .orElseThrow(() -> new AssertionError("parameter without @Param not reported"));
        assertThat(plain.getName()).isNotBlank();
        assertThat(plain.getMode()).isNull();
        assertThat(plain.getExcluded()).isNull();
    }

    @Test
    @AllureFeatures.Parameters
    void shouldReportParameterizedClassArguments() {
        final AllureResults results = runClasses(ParameterizedClassTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2);

        assertThat(testResults)
                .flatExtracting(TestResult::getParameters)
                .filteredOn(parameter -> "class arg".equals(parameter.getName()))
                .extracting(Parameter::getValue)
                .containsExactlyInAnyOrder("a", "b");

        assertThat(testResults)
                .extracting(TestResult::getName)
                .doesNotHaveDuplicates()
                .allMatch(name -> name.endsWith("classTemplateTest()"));
    }

    @Test
    @AllureFeatures.Parameters
    void shouldReportClassAndMethodArgumentsForNestedTemplates() {
        final AllureResults results = runClasses(NestedTemplatesTests.class);

        final List<TestResult> testResults = results.getTestResults();
        assertThat(testResults)
                .hasSize(2);

        // class arguments come first, method arguments second, nothing is duplicated
        testResults.forEach(testResult -> {
            final List<String> visibleParameterNames = testResult.getParameters().stream()
                    .filter(parameter -> !"UniqueId".equals(parameter.getName()))
                    .map(Parameter::getName)
                    .collect(Collectors.toList());
            assertThat(visibleParameterNames)
                    .containsExactly("outer", "inner");
        });

        assertThat(testResults)
                .flatExtracting(TestResult::getParameters)
                .filteredOn(parameter -> "outer".equals(parameter.getName()))
                .extracting(Parameter::getValue)
                .containsExactlyInAnyOrder("x", "y");

        assertThat(testResults)
                .flatExtracting(TestResult::getParameters)
                .filteredOn(parameter -> "inner".equals(parameter.getName()))
                .extracting(Parameter::getValue)
                .containsExactly("inner value", "inner value");
    }

    private static TestResult findTest(final List<TestResult> testResults, final String name) {
        return testResults.stream()
                .filter(testResult -> name.equals(testResult.getName()))
                .findAny()
                .orElseThrow(() -> new AssertionError("no test result named " + name));
    }

    private static TestResultContainer findMethodScope(final List<TestResultContainer> containers,
                                                       final TestResult testResult) {
        return containers.stream()
                .filter(container -> container.getChildren().equals(List.of(testResult.getUuid())))
                .findAny()
                .orElseThrow(() -> new AssertionError("no method scope for " + testResult.getName()));
    }

    @Step("Run classes {classes}")
    private AllureResults runClasses(final Class<?>... classes) {
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

            LauncherFactory.create(config).execute(request);
        });
    }
}
