package io.qameta.allure;

import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.model.WithAttachments;
import io.qameta.allure.model.WithSteps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.qameta.allure.AllureConstants.ATTACHMENT_FILE_SUFFIX;

/**
 * The class contains Allure context and methods to change it.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class AllureLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureLifecycle.class);

    private final Map<String, Object> storage = new ConcurrentHashMap<>();

    private final ThreadLocal<LinkedList<String>> currentStepContext =
            InheritableThreadLocal.withInitial(LinkedList::new);

    private final AllureResultsWriter writer;

    public AllureLifecycle(final AllureResultsWriter writer) {
        this.writer = writer;
    }

    public AllureLifecycle() {
        this(getDefaultWriter());
    }

    private static FileSystemResultsWriter getDefaultWriter() {
        final String path = System.getProperty("allure.results.directory", "allure-results");
        return new FileSystemResultsWriter(Paths.get(path));
    }

    public void startTestContainer(final String parentUuid, final TestResultContainer container) {
        get(parentUuid, TestResultContainer.class)
                .getChildren().add(container.getUuid());
        startTestContainer(container);
    }

    public void startTestContainer(final TestResultContainer container) {
        LOGGER.debug("Start test result container {}", container.getUuid());
        put(container.getUuid(), container)
                .withStart(System.currentTimeMillis());
    }

    public void updateTestContainer(final String uuid, final Consumer<TestResultContainer> update) {
        LOGGER.debug("Update test result container {}", uuid);
        update.accept(get(uuid, TestResultContainer.class));
    }

    public void stopTestContainer(final String uuid) {
        LOGGER.debug("Stop test result container {}", uuid);
        get(uuid, TestResultContainer.class)
                .withStop(System.currentTimeMillis());
    }

    public void writeTestContainer(final String uuid) {
        LOGGER.debug("Stop test group {}", uuid);
        writer.write(remove(uuid, TestResultContainer.class));
    }

    public void startBeforeFixture(final String parentUuid, final String uuid, final FixtureResult result) {
        LOGGER.debug("Start test before {} with parent {}", uuid, parentUuid);
        startFixture(parentUuid, uuid, result, TestResultContainer::getBefores);
    }

    public void startAfterFixture(final String parentUuid, final String uuid, final FixtureResult result) {
        LOGGER.debug("Start test after {} with parent {}", uuid, parentUuid);
        startFixture(parentUuid, uuid, result, TestResultContainer::getAfters);
    }

    private void startFixture(final String parentUuid, final String uuid, final FixtureResult result,
                              final Function<TestResultContainer, List<FixtureResult>> fixturesGetter) {
        put(uuid, result)
                .withStage(Stage.RUNNING)
                .withStart(System.currentTimeMillis());
        final TestResultContainer container = get(parentUuid, TestResultContainer.class);
        fixturesGetter.apply(container).add(result);
        currentStepContext.remove();
        currentStepContext.get().push(uuid);
    }

    public void updateFixture(final String uuid, final Consumer<FixtureResult> update) {
        LOGGER.debug("Update test group {}", uuid);
        update.accept(get(uuid, FixtureResult.class));
    }

    public void stopFixture(final String uuid) {
        LOGGER.debug("Stop test before {}", uuid);
        currentStepContext.remove();
        remove(uuid, FixtureResult.class)
                .withStage(Stage.FINISHED)
                .withStop(System.currentTimeMillis());
    }

    public void scheduleTestCase(final String parentUuid, final TestResult result) {
        LOGGER.debug("Add test case {} to {}", result.getUuid(), parentUuid);
        get(parentUuid, TestResultContainer.class)
                .getChildren().add(result.getUuid());
        scheduleTestCase(result);
    }

    public void scheduleTestCase(final TestResult result) {
        LOGGER.debug("Schedule test case {}", result.getUuid());
        put(result.getUuid(), result)
                .withStage(Stage.SCHEDULED);
    }

    public void startTestCase(final String uuid) {
        LOGGER.debug("Start test case {}", uuid);
        get(uuid, TestResult.class)
                .withStage(Stage.RUNNING)
                .withStart(System.currentTimeMillis());
        currentStepContext.remove();
        currentStepContext.get().push(uuid);
    }

    public void updateTestCase(final String uuid, final Consumer<TestResult> update) {
        LOGGER.debug("Update test case {}", uuid);
        update.accept(get(uuid, TestResult.class));
    }

    public void stopTestCase(final String uuid) {
        LOGGER.debug("Stop test case {}", uuid);
        currentStepContext.remove();
        get(uuid, TestResult.class)
                .withStage(Stage.FINISHED)
                .withStop(System.currentTimeMillis());
    }

    public void writeTestCase(final String uuid) {
        LOGGER.debug("Close test case {}", uuid);
        writer.write(remove(uuid, TestResult.class));
    }

    public void addAttachment(final String name, final String type,
                              final String fileExtension, final byte[] body) {
        addAttachment(name, type, fileExtension, new ByteArrayInputStream(body));
    }

    @SuppressWarnings({"PMD.NullAssignment", "PMD.UseObjectForClearerAPI"})
    public void addAttachment(final String name, final String type,
                              final String fileExtension, final InputStream stream) {
        final String uuid = currentStepContext.get().getFirst();
        LOGGER.debug("Adding attachment to item with uuid {}", uuid);
        final String extension = Optional.ofNullable(fileExtension)
                .filter(ext -> !ext.isEmpty())
                .map(ext -> ext.charAt(0) == '.' ? ext : "." + ext)
                .orElse("");
        final String source = UUID.randomUUID().toString() + ATTACHMENT_FILE_SUFFIX + extension;
        final Attachment attachment = new Attachment()
                .withName(isEmpty(name) ? null : name)
                .withType(isEmpty(type) ? null : type)
                .withSource(source);

        writer.write(attachment.getSource(), stream);
        get(uuid, WithAttachments.class).getAttachments().add(attachment);
    }

    public void addStep(final StepResult result) {
        get(currentStepContext.get().getFirst(), WithSteps.class).getSteps().add(result);
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void startStep(final String uuid, final StepResult result) {
        final LinkedList<String> uuids = currentStepContext.get();
        startStep(uuids.isEmpty() ? null : uuids.getFirst(), uuid, result);
    }

    public void startStep(final String parentUuid, final String uuid, final StepResult result) {
        LOGGER.debug("Start step {} with parent {}", uuid, parentUuid);
        put(uuid, result)
                .withStage(Stage.RUNNING)
                .withStart(System.currentTimeMillis());
        currentStepContext.get().push(uuid);

        if (Objects.nonNull(parentUuid)) {
            get(parentUuid, WithSteps.class).getSteps().add(result);
        }
    }

    public void updateStep(final Consumer<StepResult> update) {
        updateStep(currentStepContext.get().getFirst(), update);
    }

    public void updateStep(final String uuid, final Consumer<StepResult> update) {
        LOGGER.debug("Update step {}", uuid);
        update.accept(get(uuid, StepResult.class));
    }

    public void stopStep() {
        stopStep(currentStepContext.get().getFirst());
    }

    public void stopStep(final String uuid) {
        LOGGER.debug("Stop step {}", uuid);
        remove(uuid, StepResult.class)
                .withStage(Stage.FINISHED)
                .withStop(System.currentTimeMillis());
        currentStepContext.get().pop();
    }

    private <T> T put(final String uuid, final T item) {
        Objects.requireNonNull(uuid, "Can't put item to storage: uuid can't be null");
        storage.put(uuid, item);
        return item;
    }

    private <T> T get(final String uuid, final Class<T> clazz) {
        Objects.requireNonNull(uuid, "Can't get item from storage: uuid can't be null");
        final Object obj = Objects.requireNonNull(
                storage.get(uuid),
                String.format("Could not get %s by uuid %s", clazz, uuid)
        );
        return cast(obj, clazz);
    }

    private <T> T remove(final String uuid, final Class<T> clazz) {
        Objects.requireNonNull(uuid, "Can't remove item from storage: uuid can't be null");
        final Object obj = Objects.requireNonNull(
                storage.remove(uuid),
                String.format("Could not remove %s by uuid %s", clazz, uuid)
        );
        return cast(obj, clazz);
    }

    private <T> T cast(final Object obj, final Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        throw new IllegalStateException("Can not cast " + obj + " to " + clazz);
    }

    private boolean isEmpty(final String s) {
        return Objects.isNull(s) || s.isEmpty();
    }
}
