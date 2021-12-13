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
package io.qameta.allure.junit5;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.junit5.features.AfterEachFixtureFailureSupport;
import io.qameta.allure.junit5.features.AllFixtureSupport;
import io.qameta.allure.junit5.features.BeforeEachFixtureFailureSupport;
import io.qameta.allure.junit5.features.EachFixtureSupport;
import io.qameta.allure.junit5.features.ParameterisedBlankParameterValueTests;
import io.qameta.allure.junit5.features.ParameterisedPrimitivesTests;
import io.qameta.allure.junit5.features.ParameterisedTests;
import io.qameta.allure.junit5.features.SkipOtherInjectables;
import io.qameta.allure.junitplatform.AllureJunitPlatform;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@AllureFeatures.Fixtures
class AllureJunit5Test {

    @Issue("697")
    @Test
    void shouldSupportEmptyStringParameters() {
        final AllureResults results = runClasses(ParameterisedBlankParameterValueTests.class);

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, tr -> tr.getParameters().size())
                .containsExactlyInAnyOrder(
                        tuple("first(String) [1] value=", 2),
                        tuple("first(String) [2] value=   ", 2),
                        tuple("first(String) [3] value=null", 2)
                );
    }

    @Test
    void shouldSupportPrimitiveTypeParameters() {
        final AllureResults results = runClasses(ParameterisedPrimitivesTests.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getName, tr -> tr.getParameters().size())
                .containsExactlyInAnyOrder(
                        tuple("booleansMethodSource(boolean, boolean) [1] a=true, b=true", 3),
                        tuple("booleansMethodSource(boolean, boolean) [2] a=true, b=false", 3),
                        tuple("booleansMethodSource(boolean, boolean) [3] a=false, b=true", 3),
                        tuple("booleansMethodSource(boolean, boolean) [4] a=false, b=false", 3),
                        tuple("floats(float) [1] value=0.1", 2),
                        tuple("floats(float) [2] value=0.01", 2),
                        tuple("shorts(int) [1] value=1", 2),
                        tuple("shorts(int) [2] value=2", 2),
                        tuple("shorts(int) [3] value=3", 2),
                        tuple("ints(int) [1] value=1", 2),
                        tuple("ints(int) [2] value=2", 2),
                        tuple("ints(int) [3] value=3", 2),
                        tuple("bytes(byte) [1] value=0", 2),
                        tuple("bytes(byte) [2] value=1", 2),
                        tuple("chars(char) [1] value=a", 2),
                        tuple("chars(char) [2] value=b", 2),
                        tuple("chars(char) [3] value=c", 2),
                        tuple("longs(long) [1] value=0", 2),
                        tuple("longs(long) [2] value=1", 2),
                        tuple("nullMethodSource(String, Long) [1] stringValue=null, longValue=null", 3),
                        tuple("doubles(double) [1] value=0.1", 2),
                        tuple("doubles(double) [2] value=0.01", 2),
                        tuple("booleans(boolean) [1] value=true", 2),
                        tuple("booleans(boolean) [2] value=false", 2)
                );
    }

    @Test
    void shouldSupportParametersForParameterisedTests() {
        final AllureResults results = runClasses(ParameterisedTests.class);

        assertThat(results.getTestResults())
                .filteredOn("name", "first(String) [1] value=a")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .contains(
                        tuple("value", "a", null, null)
                );

        assertThat(results.getTestResults())
                .filteredOn("name", "first(String) [2] value=b")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .contains(
                        tuple("value", "b", null, null)
                );

    }

    @Test
    void shouldSupportParamAnnotationForParameters() {
        final AllureResults results = runClasses(ParameterisedTests.class);

        assertThat(results.getTestResults())
                .filteredOn("name", "third(String) [1] value=a")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .contains(
                        tuple("some value", "a", Parameter.Mode.DEFAULT, false)
                );

        assertThat(results.getTestResults())
                .filteredOn("name", "third(String) [2] value=b")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .contains(
                        tuple("some value", "b", Parameter.Mode.DEFAULT, false)
                );

    }

    @Test
    void shouldSkipReportingOfTestInjectablesTestReporterForRegularTest() {
        final AllureResults results = runClasses(SkipOtherInjectables.class);

        assertThat(results.getTestResults())
                .filteredOn("fullName", "io.qameta.allure.junit5.features.SkipOtherInjectables.regularTestWithReporterInjection")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .isEmpty();
    }

    @Test
    void shouldSkipReportingOfTestInjectablesTestReporterForParameterisedTest() {
        final AllureResults results = runClasses(SkipOtherInjectables.class);

        assertThat(results.getTestResults())
                .filteredOn("fullName", "io.qameta.allure.junit5.features.SkipOtherInjectables.testReporterInjection")
                .filteredOn("name", "testReporterInjection(String, TestReporter) [1] value=a")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .contains(
                        tuple("value", "a", null, null)
                );

        assertThat(results.getTestResults())
                .filteredOn("fullName", "io.qameta.allure.junit5.features.SkipOtherInjectables.testReporterInjection")
                .filteredOn("name", "testReporterInjection(String, TestReporter) [2] value=b")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue, Parameter::getMode, Parameter::getExcluded)
                .contains(
                        tuple("value", "b", null, null)
                );
    }

    @Test
    void shouldSupportBeforeEachFixture() {
        final AllureResults results = runClasses(EachFixtureSupport.class);

        assertThat(results.getTestResults())
                .hasSize(1);

        final TestResult testResult = results.getTestResults().get(0);

        assertThat(results.getTestResultContainers())
                .filteredOn("name", testResult.getName())
                .flatExtracting(TestResultContainer::getChildren)
                .contains(testResult.getUuid());

        assertThat(results.getTestResultContainers())
                .filteredOn("name", testResult.getName())
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(
                        tuple("setUp", Status.PASSED)
                );
    }

    @Test
    void shouldSupportStepsInBeforeEachFixture() {
        final AllureResults results = runClasses(EachFixtureSupport.class);

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .flatExtracting(FixtureResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("setUp 1", Status.PASSED),
                        tuple("setUp 2", Status.PASSED)
                );
    }

    @Test
    void shouldSupportAfterEachFixture() {
        final AllureResults results = runClasses(EachFixtureSupport.class);

        assertThat(results.getTestResults())
                .hasSize(1);

        final TestResult testResult = results.getTestResults().get(0);

        assertThat(results.getTestResultContainers())
                .filteredOn("name", testResult.getName())
                .flatExtracting(TestResultContainer::getChildren)
                .contains(testResult.getUuid());

        assertThat(results.getTestResultContainers())
                .filteredOn("name", testResult.getName())
                .flatExtracting(TestResultContainer::getAfters)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(
                        tuple("tearDown", Status.PASSED)
                );
    }

    @Test
    void shouldSupportStepsInAfterEachFixture() {
        final AllureResults results = runClasses(EachFixtureSupport.class);

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .flatExtracting(FixtureResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("tearDown 1", Status.PASSED),
                        tuple("tearDown 2", Status.PASSED)
                );
    }

    @Test
    void shouldSupportStepsInTestWithFixtures() {
        final AllureResults results = runClasses(EachFixtureSupport.class);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("test1 1", Status.PASSED),
                        tuple("test1 2", Status.PASSED)
                );
    }

    @Test
    void shouldSupportBeforeAllFixture() {
        final AllureResults results = runClasses(AllFixtureSupport.class);
        assertThat(results.getTestResults())
                .hasSize(1);

        final TestResult testResult = results.getTestResults().get(0);

        assertThat(results.getTestResultContainers())
                .filteredOn("name", "AllFixtureSupport")
                .flatExtracting(TestResultContainer::getChildren)
                .contains(testResult.getUuid());

        assertThat(results.getTestResultContainers())
                .filteredOn("name", "AllFixtureSupport")
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(
                        tuple("setUpAll", Status.PASSED)
                );
    }

    @Test
    void shouldSupportStepsInBeforeAllFixture() {
        final AllureResults results = runClasses(AllFixtureSupport.class);

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .flatExtracting(FixtureResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("setUpAll 1", Status.PASSED),
                        tuple("setUpAll 2", Status.PASSED)
                );
    }

    @Test
    void shouldSupportAfterAllFixture() {
        final AllureResults results = runClasses(AllFixtureSupport.class);
        assertThat(results.getTestResults())
                .hasSize(1);

        final TestResult testResult = results.getTestResults().get(0);

        assertThat(results.getTestResultContainers())
                .filteredOn("name", "AllFixtureSupport")
                .flatExtracting(TestResultContainer::getChildren)
                .contains(testResult.getUuid());

        assertThat(results.getTestResultContainers())
                .filteredOn("name", "AllFixtureSupport")
                .flatExtracting(TestResultContainer::getAfters)
                .extracting(FixtureResult::getName, FixtureResult::getStatus)
                .containsExactly(
                        tuple("tearDownAll", Status.PASSED)
                );
    }

    @Test
    void shouldSupportStepsInAfterAllFixture() {
        final AllureResults results = runClasses(AllFixtureSupport.class);

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .flatExtracting(FixtureResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("tearDownAll 1", Status.PASSED),
                        tuple("tearDownAll 2", Status.PASSED)
                );
    }

    @Test
    void shouldSupportFailureInBeforeEachFixture() {
        final AllureResults results = runClasses(BeforeEachFixtureFailureSupport.class);

        assertThat(results.getTestResults())
                .hasSize(1);

        final TestResult testResult = results.getTestResults().get(0);

        assertThat(results.getTestResultContainers())
                .filteredOn("name", testResult.getName())
                .flatExtracting(TestResultContainer::getChildren)
                .contains(testResult.getUuid());

        assertThat(results.getTestResultContainers())
                .filteredOn("name", testResult.getName())
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(FixtureResult::getName, FixtureResult::getStatus, f -> f.getStatusDetails().getMessage())
                .containsExactly(
                        tuple("setUp", Status.BROKEN, "ta da")
                );
    }

    @Test
    void shouldSupportFailureInAfterEachFixture() {
        final AllureResults results = runClasses(AfterEachFixtureFailureSupport.class);

        assertThat(results.getTestResults())
                .hasSize(1);

        final TestResult testResult = results.getTestResults().get(0);

        assertThat(results.getTestResultContainers())
                .filteredOn("name", testResult.getName())
                .flatExtracting(TestResultContainer::getChildren)
                .contains(testResult.getUuid());

        assertThat(results.getTestResultContainers())
                .filteredOn("name", testResult.getName())
                .flatExtracting(TestResultContainer::getAfters)
                .extracting(FixtureResult::getName, FixtureResult::getStatus, f -> f.getStatusDetails().getMessage())
                .containsExactly(
                        tuple("tearDown", Status.BROKEN, "ta da")
                );
    }

    @Step("Run classes {classes}")
    private AllureResults runClasses(final Class<?>... classes) {
        final AllureResultsWriterStub writerStub = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writerStub);

        final ClassSelector[] classSelectors = Stream.of(classes)
                .map(DiscoverySelectors::selectClass)
                .toArray(ClassSelector[]::new);

        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
                .selectors(classSelectors)
                .build();

        final LauncherConfig config = LauncherConfig.builder()
                .enableTestExecutionListenerAutoRegistration(false)
                .addTestExecutionListeners(new AllureJunitPlatform(lifecycle))
                .build();
        final Launcher launcher = LauncherFactory.create(config);

        final AllureLifecycle defaultLifecycle = Allure.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            StepsAspects.setLifecycle(lifecycle);
            AttachmentsAspects.setLifecycle(lifecycle);
            launcher.execute(request);
            return writerStub;
        } finally {
            Allure.setLifecycle(defaultLifecycle);
            StepsAspects.setLifecycle(defaultLifecycle);
            AttachmentsAspects.setLifecycle(defaultLifecycle);
        }
    }
}
