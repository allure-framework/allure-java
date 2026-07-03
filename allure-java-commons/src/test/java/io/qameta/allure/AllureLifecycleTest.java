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
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.model.WithMetadata;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.qameta.allure.Allure.attachmentAsync;
import static io.qameta.allure.test.TestData.randomId;
import static io.qameta.allure.test.TestData.randomName;
import static io.qameta.allure.test.TestData.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
@SuppressWarnings({"deprecation", "removal"})
@IsolatedLifecycle
class AllureLifecycleTest {

    private static final String SCOPE_KEY_NAMESPACE = "test:scope";

    private AllureResultsWriter writer;
    private AllureLifecycle lifecycle;

    @BeforeEach
    public void setUp() {
        writer = Mockito.mock(AllureResultsWriter.class);
        lifecycle = new AllureLifecycle(writer);
    }

    private void applyMetadata(final Consumer<WithMetadata> update) {
        lifecycle.updateTestMetadata(update);
    }

    @Test
    void shouldCreateTest() {
        final String uuid = randomId();
        final String name = randomName();
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTest(testKey, result);
        lifecycle.startTest(testKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);
    }

    @Test
    void shouldCreateScope() {
        final String uuid = randomUuid();
        lifecycle.registerScope(scopeKey(uuid));
        lifecycle.writeScope(scopeKey(uuid));

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isExactlyInstanceOf(TestResultContainer.class)
                .hasFieldOrPropertyWithValue("name", null);
        assertThat(captor.getValue().getUuid())
                .isNotEmpty();
    }

    @Test
    void shouldCreateScopeByExternalKey() {
        final AllureExternalKey key = AllureExternalKey.random(AllureLifecycleTest.class);
        lifecycle.registerScope(key);
        lifecycle.writeScope(key);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isExactlyInstanceOf(TestResultContainer.class)
                .hasFieldOrPropertyWithValue("name", null);
        assertThat(captor.getValue().getUuid())
                .isNotEmpty();
    }

    @Test
    void shouldLinkTestToMultipleScopesByExternalKey() {
        final AllureExternalKey suiteKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final AllureExternalKey classKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final String testName = randomName();
        lifecycle.registerScope(suiteKey);
        lifecycle.registerScope(classKey);
        lifecycle.scheduleTest(List.of(suiteKey, classKey), testKey, new TestResult().setName(testName));
        lifecycle.startTest(testKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);
        lifecycle.writeScope(suiteKey);
        lifecycle.writeScope(classKey);

        final ArgumentCaptor<TestResult> testCaptor = forClass(TestResult.class);
        verify(writer, times(1)).write(testCaptor.capture());
        final String testUuid = testCaptor.getValue().getUuid();
        assertThat(testCaptor.getValue())
                .hasFieldOrPropertyWithValue("name", testName);

        final ArgumentCaptor<TestResultContainer> containerCaptor = forClass(TestResultContainer.class);
        verify(writer, times(2)).write(containerCaptor.capture());
        assertThat(containerCaptor.getAllValues())
                .extracting(TestResultContainer::getChildren)
                .allSatisfy(children -> assertThat(children).containsExactly(testUuid));
    }

    @Test
    void shouldMergeScopeMetadataIntoLiveTestsWhenScopeIsWrittenFirst() {
        final AllureExternalKey scopeKey = scopeKey(randomUuid());
        final AllureExternalKey fixtureKey = fixtureKey(randomId());
        final AllureExternalKey testKey = testKey(randomId());
        lifecycle.registerScope(scopeKey);

        // metadata written during a before fixture lands on the scope
        lifecycle.startBeforeFixture(scopeKey, fixtureKey, new FixtureResult().setName(randomName()));
        lifecycle.updateTestMetadata(
                metadata -> metadata.getLabels()
                        .add(new Label().setName("layer").setValue("rest"))
        );
        lifecycle.stopFixture(fixtureKey);

        lifecycle.scheduleTest(testKey, new TestResult().setName(randomName()));
        lifecycle.startTest(testKey);
        lifecycle.addTestToScope(scopeKey, testKey);
        // the scope is written while the test is still running — TestNG per-method scopes do exactly this
        lifecycle.writeScope(scopeKey);

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());
        assertThat(captor.getValue().getLabels())
                .extracting(Label::getName, Label::getValue)
                .containsExactly(tuple("layer", "rest"));
    }

    @Test
    void shouldLinkScopeToTestAndFixture() {
        final String uuid = randomUuid();
        final String childUuid = randomId();
        final String fixtureUuid = randomId();
        final String fixtureName = randomName();
        final AllureExternalKey scopeKey = scopeKey(uuid);
        final AllureExternalKey childKey = testKey(childUuid);
        final AllureExternalKey fixtureKey = fixtureKey(fixtureUuid);
        lifecycle.registerScope(scopeKey);
        lifecycle.scheduleTest(childKey, new TestResult().setUuid(childUuid).setName(randomName()));
        lifecycle.addTestToScope(scopeKey, childKey);
        lifecycle.startBeforeFixture(scopeKey, fixtureKey, new FixtureResult().setName(fixtureName));
        lifecycle.stopFixture(fixtureKey);
        lifecycle.writeScope(scopeKey);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResultContainer actual = captor.getValue();
        assertThat(actual)
                .isExactlyInstanceOf(TestResultContainer.class)
                .hasFieldOrPropertyWithValue("name", null)
                .hasFieldOrPropertyWithValue("description", null)
                .hasFieldOrPropertyWithValue("descriptionHtml", null);
        assertThat(actual.getUuid())
                .isNotEmpty();
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
    void shouldAddStepsToTests() {
        final String uuid = randomId();
        final String name = randomName();
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTest(testKey, result);
        lifecycle.startTest(testKey);

        final String firstStepName = randomStep(testKey);
        final String secondStepName = randomStep(testKey);

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

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
    void shouldReturnCurrentExternalKeys() {
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final AllureExternalKey stepKey = AllureExternalKey.random(AllureLifecycleTest.class);
        lifecycle.scheduleTest(testKey, new TestResult().setName(randomName()));
        lifecycle.startTest(testKey);

        assertThat(lifecycle.getCurrentRootKey())
                .hasValue(testKey);
        assertThat(lifecycle.getCurrentExecutableKey())
                .hasValue(testKey);

        lifecycle.startStep(stepKey, new StepResult().setName(randomName()));

        assertThat(lifecycle.getCurrentRootKey())
                .hasValue(testKey);
        assertThat(lifecycle.getCurrentExecutableKey())
                .hasValue(stepKey);

        lifecycle.stopStep(stepKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);
    }

    @Test
    void shouldReportCurrentExecutablePresence() {
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final AllureExternalKey stepKey = AllureExternalKey.random(AllureLifecycleTest.class);

        assertThat(lifecycle.getCurrentExecutableKey().isPresent())
                .isFalse();

        lifecycle.scheduleTest(testKey, new TestResult().setName(randomName()));
        lifecycle.startTest(testKey);
        assertThat(lifecycle.getCurrentExecutableKey().isPresent())
                .isTrue();

        lifecycle.startStep(stepKey, new StepResult().setName(randomName()));
        assertThat(lifecycle.getCurrentExecutableKey().isPresent())
                .isTrue();

        lifecycle.stopStep(stepKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);
        assertThat(lifecycle.getCurrentExecutableKey().isPresent())
                .isFalse();
    }

    @Test
    void shouldKeepCurrentExecutableKeyResolvableWheneverExecutableIsRunning() {
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final AllureExternalKey stepKey = AllureExternalKey.random(AllureLifecycleTest.class);

        lifecycle.scheduleTest(testKey, new TestResult().setName(randomName()));
        lifecycle.startTest(testKey);
        assertThat(lifecycle.getCurrentExecutableKey().isPresent()).isTrue();
        assertThat(lifecycle.getCurrentExecutableKey()).isPresent();

        lifecycle.startStep(stepKey, new StepResult().setName(randomName()));
        assertThat(lifecycle.getCurrentExecutableKey().isPresent()).isTrue();
        assertThat(lifecycle.getCurrentExecutableKey()).isPresent();

        lifecycle.stopStep(stepKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);
    }

    @Test
    void shouldUpdateTest() {
        final String uuid = randomId();
        final String name = randomName();

        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTest(testKey, result);
        lifecycle.startTest(testKey);

        final AllureExternalKey stepKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final String stepName = randomName();

        final StepResult step = new StepResult().setName(stepName);
        lifecycle.startStep(stepKey, step);

        final String description = randomName();
        final String fullName = randomName();

        Allure.step("Update test case metadata through lifecycle", stepContext -> {
            stepContext.parameter("testUuid", uuid);
            lifecycle.updateTest(testKey, testResult -> testResult.setDescription(description));
            lifecycle.updateTest(testResult -> testResult.setFullName(fullName));

            lifecycle.stopStep(stepKey);

            lifecycle.stopTest(testKey);
            lifecycle.writeTest(testKey);
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
    void shouldCreateTestFixture() {
        final String scopeUuid = randomUuid();
        final AllureExternalKey scopeKey = scopeKey(scopeUuid);

        final String firstUuid = randomId();
        final AllureExternalKey fixtureKey = fixtureKey(firstUuid);
        final String firstName = randomName();
        final FixtureResult first = new FixtureResult().setName(firstName);

        final List<String> stepNames = Allure.step("Create and write before fixture with child steps", step -> {
            step.parameter("scope uuid", scopeUuid);
            step.parameter("fixture uuid", firstUuid);
            lifecycle.registerScope(scopeKey);
            lifecycle.startBeforeFixture(scopeKey, fixtureKey, first);

            final String firstStepName = randomStep(fixtureKey);
            final String secondStepName = randomStep(fixtureKey);

            lifecycle.stopFixture(fixtureKey);
            lifecycle.writeScope(scopeKey);

            return List.of(firstStepName, secondStepName);
        });

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResultContainer actual = captor.getValue();
        Allure.step("Verify written scope includes before fixture steps", step -> {
            step.parameter("scope uuid", scopeUuid);
            assertThat(actual)
                    .isNotNull();
            assertThat(actual.getUuid())
                    .isNotEmpty();

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
        final String scopeUuid = randomUuid();
        final AllureExternalKey scopeKey = scopeKey(scopeUuid);
        lifecycle.registerScope(scopeKey);

        final String fixtureUuid = randomId();
        final AllureExternalKey fixtureKey = fixtureKey(fixtureUuid);
        lifecycle.startBeforeFixture(scopeKey, fixtureKey, new FixtureResult().setName(randomName()));

        final Label label = new Label().setName("layer").setValue("api");
        final Link link = new Link().setName("issue").setType("issue").setUrl("https://example.com/ISSUE-1");
        final Parameter parameter = new Parameter().setName("browser").setValue("chrome");
        final String description = randomName();
        final String descriptionHtml = "<p>" + randomName() + "</p>";

        Allure.step("Apply before-fixture metadata to the active scope", step -> {
            step.parameter("scope uuid", scopeUuid);
            step.parameter("fixture uuid", fixtureUuid);
            applyMetadata(metadata -> metadata.getLabels().add(label));
            applyMetadata(metadata -> metadata.getLinks().add(link));
            applyMetadata(metadata -> metadata.getParameters().add(parameter));
            applyMetadata(metadata -> metadata.setDescription(description));
            applyMetadata(metadata -> metadata.setDescriptionHtml(descriptionHtml));
            lifecycle.stopFixture(fixtureKey);
        });

        final String testUuid = randomId();
        final AllureExternalKey testKey = testKey(testUuid);
        lifecycle.scheduleTest(List.of(scopeKey), testKey, new TestResult().setUuid(testUuid).setName(randomName()));
        lifecycle.startTest(testKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

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
        final String scopeUuid = randomUuid();
        final AllureExternalKey scopeKey = scopeKey(scopeUuid);
        lifecycle.registerScope(scopeKey);

        final String fixtureUuid = randomId();
        final AllureExternalKey fixtureKey = fixtureKey(fixtureUuid);
        Allure.step("Apply after-fixture metadata to the active scope", step -> {
            step.parameter("scope uuid", scopeUuid);
            step.parameter("fixture uuid", fixtureUuid);
            lifecycle.startAfterFixture(scopeKey, fixtureKey, new FixtureResult().setName(randomName()));
            applyMetadata(metadata -> metadata.getLabels().add(new Label().setName("layer").setValue("api")));
            applyMetadata(metadata -> metadata.getParameters().add(new Parameter().setName("browser").setValue("chrome")));
            lifecycle.stopFixture(fixtureKey);
        });

        final String testUuid = randomId();
        final AllureExternalKey testKey = testKey(testUuid);
        lifecycle.scheduleTest(List.of(scopeKey), testKey, new TestResult().setUuid(testUuid).setName(randomName()));
        lifecycle.startTest(testKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

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
        final String scopeUuid = randomUuid();
        final AllureExternalKey scopeKey = scopeKey(scopeUuid);
        lifecycle.registerScope(scopeKey);

        final String testUuid = randomId();
        final AllureExternalKey testKey = testKey(testUuid);
        lifecycle.scheduleTest(List.of(scopeKey), testKey, new TestResult().setUuid(testUuid).setName(randomName()));
        lifecycle.startTest(testKey);

        final String fixtureUuid = randomId();
        final AllureExternalKey fixtureKey = fixtureKey(fixtureUuid);
        Allure.step("Run a before fixture while a test case is active", step -> {
            step.parameter("test uuid", testUuid);
            step.parameter("fixture uuid", fixtureUuid);
            lifecycle.startBeforeFixture(scopeKey, fixtureKey, new FixtureResult().setName(randomName()));
            randomStep(fixtureKey);
            lifecycle.stopFixture(fixtureKey);
        });

        final String testStepUuid = randomId();
        final String testStepName = randomName();
        Allure.step("Create a test step after the fixture completes", step -> {
            step.parameter("test step uuid", testStepUuid);
            lifecycle.startStep(new StepResult().setName(testStepName));
            lifecycle.stopStep();
        });

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

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
    void shouldBindDetachedContextFromExternalKey() {
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        lifecycle.scheduleTest(testKey, new TestResult().setName(randomName()));
        lifecycle.startTest(testKey);

        final AllureExternalKey parentStepKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final String parentStepName = randomName();
        lifecycle.startStep(parentStepKey, new StepResult().setName(parentStepName));

        final AllureExternalKey current = lifecycle.getCurrentExecutableKey().orElseThrow();
        final AllureExternalKey childStepKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final String childStepName = randomName();
        try (AllureThreadBinding ignored = lifecycle.bindDetached(current)) {
            lifecycle.startStep(childStepKey, new StepResult().setName(childStepName));
            lifecycle.stopStep(childStepKey);
        }

        assertThat(lifecycle.getCurrentExecutableKey())
                .hasValue(parentStepKey);

        lifecycle.stopStep(parentStepKey);
        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        final StepResult parentStep = captor.getValue().getSteps().get(0);
        assertThat(parentStep.getName())
                .isEqualTo(parentStepName);
        assertThat(parentStep.getSteps())
                .extracting(StepResult::getName)
                .containsExactly(childStepName);
    }

    @Test
    void shouldNotDisturbThreadContextWhenManualStepRunsOnWorker() throws Exception {
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        lifecycle.scheduleTest(testKey, new TestResult().setName(randomName()));
        lifecycle.startTest(testKey);

        assertThat(lifecycle.getCurrentExecutableKey())
                .as("the test is current on the test thread")
                .hasValue(testKey);

        final AllureExternalKey stepKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final String stepName = randomName();
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            // capture-now-apply-later: a manual step opened and closed entirely on a worker thread
            worker.submit(() -> {
                lifecycle.startStep(testKey, stepKey, new StepResult().setName(stepName));
                lifecycle.stopStep(stepKey);
            }).get(1, TimeUnit.SECONDS);
        } finally {
            worker.shutdownNow();
        }

        assertThat(lifecycle.getCurrentExecutableKey())
                .as("manual key-based step on a worker leaves the test thread's context untouched")
                .hasValue(testKey);

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());
        assertThat(captor.getValue().getSteps())
                .as("the manual step is linked under the test")
                .extracting(StepResult::getName)
                .containsExactly(stepName);
    }

    @Test
    void shouldBindMissingContextAsNoopUntilClosed() {
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final AllureExternalKey missingKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final AllureExternalKey orphanStepKey = AllureExternalKey.random(AllureLifecycleTest.class);
        lifecycle.scheduleTest(testKey, new TestResult().setName(randomName()));
        lifecycle.startTest(testKey);

        try (AllureThreadBinding ignored = lifecycle.bindDetached(missingKey)) {
            assertThat(lifecycle.getCurrentExecutableKey())
                    .isEmpty();
            lifecycle.startStep(orphanStepKey, new StepResult().setName(randomName()));
        }

        assertThat(lifecycle.getCurrentExecutableKey())
                .hasValue(testKey);

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());
        assertThat(captor.getValue().getSteps())
                .isEmpty();
    }

    @Test
    void shouldAttachAsync() {
        final String attachment1Name = randomName();
        final String attachment2Name = randomName();

        final String attachment1Content = randomString(100);
        final String attachment2Content = randomString(100);

        final AllureResults writer = RunUtils.runWithinTestContext(() -> {
            attachmentAsync(
                    attachment1Name,
                    "video/mp4",
                    CompletableFuture.supplyAsync(getStreamWithTimeout(2, attachment1Content))
            );
            attachmentAsync(
                    attachment2Name,
                    "text/plain",
                    CompletableFuture.supplyAsync(getStreamWithTimeout(1, attachment2Content))
            );
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

    @Test
    void shouldAddAttachmentAsyncByKey() throws Exception {
        final String uuid = randomId();
        final String name = randomName();
        final String content = randomString(100);
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final TestResult result = new TestResult().setUuid(uuid).setName(randomName());
        lifecycle.scheduleTest(testKey, result);
        lifecycle.startTest(testKey);

        final CompletableFuture<Void> future = lifecycle.addAttachmentAsync(
                testKey,
                name,
                "text/plain",
                CompletableFuture.supplyAsync(getStreamWithTimeout(1, content)),
                AttachmentOptions.empty()
        );

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

        assertThat(future.isDone())
                .isTrue();

        final ArgumentCaptor<TestResult> testResultCaptor = forClass(TestResult.class);
        verify(writer, times(1)).write(testResultCaptor.capture());

        final List<Attachment> attachments = testResultCaptor.getValue().getAttachments();
        assertThat(attachments)
                .hasSize(1);

        final Attachment attachment = attachments.get(0);
        assertThat(attachment)
                .hasFieldOrPropertyWithValue("name", name)
                .hasFieldOrPropertyWithValue("type", "text/plain");
        assertThat(attachment.getSource())
                .endsWith(".txt");

        final ArgumentCaptor<String> sourceCaptor = forClass(String.class);
        final ArgumentCaptor<InputStream> streamCaptor = forClass(InputStream.class);
        verify(writer, times(1)).write(sourceCaptor.capture(), streamCaptor.capture());

        assertThat(sourceCaptor.getValue())
                .isEqualTo(attachment.getSource());
        assertThat(new String(streamCaptor.getValue().readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo(content);
    }

    @Test
    void shouldIgnoreAttachmentWithoutWriteOwner() {
        final String content = randomString(100);

        Allure.step("Verify sync attachment is ignored without lifecycle context", () -> {
            lifecycle.addAttachment(
                    randomName(),
                    "text/plain",
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                    AttachmentOptions.empty()
            );

            verify(writer, times(0)).write(Mockito.anyString(), Mockito.any(InputStream.class));
        });
    }

    @Test
    void shouldIgnoreAsyncAttachmentWithoutWriteOwner() throws Exception {
        final AllureExternalKey key = AllureExternalKey.random(AllureLifecycleTest.class);
        final String content = randomString(100);
        final CompletableFuture<InputStream> body = new CompletableFuture<>();

        Allure.step("Verify async attachment is ignored without lifecycle context", () -> {
            final CompletableFuture<Void> future = lifecycle.addAttachmentAsync(
                    key,
                    randomName(),
                    "text/plain",
                    body,
                    AttachmentOptions.empty()
            );

            assertThat(future.get(1, TimeUnit.SECONDS))
                    .isNull();

            body.complete(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            verify(writer, times(0)).write(Mockito.anyString(), Mockito.any(InputStream.class));
        });
    }

    @Test
    void shouldWaitForAsyncStepAttachmentOnTestWrite() throws Exception {
        final String uuid = randomId();
        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final AllureExternalKey stepKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final String attachmentName = randomName();
        final String content = randomString(100);
        final CompletableFuture<InputStream> body = new CompletableFuture<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        lifecycle.scheduleTest(testKey, new TestResult().setUuid(uuid).setName(randomName()));
        lifecycle.startTest(testKey);
        lifecycle.startStep(testKey, stepKey, new StepResult().setName(randomName()));
        lifecycle.addAttachmentAsync(stepKey, attachmentName, "text/plain", body, AttachmentOptions.empty());

        try {
            final Future<?> stopStep = executor.submit(() -> lifecycle.stopStep(stepKey));
            stopStep.get(1, TimeUnit.SECONDS);

            final Future<?> stopTest = executor.submit(() -> lifecycle.stopTest(testKey));
            stopTest.get(1, TimeUnit.SECONDS);

            // the write waits for the pending attachment body before serializing the result,
            // so the result file on disk is a completion marker
            final Future<?> writeTest = executor.submit(() -> lifecycle.writeTest(testKey));
            verify(writer, Mockito.after(200).never()).write(Mockito.any(TestResult.class));
            assertThat(writeTest.isDone())
                    .isFalse();

            body.complete(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            writeTest.get(1, TimeUnit.SECONDS);

            final ArgumentCaptor<TestResult> testResultCaptor = forClass(TestResult.class);
            final ArgumentCaptor<String> sourceCaptor = forClass(String.class);
            final ArgumentCaptor<InputStream> streamCaptor = forClass(InputStream.class);
            final InOrder inOrder = Mockito.inOrder(writer);
            inOrder.verify(writer).write(sourceCaptor.capture(), streamCaptor.capture());
            inOrder.verify(writer).write(testResultCaptor.capture());

            final Attachment attachment = testResultCaptor.getValue().getSteps().get(0).getAttachments().get(0);
            assertThat(attachment)
                    .hasFieldOrPropertyWithValue("name", attachmentName)
                    .hasFieldOrPropertyWithValue("type", "text/plain");

            assertThat(sourceCaptor.getValue())
                    .isEqualTo(attachment.getSource());
            assertThat(new String(streamCaptor.getValue().readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo(content);
        } finally {
            body.complete(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            executor.shutdownNow();
        }
    }

    @Test
    void shouldWaitForAsyncFixtureAttachmentOnScopeWrite() throws Exception {
        final String scopeUuid = randomId();
        final AllureExternalKey scopeKey = scopeKey(scopeUuid);
        final String fixtureUuid = randomId();
        final AllureExternalKey fixtureKey = fixtureKey(fixtureUuid);
        final String attachmentName = randomName();
        final String content = randomString(100);
        final CompletableFuture<InputStream> body = new CompletableFuture<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        lifecycle.registerScope(scopeKey);
        lifecycle.startBeforeFixture(scopeKey, fixtureKey, new FixtureResult().setName(randomName()));
        lifecycle.addAttachmentAsync(fixtureKey, attachmentName, "text/plain", body, AttachmentOptions.empty());

        try {
            final Future<?> stopFixture = executor.submit(() -> lifecycle.stopFixture(fixtureKey));
            stopFixture.get(1, TimeUnit.SECONDS);

            // the write waits for the pending attachment body before serializing the container,
            // so the container file on disk is a completion marker
            final Future<?> writeContainer = executor.submit(() -> lifecycle.writeScope(scopeKey));
            verify(writer, Mockito.after(200).never()).write(Mockito.any(TestResultContainer.class));
            assertThat(writeContainer.isDone())
                    .isFalse();

            body.complete(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            writeContainer.get(1, TimeUnit.SECONDS);

            final ArgumentCaptor<TestResultContainer> containerCaptor = forClass(TestResultContainer.class);
            final ArgumentCaptor<String> sourceCaptor = forClass(String.class);
            final ArgumentCaptor<InputStream> streamCaptor = forClass(InputStream.class);
            final InOrder inOrder = Mockito.inOrder(writer);
            inOrder.verify(writer).write(sourceCaptor.capture(), streamCaptor.capture());
            inOrder.verify(writer).write(containerCaptor.capture());

            final Attachment attachment = containerCaptor.getValue().getBefores().get(0).getAttachments().get(0);
            assertThat(attachment)
                    .hasFieldOrPropertyWithValue("name", attachmentName)
                    .hasFieldOrPropertyWithValue("type", "text/plain");

            assertThat(sourceCaptor.getValue())
                    .isEqualTo(attachment.getSource());
            assertThat(new String(streamCaptor.getValue().readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo(content);
        } finally {
            body.complete(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            executor.shutdownNow();
        }
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

        final AllureExternalKey testKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTest(testKey, result);
        lifecycle.startTest(testKey);
        lifecycle.startTest(testKey);

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

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);

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

    private AllureExternalKey scopeKey(final String uuid) {
        return AllureExternalKey.of(AllureLifecycleTest.class, SCOPE_KEY_NAMESPACE, uuid);
    }

    private AllureExternalKey testKey(final String uuid) {
        return AllureExternalKey.of(AllureLifecycleTest.class, "test", uuid);
    }

    private AllureExternalKey fixtureKey(final String uuid) {
        return AllureExternalKey.of(AllureLifecycleTest.class, "fixture", uuid);
    }

    private String randomUuid() {
        return UUID.randomUUID().toString();
    }

    private String randomStep(final AllureExternalKey parentKey) {
        final AllureExternalKey stepKey = AllureExternalKey.random(AllureLifecycleTest.class);
        final String name = randomName();
        final StepResult step = new StepResult().setName(name);
        lifecycle.startStep(parentKey, stepKey, step);
        lifecycle.stopStep(stepKey);
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
                final AllureExternalKey stepKey = AllureExternalKey.of(AllureLifecycleTest.class, "step", stepId);
                final String stepName = "Step " + stepId;
                final StepResult step = new StepResult().setName(stepName);
                lifecycle.startStep(stepKey, step);
                lifecycle.stopStep();
            }
            return null;
        }
    }
}
