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
package io.qameta.allure;

import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.ScopeResult;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.qameta.allure.Allure.addStreamAttachmentAsync;
import static io.qameta.allure.test.TestData.randomId;
import static io.qameta.allure.test.TestData.randomName;
import static io.qameta.allure.test.TestData.randomString;
import static java.util.concurrent.CompletableFuture.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
@SuppressWarnings({"deprecation", "removal"})
class AllureLifecycleTest {

    private AllureResultsWriter writer;
    private AllureLifecycle lifecycle;

    @BeforeEach
    public void setUp() {
        writer = Mockito.mock(AllureResultsWriter.class);
        lifecycle = new AllureLifecycle(writer);
    }

    @Test
    void shouldReturnCurrentTestCaseId() {
        final String uuid = randomId();
        final String name = randomName();
        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        final String stepUuid = randomId();
        lifecycle.startStep(uuid, stepUuid, new StepResult().setName(randomName()));

        final Optional<String> currentTestCase = lifecycle.getCurrentTestCase();
        assertThat(currentTestCase)
                .isPresent()
                .hasValue(uuid);
    }

    @Test
    void shouldCreateTest() {
        final String uuid = randomId();
        final String name = randomName();
        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);
        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);
    }

    @Test
    void shouldCreateTestContainer() {
        final String uuid = randomId();
        final String name = randomName();
        final TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setName(name);
        lifecycle.startTestContainer(container);
        lifecycle.stopTestContainer(uuid);
        lifecycle.writeTestContainer(uuid);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);

    }

    @Test
    void shouldCreateScope() {
        final String uuid = randomId();
        final String name = randomName();
        final ScopeResult scope = new ScopeResult()
                .setUuid(uuid)
                .setName(name);
        lifecycle.startScope(scope);
        lifecycle.stopScope(uuid);
        lifecycle.writeScope(uuid);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isExactlyInstanceOf(TestResultContainer.class)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);
    }

    @Test
    void shouldUpdateScope() {
        final String uuid = randomId();
        final String name = randomName();
        final String newName = randomName();
        final String childUuid = randomId();
        final String fixtureUuid = randomId();
        final String fixtureName = randomName();
        final String description = randomName();
        final String descriptionHtml = "<p>" + randomName() + "</p>";
        final ScopeResult scope = new ScopeResult()
                .setUuid(uuid)
                .setName(name);
        lifecycle.startScope(scope);

        lifecycle.updateScope(
                uuid, result -> result
                        .setName(newName)
                        .setTests(List.of(childUuid))
                        .setDescription(description)
                        .setDescriptionHtml(descriptionHtml)
        );
        lifecycle.startBeforeFixture(uuid, fixtureUuid, new FixtureResult().setName(fixtureName));
        lifecycle.stopFixture(fixtureUuid);
        lifecycle.stopScope(uuid);
        lifecycle.writeScope(uuid);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResultContainer actual = captor.getValue();
        assertThat(actual)
                .isExactlyInstanceOf(TestResultContainer.class)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", newName)
                .hasFieldOrPropertyWithValue("description", null)
                .hasFieldOrPropertyWithValue("descriptionHtml", null);
        assertThat(actual.getChildren())
                .containsExactly(childUuid);
        assertThat(actual.getBefores())
                .hasSize(1)
                .extracting(FixtureResult::getName)
                .containsExactly(fixtureName);
        assertThat(actual.getAfters())
                .isEmpty();
    }

    @Test
    void shouldCreateChildTestContainer() {
        final String parentUuid = randomId();
        final String parentName = randomName();
        final TestResultContainer parent = new TestResultContainer()
                .setUuid(parentUuid)
                .setName(parentName);
        lifecycle.startTestContainer(parent);

        final String childUuid = randomId();
        final String childName = randomName();
        final TestResultContainer container = new TestResultContainer()
                .setUuid(childUuid)
                .setName(childName);
        lifecycle.startTestContainer(parentUuid, container);
        lifecycle.stopTestContainer(childUuid);
        lifecycle.writeTestContainer(childUuid);

        lifecycle.stopTestContainer(parentUuid);
        lifecycle.writeTestContainer(parentUuid);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(2)).write(captor.capture());

        final List<TestResultContainer> values = captor.getAllValues();
        assertThat(values)
                .isNotNull()
                .hasSize(2);

        assertThat(values.get(0))
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", childUuid)
                .hasFieldOrPropertyWithValue("name", childName);

        final TestResultContainer actual = values.get(1);
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", parentUuid)
                .hasFieldOrPropertyWithValue("name", parentName);

        assertThat(actual.getChildren())
                .containsExactly(childUuid);
    }

    @Test
    void shouldAddStepsToTests() {
        final String uuid = randomId();
        final String name = randomName();
        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        final String firstStepName = randomStep(uuid);
        final String secondStepName = randomStep(uuid);

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResult actual = captor.getValue();
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);

        assertThat(actual.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(firstStepName, secondStepName);
    }

    @Test
    void shouldUpdateTest() {
        final String uuid = randomId();
        final String name = randomName();

        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        final String stepUuid = randomId();
        final String stepName = randomName();

        final StepResult step = new StepResult().setName(stepName);
        lifecycle.startStep(uuid, stepUuid, step);

        final String description = randomName();
        final String fullName = randomName();

        Allure.step("Update test case metadata through lifecycle", stepContext -> {
            stepContext.parameter("testUuid", uuid);
            lifecycle.updateTestCase(uuid, testResult -> testResult.setDescription(description));
            lifecycle.updateTestCase(testResult -> testResult.setFullName(fullName));

            lifecycle.stopStep(stepUuid);

            lifecycle.stopTestCase(uuid);
            lifecycle.writeTestCase(uuid);
        });

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResult actual = captor.getValue();
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("description", description)
                .hasFieldOrPropertyWithValue("name", name)
                .hasFieldOrPropertyWithValue("fullName", fullName);

        assertThat(actual.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(stepName);
    }

    @Test
    void shouldUpdateContainer() {
        final String uuid = randomId();
        final String name = randomName();
        final String newName = randomName();

        final TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setName(name);
        lifecycle.startTestContainer(container);

        lifecycle.updateTestContainer(uuid, c -> c.setName(newName));
        lifecycle.stopTestContainer(uuid);
        lifecycle.writeTestContainer(uuid);

        ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", newName);
    }

    @Test
    void shouldCreateTestFixture() {
        final String uuid = randomId();
        final String name = randomName();

        TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setName(name);

        final String firstUuid = randomId();
        final String firstName = randomName();
        final FixtureResult first = new FixtureResult().setName(firstName);

        final List<String> stepNames = Allure.step("Create and write before fixture with child steps", step -> {
            step.parameter("container uuid", uuid);
            step.parameter("fixture uuid", firstUuid);
            lifecycle.startTestContainer(container);
            lifecycle.startPrepareFixture(uuid, firstUuid, first);

            final String firstStepName = randomStep(firstUuid);
            final String secondStepName = randomStep(firstUuid);

            lifecycle.stopFixture(firstUuid);
            lifecycle.stopTestContainer(uuid);
            lifecycle.writeTestContainer(uuid);

            return List.of(firstStepName, secondStepName);
        });

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResultContainer actual = captor.getValue();
        Allure.step("Verify written container includes before fixture steps", step -> {
            step.parameter("container uuid", uuid);
            assertThat(actual)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("uuid", uuid)
                    .hasFieldOrPropertyWithValue("name", name);

            assertThat(actual.getBefores())
                    .hasSize(1);

            final FixtureResult fixtureResult = actual.getBefores().get(0);
            assertThat(fixtureResult)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("name", firstName);

            assertThat(fixtureResult.getSteps())
                    .hasSize(2)
                    .flatExtracting(StepResult::getName)
                    .containsExactlyElementsOf(stepNames);
        });
    }

    @Test
    void shouldMergeBeforeFixtureMetadataIntoLinkedTest() {
        final String scopeUuid = randomId();
        lifecycle.startScope(new ScopeResult().setUuid(scopeUuid));

        final String fixtureUuid = randomId();
        lifecycle.startBeforeFixture(scopeUuid, fixtureUuid, new FixtureResult().setName(randomName()));

        final Label label = new Label().setName("layer").setValue("api");
        final Link link = new Link().setName("issue").setType("issue").setUrl("https://example.com/ISSUE-1");
        final Parameter parameter = new Parameter().setName("browser").setValue("chrome");
        final String description = randomName();
        final String descriptionHtml = "<p>" + randomName() + "</p>";

        Allure.step("Apply before-fixture metadata to the active scope", step -> {
            step.parameter("scope uuid", scopeUuid);
            step.parameter("fixture uuid", fixtureUuid);
            lifecycle.addLabel(label);
            lifecycle.addLink(link);
            lifecycle.addParameter(parameter);
            lifecycle.setDescription(description);
            lifecycle.setDescriptionHtml(descriptionHtml);
            lifecycle.stopFixture(fixtureUuid);
        });

        final String testUuid = randomId();
        lifecycle.scheduleTestCase(scopeUuid, new TestResult().setUuid(testUuid).setName(randomName()));
        lifecycle.startTestCase(testUuid);
        lifecycle.stopTestCase(testUuid);
        lifecycle.writeTestCase(testUuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResult actual = captor.getValue();
        Allure.step("Verify linked test received before-fixture metadata", step -> {
            step.parameter("test uuid", testUuid);
            assertThat(actual.getLabels())
                    .containsExactly(label);
            assertThat(actual.getLinks())
                    .containsExactly(link);
            assertThat(actual.getParameters())
                    .containsExactly(parameter);
            assertThat(actual)
                    .hasFieldOrPropertyWithValue("description", description)
                    .hasFieldOrPropertyWithValue("descriptionHtml", descriptionHtml);
        });
    }

    @Test
    void shouldNotMergeAfterFixtureMetadataIntoLinkedTest() {
        final String scopeUuid = randomId();
        lifecycle.startScope(new ScopeResult().setUuid(scopeUuid));

        final String fixtureUuid = randomId();
        Allure.step("Apply after-fixture metadata to the active scope", step -> {
            step.parameter("scope uuid", scopeUuid);
            step.parameter("fixture uuid", fixtureUuid);
            lifecycle.startAfterFixture(scopeUuid, fixtureUuid, new FixtureResult().setName(randomName()));
            lifecycle.addLabel(new Label().setName("layer").setValue("api"));
            lifecycle.addParameter(new Parameter().setName("browser").setValue("chrome"));
            lifecycle.stopFixture(fixtureUuid);
        });

        final String testUuid = randomId();
        lifecycle.scheduleTestCase(scopeUuid, new TestResult().setUuid(testUuid).setName(randomName()));
        lifecycle.startTestCase(testUuid);
        lifecycle.stopTestCase(testUuid);
        lifecycle.writeTestCase(testUuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        Allure.step("Verify linked test ignores after-fixture metadata", step -> {
            step.parameter("test uuid", testUuid);
            assertThat(captor.getValue().getLabels())
                    .isEmpty();
            assertThat(captor.getValue().getParameters())
                    .isEmpty();
        });
    }

    @Test
    void shouldRestoreTestContextAfterFixture() {
        final String scopeUuid = randomId();
        lifecycle.startScope(new ScopeResult().setUuid(scopeUuid));

        final String testUuid = randomId();
        lifecycle.scheduleTestCase(scopeUuid, new TestResult().setUuid(testUuid).setName(randomName()));
        lifecycle.startTestCase(testUuid);

        final String fixtureUuid = randomId();
        Allure.step("Run a before fixture while a test case is active", step -> {
            step.parameter("test uuid", testUuid);
            step.parameter("fixture uuid", fixtureUuid);
            lifecycle.startBeforeFixture(scopeUuid, fixtureUuid, new FixtureResult().setName(randomName()));
            randomStep(fixtureUuid);
            lifecycle.stopFixture(fixtureUuid);
        });

        final String testStepUuid = randomId();
        final String testStepName = randomName();
        Allure.step("Create a test step after the fixture completes", step -> {
            step.parameter("test step uuid", testStepUuid);
            lifecycle.startStep(testStepUuid, new StepResult().setName(testStepName));
            lifecycle.stopStep(testStepUuid);
        });

        lifecycle.stopTestCase(testUuid);
        lifecycle.writeTestCase(testUuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        Allure.step("Verify the post-fixture step belongs to the test case", step -> {
            step.parameter("test uuid", testUuid);
            assertThat(captor.getValue().getSteps())
                    .extracting(StepResult::getName)
                    .containsExactly(testStepName);
        });
    }

    @Test
    void shouldAttachAsync() {
        final List<CompletableFuture<InputStream>> features = new CopyOnWriteArrayList<>();

        final String attachment1Name = randomName();
        final String attachment2Name = randomName();

        final String attachment1Content = randomString(100);
        final String attachment2Content = randomString(100);

        final AllureResults writer = RunUtils.runWithinTestContext(() -> {
            features.add(
                    addStreamAttachmentAsync(
                            attachment1Name, "video/mp4", getStreamWithTimeout(2, attachment1Content)
                    )
            );
            features.add(
                    addStreamAttachmentAsync(
                            attachment2Name, "text/plain", getStreamWithTimeout(1, attachment2Content)
                    )
            );

            allOf(features.toArray(new CompletableFuture[0])).join();
        });

        final List<StepResult> attachmentSteps = writer.getTestResults().stream()
                .map(TestResult::getSteps)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(attachmentSteps)
                .extracting(StepResult::getName)
                .containsExactly(attachment1Name, attachment2Name);

        final List<io.qameta.allure.model.Attachment> attachments = attachmentSteps.stream()
                .map(StepResult::getAttachments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(attachments)
                .extracting(io.qameta.allure.model.Attachment::getName, io.qameta.allure.model.Attachment::getType)
                .containsExactly(
                        tuple(attachment1Name, "video/mp4"),
                        tuple(attachment2Name, "text/plain")
                );

        assertThat(attachments)
                .extracting(io.qameta.allure.model.Attachment::getSize)
                .containsOnlyNulls();

        final String[] sources = attachments.stream()
                .map(io.qameta.allure.model.Attachment::getSource)
                .toArray(String[]::new);

        final Map<String, byte[]> attachmentFiles = writer.getAttachments();
        assertThat(attachmentFiles)
                .containsKeys(sources);

        final io.qameta.allure.model.Attachment attachment1 = attachments.stream()
                .filter(attachment -> Objects.equals(attachment.getName(), attachment1Name))
                .findAny()
                .orElseThrow();

        final byte[] actual1 = attachmentFiles.get(attachment1.getSource());

        assertThat(new String(actual1, StandardCharsets.UTF_8))
                .isEqualTo(attachment1Content);

        final Attachment attachment2 = attachments.stream()
                .filter(attachment -> Objects.equals(attachment.getName(), attachment2Name))
                .findAny()
                .orElseThrow();

        final byte[] actual2 = attachmentFiles.get(attachment2.getSource());

        assertThat(new String(actual2, StandardCharsets.UTF_8))
                .isEqualTo(attachment2Content);

    }

    private Supplier<InputStream> getStreamWithTimeout(final long delay, final String content) {
        return () -> {
            try {
                TimeUnit.SECONDS.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread interrupted", e);
            }
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        };
    }

    @Test
    void supportForConcurrentUseOfChildThreads() throws Exception {
        final String uuid = randomId();
        final String name = randomName();

        final int threads = 20;
        final int stepsCount = 1000;

        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);
        lifecycle.startTestCase(uuid);

        final ExecutorService service = Executors.newFixedThreadPool(threads);

        final List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(new StepCall(lifecycle, i, stepsCount));
        }

        List<Future<Void>> futures = Allure.step("Run child thread step lifecycle updates", stepContext -> {
            stepContext.parameter("threads", threads);
            stepContext.parameter("stepsPerThread", stepsCount);
            return service.invokeAll(tasks);
        });
        for (Future<Void> future : futures) {
            future.get();
        }

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        final List<StepResult> steps = captor.getValue().getSteps();
        final int expected = threads * stepsCount;
        assertThat(steps)
                .hasSize(expected);

        assertThat(steps)
                .doesNotContain((StepResult) null);

        final long emptyNameCount = steps.stream()
                .map(StepResult::getName)
                .filter(Objects::isNull)
                .count();

        assertThat(emptyNameCount)
                .describedAs("All steps should have non-empty names")
                .isEqualTo(0);

        final boolean anyMatch = steps.stream()
                .map(StepResult::getName)
                .anyMatch(s -> s.matches("^Step \\d+$"));

        assertThat(anyMatch)
                .describedAs("All steps names should start with Step")
                .isTrue();

        final long countDistinct = steps.stream()
                .map(StepResult::getName)
                .distinct()
                .count();

        assertThat(countDistinct)
                .isEqualTo(expected);

    }

    private String randomStep(String parentUuid) {
        final String uuid = randomId();
        final String name = randomName();
        final StepResult step = new StepResult().setName(name);
        lifecycle.startStep(parentUuid, uuid, step);
        lifecycle.stopStep(uuid);
        return name;
    }

    private static class StepCall implements Callable<Void> {

        private final AllureLifecycle lifecycle;

        private final int id;

        private final int stepsCount;

        private StepCall(final AllureLifecycle lifecycle, final int id, final int stepsCount) {
            this.lifecycle = lifecycle;
            this.id = id;
            this.stepsCount = stepsCount;
        }

        @Override
        public Void call() {
            for (int j = 0; j < stepsCount; j++) {
                final int stepId = id * stepsCount + j;
                final String stepUuid = "step " + stepId;
                final String stepName = "Step " + stepId;
                final StepResult step = new StepResult().setName(stepName);
                lifecycle.startStep(stepUuid, step);
                lifecycle.stopStep(stepUuid);
            }
            return null;
        }
    }
}
