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

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import io.qameta.allure.testng.config.AllureTestNgConfig;
import io.qameta.allure.testng.samples.ConfigurationListenerFixtures;
import io.qameta.allure.testng.samples.FailedAfterMethod;
import io.qameta.allure.testng.samples.FailedBeforeMethod;
import io.qameta.allure.testng.samples.FailedBeforeSuite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testng.TestNG;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@IsolatedLifecycle
class AllureTestNgConfigurationTest {

    /**
     * A screenshot collected by a configuration-failure listener belongs to the failed setup fixture,
     * regardless of whether Allure or the custom listener was registered first.
     */
    @Description
    @Issue("776")
    @ParameterizedTest(name = "Failed setup, Allure registered first: {0}")
    @ValueSource(booleans = {true, false})
    void shouldKeepFailedSetupEvidence(final boolean allureFirst) {
        final AllureResults results = runConfigurations(FailedBeforeMethod.class, allureFirst);

        assertFixtureEvidence(results, "Configuration failure", "beforeMethod");
        assertThat(fixtures(results)).extracting(FixtureResult::getStatus).containsExactly(Status.BROKEN);
        assertThat(results.getTestResults()).singleElement()
                .satisfies(test -> assertThat(test.getStatus()).isEqualTo(Status.SKIPPED));
    }

    /**
     * Teardown evidence collected after a configuration failure is retained before the after-method
     * container is written, even though its test result has already finished.
     */
    @Description
    @Issue("776")
    @ParameterizedTest(name = "Failed teardown, Allure registered first: {0}")
    @ValueSource(booleans = {true, false})
    void shouldKeepFailedTeardownEvidence(final boolean allureFirst) {
        final AllureResults results = runConfigurations(FailedAfterMethod.class, allureFirst);

        assertFixtureEvidence(results, "Configuration failure", "afterMethod");
        assertThat(fixtures(results)).extracting(FixtureResult::getStatus).containsExactly(Status.BROKEN);
        assertThat(results.getTestResults()).singleElement()
                .satisfies(test -> assertThat(test.getStatus()).isEqualTo(Status.PASSED));
    }

    /**
     * Suite setup failures retain listener attachments even when setup runs before TestNG sends the
     * test-context start event used to order ordinary test listeners.
     */
    @Description
    @Issue("776")
    @ParameterizedTest(name = "Failed suite setup, Allure registered first: {0}")
    @ValueSource(booleans = {true, false})
    void shouldKeepFailedSuiteSetupEvidence(final boolean allureFirst) {
        final AllureResults results = runConfigurations(FailedBeforeSuite.class, allureFirst);

        assertFixtureEvidence(results, "Configuration failure", "beforeSuite");
        assertThat(fixtures(results)).extracting(FixtureResult::getStatus).containsExactly(Status.BROKEN);
    }

    /**
     * Success-listener attachments belong to their suite or method fixtures, while the test body's
     * steps remain on the test itself.
     */
    @Description
    @Issue("776")
    @ParameterizedTest(name = "Successful configurations, Allure registered first: {0}")
    @ValueSource(booleans = {true, false})
    void shouldKeepSuccessfulConfigurationEvidence(final boolean allureFirst) {
        final AllureResults results = runConfigurations(ConfigurationListenerFixtures.Passing.class, allureFirst);

        assertFixtureEvidence(
                results, "Configuration success",
                "beforeSuite", "beforeMethod", "afterMethod", "afterSuite"
        );
        assertThat(fixtures(results)).extracting(FixtureResult::getStatus).containsOnly(Status.PASSED);
        assertThat(results.getTestResults()).singleElement().satisfies(test -> {
            assertThat(test.getSteps()).extracting(StepResult::getName).containsExactly("Test body");
            assertThat(test.getAttachments()).isEmpty();
        });
    }

    /**
     * Both an explicitly skipped configuration and a configuration skipped because its dependency
     * did not run retain their skip-listener evidence and reach a finished stage.
     */
    @Description
    @Issue("776")
    @ParameterizedTest(name = "Skipped configurations, Allure registered first: {0}")
    @ValueSource(booleans = {true, false})
    void shouldKeepSkippedConfigurationEvidence(final boolean allureFirst) {
        final AllureResults results = runConfigurations(ConfigurationListenerFixtures.Skipped.class, allureFirst);

        assertFixtureEvidence(results, "Configuration skip", "skip", "skippedDependency");
        assertThat(results.getTestResults()).singleElement()
                .satisfies(test -> assertThat(test.getStatus()).isEqualTo(Status.SKIPPED));
    }

    /**
     * Hiding the run-level configuration error still allows the failed fixture to finish with its
     * diagnostic attachments.
     */
    @Description
    @Issue("776")
    @Test
    void shouldFinishHiddenConfigurationFailure() {
        final AllureTestNgConfig config = AllureTestNgConfig.loadConfigProperties();
        config.setHideConfigurationFailures(true);
        final AllureResults results = RunUtils.runTests(lifecycle -> {
            final TestNG testNg = new TestNG(false);
            testNg.addListener(new AllureTestNg(lifecycle, new AllureTestNgTestFilter(), config));
            testNg.addListener(new ConfigurationListenerFixtures.EvidenceListener());
            testNg.setTestClasses(new Class<?>[]{FailedBeforeMethod.class});
            testNg.run();
        });

        assertFixtureEvidence(results, "Configuration failure", "beforeMethod");
        assertThat(results.getGlobals()).isEmpty();
    }

    /**
     * Configuration listeners declared on a test class receive an active fixture for their
     * completion callbacks without requiring programmatic listener registration.
     */
    @Description
    @Issue("776")
    @Test
    void shouldSupportAnnotatedConfigurationListener() {
        final AllureResults results = runConfigurations(testNg -> testNg.setTestClasses(new Class<?>[]{ConfigurationListenerFixtures.Annotated.class}));

        assertFixtureEvidence(
                results, "Configuration success",
                "beforeSuite", "beforeMethod", "afterMethod", "afterSuite"
        );
    }

    /**
     * Configuration listeners declared in suite XML retain their evidence on each completed
     * fixture without requiring programmatic listener registration.
     */
    @Description
    @Issue("776")
    @Test
    void shouldSupportXmlConfigurationListener() {
        final String suite = getClass().getClassLoader()
                .getResource("suites/configuration-listener-attachments.xml").getFile();
        final AllureResults results = runConfigurations(testNg -> testNg.setTestSuites(List.of(suite)));

        assertFixtureEvidence(
                results, "Configuration success",
                "beforeSuite", "beforeMethod", "afterMethod", "afterSuite"
        );
    }

    /**
     * An explicit TestNG comparator retains its ordering between user listeners while Allure
     * finishes each configuration only after both listeners have attached their evidence.
     */
    @Description
    @Issue("776")
    @Test
    void shouldPreserveCustomComparatorOrder() {
        final AllureResults results = runConfigurations(testNg -> {
            testNg.addListener(new ConfigurationListenerFixtures.EvidenceListener());
            testNg.addListener(new ConfigurationListenerFixtures.AnotherListener());
            testNg.setListenerComparator((first, second) -> second.getClass().getName().compareTo(first.getClass().getName()));
            testNg.setTestClasses(new Class<?>[]{ConfigurationListenerFixtures.Passing.class});
        });

        assertThat(fixtures(results)).hasSize(4).allSatisfy(fixture -> {
            assertThat(fixture.getStage()).isEqualTo(Stage.FINISHED);
            assertThat(fixture.getSteps()).flatExtracting(StepResult::getAttachments)
                    .extracting(Attachment::getName).containsExactly("Another listener", "Configuration success");
        });
    }

    /**
     * A first-time-only setup still finishes and keeps its attachment when TestNG omits the
     * configuration-success callback for that method.
     */
    @Description
    @Issue("776")
    @Test
    void shouldFinishFirstTimeOnlyConfiguration() {
        final AllureResults results = runConfigurations(ConfigurationListenerFixtures.FirstTimeOnly.class, true);

        assertThat(fixtures(results)).singleElement().satisfies(fixture -> {
            assertThat(fixture.getStage()).isEqualTo(Stage.FINISHED);
            assertThat(fixture.getStatus()).isEqualTo(Status.PASSED);
            assertThat(fixture.getSteps()).extracting(StepResult::getName).containsExactly("Setup body");
        });
        assertThat(results.getTestResults()).hasSize(2).allSatisfy(test -> assertThat(test.getSteps()).extracting(StepResult::getName).containsExactly("Test body"));
    }

    /**
     * A configuration failure before invocation, such as a missing required parameter, does not
     * finalize a nonexistent fixture or interfere with the always-run teardown that follows it.
     */
    @Description
    @Issue("776")
    @Test
    void shouldHandleConfigurationFailureBeforeInvocation() {
        final AllureResults results = runConfigurations(ConfigurationListenerFixtures.MissingParameter.class, true);

        assertFixtureEvidence(results, "Configuration success", "afterMethod");
        assertThat(results.getTestResults()).singleElement()
                .satisfies(test -> assertThat(test.getStatus()).isEqualTo(Status.SKIPPED));
    }

    private AllureResults runConfigurations(final Class<?> testClass, final boolean allureFirst) {
        return RunUtils.runTests(lifecycle -> {
            final TestNG testNg = new TestNG(false);
            final AllureTestNg adapter = new AllureTestNg(lifecycle);
            final ConfigurationListenerFixtures.EvidenceListener listener = new ConfigurationListenerFixtures.EvidenceListener();
            if (allureFirst) {
                testNg.addListener(adapter);
                testNg.addListener(listener);
            } else {
                testNg.addListener(listener);
                testNg.addListener(adapter);
            }
            testNg.setTestClasses(new Class<?>[]{testClass});
            testNg.run();
        });
    }

    private AllureResults runConfigurations(final Consumer<TestNG> configure) {
        return RunUtils.runTests(lifecycle -> {
            final TestNG testNg = new TestNG(false);
            testNg.addListener(new AllureTestNg(lifecycle));
            configure.accept(testNg);
            testNg.run();
        });
    }

    private static List<FixtureResult> fixtures(final AllureResults results) {
        return results.getTestResultContainers().stream()
                .flatMap(container -> Stream.concat(container.getBefores().stream(), container.getAfters().stream()))
                .toList();
    }

    private static void assertFixtureEvidence(final AllureResults results,
                                              final String attachmentName, final String... fixtureNames) {
        final List<FixtureResult> fixtures = fixtures(results);
        assertThat(fixtures).extracting(FixtureResult::getName).containsExactlyInAnyOrder(fixtureNames);
        assertThat(fixtures).allSatisfy(fixture -> {
            assertThat(fixture.getStage()).isEqualTo(Stage.FINISHED);
            final List<Attachment> attachments = fixture.getSteps().stream()
                    .flatMap(step -> step.getAttachments().stream()).toList();
            assertThat(attachments).singleElement().satisfies(attachment -> {
                assertThat(attachment.getName()).isEqualTo(attachmentName);
                assertThat(results.getAttachmentContentAsString(attachment)).isEqualTo(fixture.getName());
            });
        });
    }
}
