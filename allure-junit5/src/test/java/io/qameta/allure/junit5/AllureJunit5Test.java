package io.qameta.allure.junit5;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.junit5.features.AfterEachFixtureFailureSupport;
import io.qameta.allure.junit5.features.AllFixtureSupport;
import io.qameta.allure.junit5.features.BeforeEachFixtureFailureSupport;
import io.qameta.allure.junit5.features.EachFixtureSupport;
import io.qameta.allure.junitplatform.AllureJunitPlatform;
import io.qameta.allure.model.FixtureResult;
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
@SuppressWarnings("unchecked")
class AllureJunit5Test {

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