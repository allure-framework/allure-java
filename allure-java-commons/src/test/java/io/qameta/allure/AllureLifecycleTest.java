package io.qameta.allure;

import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.test.AllureResultsWriterStub;
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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.qameta.allure.Allure.addStreamAttachmentAsync;
import static io.qameta.allure.Allure.setLifecycle;
import static java.util.concurrent.CompletableFuture.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureLifecycleTest {

    private AllureResultsWriter writer;
    private AllureLifecycle lifecycle;

    @BeforeEach
    public void setUp() {
        writer = Mockito.mock(AllureResultsWriter.class);
        lifecycle = new AllureLifecycle(writer);
    }

    @Test
    void shouldCreateTest() {
        final String uuid = random(String.class);
        final String name = random(String.class);
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
        final String uuid = random(String.class);
        final String name = random(String.class);
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
    void shouldCreateChildTestContainer() {
        final String parentUuid = random(String.class);
        final String parentName = random(String.class);
        final TestResultContainer parent = new TestResultContainer()
                .setUuid(parentUuid)
                .setName(parentName);
        lifecycle.startTestContainer(parent);

        final String childUuid = random(String.class);
        final String childName = random(String.class);
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
        final String uuid = random(String.class);
        final String name = random(String.class);
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
        final String uuid = random(String.class);
        final String name = random(String.class);

        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        final String stepUuid = random(String.class);
        final String stepName = random(String.class);

        final StepResult step = new StepResult().setName(stepName);
        lifecycle.startStep(uuid, stepUuid, step);

        final String description = random(String.class);
        final String fullName = random(String.class);

        lifecycle.updateTestCase(uuid, testResult -> testResult.setDescription(description));
        lifecycle.updateTestCase(testResult -> testResult.setFullName(fullName));

        lifecycle.stopStep(stepUuid);

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

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
        final String uuid = random(String.class);
        final String name = random(String.class);
        final String newName = random(String.class);

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
        final String uuid = random(String.class);
        final String name = random(String.class);

        TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setName(name);
        lifecycle.startTestContainer(container);

        final String firstUuid = random(String.class);
        final String firstName = random(String.class);
        final FixtureResult first = new FixtureResult().setName(firstName);

        lifecycle.startPrepareFixture(uuid, firstUuid, first);

        final String firstStepName = randomStep(firstUuid);
        final String secondStepName = randomStep(firstUuid);

        lifecycle.stopFixture(firstUuid);

        lifecycle.stopTestContainer(uuid);
        lifecycle.writeTestContainer(uuid);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResultContainer actual = captor.getValue();
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
                .containsExactly(firstStepName, secondStepName);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void shouldAttachAsync() {
        final List<CompletableFuture<InputStream>> features = new CopyOnWriteArrayList<>();
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();

        final AllureLifecycle lifecycle = new AllureLifecycle(writer);
        setLifecycle(lifecycle);

        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        final String attachment1Content = random(String.class);
        final String attachment2Content = random(String.class);

        final String attachment1Name = random(String.class);
        final String attachment2Name = random(String.class);

        features.add(addStreamAttachmentAsync(
                attachment1Name, "video/mp4", getStreamWithTimeout(2, attachment1Content)));
        features.add(addStreamAttachmentAsync(
                attachment2Name, "text/plain", getStreamWithTimeout(1, attachment2Content)));

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        allOf(features.toArray(new CompletableFuture[0])).join();

        final List<io.qameta.allure.model.Attachment> attachments = writer.getTestResults().stream()
                .map(TestResult::getAttachments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(attachments)
                .extracting(io.qameta.allure.model.Attachment::getName, io.qameta.allure.model.Attachment::getType)
                .containsExactly(
                        tuple(attachment1Name, "video/mp4"),
                        tuple(attachment2Name, "text/plain")
                );

        final String[] sources = attachments.stream()
                .map(io.qameta.allure.model.Attachment::getSource)
                .toArray(String[]::new);


        final Map<String, byte[]> attachmentFiles = writer.getAttachments();
        assertThat(attachmentFiles)
                .containsKeys(sources);

        final io.qameta.allure.model.Attachment attachment1 = attachments.stream()
                .filter(attachment -> Objects.equals(attachment.getName(), attachment1Name))
                .findAny()
                .get();

        final byte[] actual1 = attachmentFiles.get(attachment1.getSource());

        assertThat(new String(actual1, StandardCharsets.UTF_8))
                .isEqualTo(attachment1Content);

        final Attachment attachment2 = attachments.stream()
                .filter(attachment -> Objects.equals(attachment.getName(), attachment2Name))
                .findAny()
                .get();

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
        final String uuid = random(String.class);
        final String name = random(String.class);

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

        List<Future<Void>> futures = service.invokeAll(tasks);
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
        final String uuid = random(String.class);
        final String name = random(String.class);
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