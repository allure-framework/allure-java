/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.spock2;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.junitplatform.AllurePostDiscoveryFilter;
import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.spock2.samples.BrokenTest;
import io.qameta.allure.spock2.samples.DataDrivenTest;
import io.qameta.allure.spock2.samples.DerivedSpec;
import io.qameta.allure.spock2.samples.FailedTest;
import io.qameta.allure.spock2.samples.FixturesTest;
import io.qameta.allure.spock2.samples.HelloSpockSpec;
import io.qameta.allure.spock2.samples.OneTest;
import io.qameta.allure.spock2.samples.ParametersTest;
import io.qameta.allure.spock2.samples.SpecFixtures;
import io.qameta.allure.spock2.samples.StepsAndBlocks;
import io.qameta.allure.spock2.samples.StreamsListener;
import io.qameta.allure.spock2.samples.TestWithAnnotations;
import io.qameta.allure.spock2.samples.TestWithAnnotationsOnClass;
import io.qameta.allure.spock2.samples.TestWithCustomAnnotations;
import io.qameta.allure.spock2.samples.TestWithSteps;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import io.qameta.allure.testfilter.TestPlan;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.spockframework.runtime.GlobalExtensionRegistry;
import org.spockframework.runtime.RunContext;
import org.spockframework.runtime.SpockEngine;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureSpock2Test {

    @Test
    void shouldStoreTestsInformation() {
        final AllureResults results = runClasses(OneTest.class);
        assertThat(results.getTestResults())
                .hasSize(1);
    }

    @Test
    void shouldCaptureSystemStreams() {
        final AllureResults results = runClasses(StreamsListener.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getName, this::printSteps)
                .containsExactlyInAnyOrder(
                        tuple("Streams Test", "step1, error step, expect")
                );
    }

    @Test
    void shouldSupportTestsWithStepsAndBlocks() {
        final AllureResults results = runClasses(StepsAndBlocks.class);
        assertThat(results.getTestResults())
                .hasSize(1);

        final TestResult tr = results.getTestResults().get(0);

        assertThat(tr.getSteps())
                .extracting(StepResult::getName)
                .containsExactly(
                        "given: asd",
                        "step1",
                        "step2",
                        "step some",
                        "when",
                        "step3",
                        "step4",
                        "then",
                        "step5",
                        "step6"
                );
    }

    @Test
    void shouldSupportBlocks() {
        final AllureResults results = runClasses(HelloSpockSpec.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getName, this::printSteps)
                .containsExactlyInAnyOrder(
                        tuple(
                                "length of Spock's and his friends' names [name: Spock, length: 5, #0]",
                                "expect, where"
                        ),
                        tuple(
                                "length of Spock's and his friends' names [name: Kirk, length: 4, #1]",
                                "expect, where"
                        ),
                        tuple(
                                "length of Spock's and his friends' names [name: Scotty, length: 6, #2]",
                                "expect, where"
                        )
                );
    }

    @Test
    void shouldSetTestStart() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runClasses(OneTest.class);

        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStart)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetTestStop() {
        final long before = Instant.now().toEpochMilli();
        final AllureResults results = runClasses(OneTest.class);
        final long after = Instant.now().toEpochMilli();

        assertThat(results.getTestResults())
                .extracting(TestResult::getStop)
                .allMatch(v -> v >= before && v <= after);
    }

    @Test
    void shouldSetTestFullName() {
        final AllureResults results = runClasses(OneTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getFullName)
                .containsExactly("io.qameta.allure.spock2.samples.OneTest.Simple Test");
    }

    @Test
    void shouldSetStageFinished() {
        final AllureResults results = runClasses(OneTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStage)
                .containsExactly(Stage.FINISHED);
    }

    @Test
    void shouldProcessFailedTest() {
        final AllureResults results = runClasses(FailedTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactly(Status.FAILED);
    }

    @Test
    void shouldProcessBrokenTest() {
        final AllureResults results = runClasses(BrokenTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getStatus)
                .containsExactly(Status.BROKEN);
    }

    @Test
    void shouldAddStepsToTest() {
        final AllureResults results = runClasses(TestWithSteps.class);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("expect", "step1", "step2", "step3");
    }

    @Test
    void shouldProcessMethodAnnotations() {
        final AllureResults results = runClasses(TestWithAnnotations.class);
        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("epic", "epic1"),
                        tuple("epic", "epic2"),
                        tuple("epic", "epic3"),
                        tuple("feature", "feature1"),
                        tuple("feature", "feature2"),
                        tuple("feature", "feature3"),
                        tuple("story", "story1"),
                        tuple("story", "story2"),
                        tuple("story", "story3"),
                        tuple("owner", "some-owner")
                );
    }

    @Test
    void shouldProcessClassAnnotations() {
        final AllureResults results = runClasses(TestWithAnnotationsOnClass.class);
        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getValue)
                .contains(
                        "epic1", "epic2", "epic3",
                        "feature1", "feature2", "feature3",
                        "story1", "story2", "story3",
                        "some-owner"
                );
    }

    @Test
    void shouldProcessCustomAnnotations() {
        final AllureResults results = runClasses(TestWithCustomAnnotations.class);
        assertThat(results.getTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getValue)
                .contains(
                        "epic", "feature", "story", "AS-1", "XRT-1"
                );
    }

    @Test
    void shouldProcessFlakyAnnotation() {
        final AllureResults results = runClasses(TestWithAnnotations.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getName, tr -> tr.getStatusDetails().isFlaky())
                .containsExactlyInAnyOrder(
                        tuple("someTest", true)
                );
    }

    @Test
    void shouldProcessMutedAnnotation() {
        final AllureResults results = runClasses(TestWithAnnotations.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getName, tr -> tr.getStatusDetails().isMuted())
                .containsExactlyInAnyOrder(
                        tuple("someTest", true)
                );
    }

    @Test
    void shouldSetDisplayName() {
        final AllureResults results = runClasses(OneTest.class);
        assertThat(results.getTestResults())
                .extracting(TestResult::getName)
                .containsExactly("Simple Test");
    }

    @Test
    void shouldSetLinks() {
        final AllureResults results = runClasses(FailedTest.class);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .containsExactlyInAnyOrder("link-1", "link-2", "issue-1", "issue-2", "tms-1", "tms-2");
    }

    @Test
    void shouldSetParameters() {
        final AllureResults results = runClasses(ParametersTest.class);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("a", "1"),
                        tuple("b", "3"),
                        tuple("c", "3")
                );
    }

    @Test
    void shouldSupportDataDrivenTests() {
        final AllureResults results = runClasses(DataDrivenTest.class);
        assertThat(results.getTestResults())
                .extracting(
                        TestResult::getName,
                        TestResult::getFullName,
                        TestResult::getTestCaseName,
                        TestResult::getTestCaseId,
                        TestResult::getHistoryId
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                "Simple Test [a: 1, b: 3, c: 3, #0]",
                                "io.qameta.allure.spock2.samples.DataDrivenTest.Simple Test",
                                "Simple Test",
                                "c7d975849471fd7b3d9e6637744a3154",
                                "8d0c81dcc577daba0013d9f64eac328b"
                        ),
                        tuple(
                                "Simple Test [a: 7, b: 4, c: 7, #1]",
                                "io.qameta.allure.spock2.samples.DataDrivenTest.Simple Test",
                                "Simple Test",
                                "c7d975849471fd7b3d9e6637744a3154",
                                "29f09c26cfc058d5aa5e83cdf84f4e9d"
                        ),
                        tuple(
                                "Simple Test [a: 0, b: 0, c: 0, #2]",
                                "io.qameta.allure.spock2.samples.DataDrivenTest.Simple Test",
                                "Simple Test",
                                "c7d975849471fd7b3d9e6637744a3154",
                                "e9a7c12cf7d0cf84d4a2ee322435c10f"
                        )
                );
    }

    @Test
    void shouldSupportFixtures() {
        final AllureResults results = runClasses(FixturesTest.class);

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getBefores)
                .extracting(
                        FixtureResult::getName,
                        this::printSteps
                )
                .containsExactlyInAnyOrder(
                        tuple("setup spec", "setupSpec step 1, setupSpec step 2"),
                        tuple("setup", "setup step 1, setup step 2"),
                        tuple("setup", "setup step 1, setup step 2")
                );

        assertThat(results.getTestResultContainers())
                .flatExtracting(TestResultContainer::getAfters)
                .extracting(
                        FixtureResult::getName,
                        this::printSteps
                )
                .containsExactlyInAnyOrder(
                        tuple("cleanup spec", "cleanupSpec step 1, cleanupSpec step 2"),
                        tuple("cleanup", "cleanup step 1, cleanup step 2"),
                        tuple("cleanup", "cleanup step 1, cleanup step 2")
                );


        final TestResult tr1 = results.getTestResults().stream()
                .filter(tr -> "First Test".equals(tr.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("can't find a test"));

        final TestResult tr2 = results.getTestResults().stream()
                .filter(tr -> "Second Test".equals(tr.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("can't find a test"));

        assertThat(results.getTestResultContainers())
                .extracting(TestResultContainer::getChildren)
                .usingDefaultElementComparator()
                .containsExactlyInAnyOrder(
                        Collections.singletonList(tr1.getUuid()),
                        Collections.singletonList(tr1.getUuid()),
                        Collections.singletonList(tr2.getUuid()),
                        Collections.singletonList(tr2.getUuid()),
                        Arrays.asList(tr1.getUuid(), tr2.getUuid())
                );
    }

    @Test
    void shouldSupportFixturesFromDerivedSpecs() {
        final AllureResults results = runClasses(DerivedSpec.class);

        final List<Triple<String, List<String>, List<String>>> fixtures = getTrFixtures(results);

        assertThat(fixtures)
                .extracting(Triple::getLeft, Triple::getMiddle, Triple::getRight)
                .containsExactlyInAnyOrder(
                        tuple(
                                "baseSpecMethod",
                                Arrays.asList(
                                        "setup [ base setup() ] ",
                                        "setup [ derived setup() ] ",
                                        "setup spec [ base setupSpec() ] ",
                                        "setup spec [ derived setupSpec() ] "
                                ),
                                Arrays.asList(
                                        "cleanup [ base cleanup() ] ",
                                        "cleanup [ derived cleanup() ] ",
                                        "cleanup spec [ base cleanupSpec() ] ",
                                        "cleanup spec [ derived cleanupSpec() ] "
                                )
                        ),
                        tuple(
                                "derivedSpecMethod",
                                Arrays.asList(
                                        "setup [ base setup() ] ",
                                        "setup [ derived setup() ] ",
                                        "setup spec [ base setupSpec() ] ",
                                        "setup spec [ derived setupSpec() ] "
                                ),
                                Arrays.asList(
                                        "cleanup [ base cleanup() ] ",
                                        "cleanup [ derived cleanup() ] ",
                                        "cleanup spec [ base cleanupSpec() ] ",
                                        "cleanup spec [ derived cleanupSpec() ] "
                                )
                        )
                );

    }

    private List<Triple<String, List<String>, List<String>>> getTrFixtures(final AllureResults results) {
        final Map<String, List<TestResultContainer>> trContainers = results.getTestResultContainers().stream()
                .flatMap(container -> container.getChildren().stream().map(child -> Pair.of(child, container)))
                .collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));

        return results.getTestResults().stream().map(tr -> {
                    final List<TestResultContainer> containers = trContainers
                            .getOrDefault(tr.getUuid(), Collections.emptyList());

                    final List<String> befores = containers.stream()
                            .map(TestResultContainer::getBefores)
                            .flatMap(Collection::stream)
                            .map(fr -> fr.getName() + " [ " + printSteps(fr) + " ] ")
                            .sorted()
                            .collect(Collectors.toList());


                    final List<String> afters = containers.stream()
                            .map(TestResultContainer::getAfters)
                            .flatMap(Collection::stream)
                            .map(fr -> fr.getName() + " [ " + printSteps(fr) + " ] ")
                            .sorted()
                            .collect(Collectors.toList());

                    return Triple.of(tr.getName(), befores, afters);
                })
                .collect(Collectors.toList());

    }

    @Test
    void shouldNotMixUpFixturesBetweenTests() {
        final AllureResults results = runClasses(OneTest.class, SpecFixtures.class);

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, this::printSteps)
                .containsExactlyInAnyOrder(
                        tuple("Simple Test", "expect"),
                        tuple("test with spec fixtures", "expect then and when given: the end")
                );

        final List<Triple<String, List<String>, List<String>>> trFixtures = getTrFixtures(results);

        assertThat(trFixtures)
                .extracting(Triple::getLeft, Triple::getMiddle, Triple::getRight)
                .containsExactlyInAnyOrder(
                        tuple(
                                "Simple Test",
                                Collections.singletonList("setup [ OneTest#setup ] "),
                                Collections.emptyList()
                        ),
                        tuple(
                                "test with spec fixtures",
                                Collections.singletonList("setup spec [ SpecFixtures#setupSpec ] "),
                                Collections.singletonList("cleanup spec [ SpecFixtures#cleanupSpec ] ")
                        )
                );
    }

    @Step("Run classes {classes}")
    public static AllureResults runClasses(final Class<?>... classes) {
        return runClasses(null, classes);
    }

    @Step("Run classes {classes}")
    public static AllureResults runClasses(final TestPlan testPlan, final Class<?>... classes) {
        final AllureResultsWriterStub writerStub = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writerStub);

        final ClassSelector[] classSelectors = Stream.of(classes)
                .map(DiscoverySelectors::selectClass)
                .toArray(ClassSelector[]::new);

        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .filters(new AllurePostDiscoveryFilter(testPlan))
                .selectors(classSelectors)
                .build();

        final RunContext context = RunContext.get();
        final GlobalExtensionRegistry extensionRegistry = ReflectionUtils
                .tryToReadFieldValue(RunContext.class, "globalExtensionRegistry", context)
                .andThenTry(GlobalExtensionRegistry.class::cast)
                .toOptional()
                .orElseThrow(() -> new AssertionError("could not access globalExtensionRegistry field of RunContext"));
        extensionRegistry.getGlobalExtensions().add(new AllureSpock2(lifecycle));

        final LauncherConfig config = LauncherConfig.builder()
                .enableTestEngineAutoRegistration(false)
                .addTestEngines(new SpockEngine())
                .enableTestExecutionListenerAutoRegistration(false)
                .enablePostDiscoveryFilterAutoRegistration(false)
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

    private String printSteps(final ExecutableItem item) {
        return item.getSteps().stream()
                .map(StepResult::getName)
                .collect(Collectors.joining(", "));
    }

}
