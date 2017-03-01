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
 * @author charlie (Dmitry Baev).
 */
public class AllureLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureLifecycle.class);

    private final Map<String, Object> storage = new ConcurrentHashMap<>();

    private final ThreadLocal<LinkedList<String>> currentStepContext =
            InheritableThreadLocal.withInitial(LinkedList::new);

    private AllureResultsWriter writer;

    public AllureLifecycle(AllureResultsWriter writer) {
        this.writer = writer;
    }

    public AllureLifecycle() {
        this(getDefaultWriter());
    }

    private static FileSystemResultsWriter getDefaultWriter() {
        final String path = System.getProperty("allure.results.directory", "allure-results");
        return new FileSystemResultsWriter(Paths.get(path));
    }

    public void startTestContainer(String parentUuid, TestResultContainer container) {
        get(parentUuid, TestResultContainer.class)
                .getChildren().add(container.getUuid());
        startTestContainer(container);
    }

    public void startTestContainer(TestResultContainer container) {
        LOGGER.info("Start test result container {}", container.getUuid());
        put(container.getUuid(), container)
                .withStart(System.currentTimeMillis());
    }

    public void updateTestContainer(String uuid, Consumer<TestResultContainer> update) {
        LOGGER.info("Update test result container {}", uuid);
        update.accept(get(uuid, TestResultContainer.class));
    }

    public void stopTestContainer(String uuid) {
        LOGGER.info("Update test result container {}", uuid);
        get(uuid, TestResultContainer.class)
                .withStop(System.currentTimeMillis());
    }

    public void writeTestContainer(String uuid) {
        LOGGER.info("Stop test group {}", uuid);
        writer.write(remove(uuid, TestResultContainer.class));
    }

    public void startBeforeFixture(String parentUuid, String uuid, FixtureResult result) {
        LOGGER.info("Start test before {} with parent {}", uuid, parentUuid);
        startFixture(parentUuid, uuid, result, TestResultContainer::getBefores);
    }

    public void startAfterFixture(String parentUuid, String uuid, FixtureResult result) {
        LOGGER.info("Start test after {} with parent {}", uuid, parentUuid);
        startFixture(parentUuid, uuid, result, TestResultContainer::getAfters);
    }

    private void startFixture(String parentUuid, String uuid, FixtureResult result,
                              Function<TestResultContainer, List<FixtureResult>> fixturesGetter) {
        put(uuid, result)
                .withStage(Stage.RUNNING)
                .withStart(System.currentTimeMillis());
        TestResultContainer container = get(parentUuid, TestResultContainer.class);
        fixturesGetter.apply(container).add(result);
        currentStepContext.remove();
        currentStepContext.get().push(uuid);
    }

    public void updateFixture(String uuid, Consumer<FixtureResult> update) {
        LOGGER.info("Update test group {}", uuid);
        update.accept(get(uuid, FixtureResult.class));
    }

    public void stopFixture(String uuid) {
        LOGGER.info("Stop test before {}", uuid);
        currentStepContext.remove();
        remove(uuid, FixtureResult.class)
                .withStage(Stage.FINISHED)
                .withStop(System.currentTimeMillis());
    }

    public void scheduleTestCase(String parentUuid, TestResult result) {
        LOGGER.info("Add test case {} to {}", result.getUuid(), parentUuid);
        get(parentUuid, TestResultContainer.class)
                .getChildren().add(result.getUuid());
        scheduleTestCase(result);
    }

    public void scheduleTestCase(TestResult result) {
        LOGGER.info("Schedule test case {}", result.getUuid());
        put(result.getUuid(), result)
                .withStage(Stage.SCHEDULED);
    }

    public void startTestCase(String uuid) {
        LOGGER.info("Start test case {}", uuid);
        get(uuid, TestResult.class)
                .withStage(Stage.RUNNING)
                .withStart(System.currentTimeMillis());
        currentStepContext.remove();
        currentStepContext.get().push(uuid);
    }

    public void updateTestCase(String uuid, Consumer<TestResult> update) {
        LOGGER.info("Update test case {}", uuid);
        update.accept(get(uuid, TestResult.class));
    }

    public void stopTestCase(String uuid) {
        LOGGER.info("Stop test case {}", uuid);
        currentStepContext.remove();
        get(uuid, TestResult.class)
                .withStage(Stage.FINISHED)
                .withStop(System.currentTimeMillis());
    }

    public void writeTestCase(String uuid) {
        LOGGER.info("Close test case {}", uuid);
        writer.write(remove(uuid, TestResult.class));
    }

    public void addAttachment(String name, String type, String fileExtension, byte[] body) {
        addAttachment(name, type, fileExtension, new ByteArrayInputStream(body));
    }

    public void addAttachment(String name, String type, String fileExtension, InputStream stream) {
        String uuid = currentStepContext.get().getFirst();
        LOGGER.info("Adding attachment to item with uuid {}", uuid);
        String extension = Optional.ofNullable(fileExtension)
                .filter(ext -> !ext.isEmpty())
                .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                .orElse("");
        String source = UUID.randomUUID().toString() + ATTACHMENT_FILE_SUFFIX + extension;
        Attachment attachment = new Attachment()
                .withName(isEmpty(name) ? null : name)
                .withType(isEmpty(type) ? null : type)
                .withSource(source);

        writer.write(attachment.getSource(), stream);
        get(uuid, WithAttachments.class).getAttachments().add(attachment);
    }

    public void addStep(StepResult result) {
        get(currentStepContext.get().getFirst(), WithSteps.class).getSteps().add(result);
    }

    public void startStep(String uuid, StepResult result) {
        LinkedList<String> uuids = currentStepContext.get();
        startStep(uuids.isEmpty() ? null : uuids.getFirst(), uuid, result);
    }

    public void startStep(String parentUuid, String uuid, StepResult result) {
        LOGGER.info("Start step {} with parent {}", uuid, parentUuid);
        put(uuid, result)
                .withStage(Stage.RUNNING)
                .withStart(System.currentTimeMillis());
        currentStepContext.get().push(uuid);

        if (Objects.nonNull(parentUuid)) {
            get(parentUuid, WithSteps.class).getSteps().add(result);
        }
    }

    public void updateStep(Consumer<StepResult> update) {
        updateStep(currentStepContext.get().getFirst(), update);
    }

    public void updateStep(String uuid, Consumer<StepResult> update) {
        LOGGER.info("Update step {}", uuid);
        update.accept(get(uuid, StepResult.class));
    }

    public void stopStep() {
        stopStep(currentStepContext.get().getFirst());
    }

    public void stopStep(String uuid) {
        LOGGER.info("Stop step {}", uuid);
        remove(uuid, StepResult.class)
                .withStage(Stage.FINISHED)
                .withStop(System.currentTimeMillis());
        currentStepContext.get().pop();
    }

    private <T> T put(String uuid, T item) {
        Objects.requireNonNull(uuid, "Uuid can't be null");
        storage.put(uuid, item);
        return item;
    }

    private <T> T get(String uuid, Class<T> clazz) {
        Objects.requireNonNull(uuid, "Uuid can't be null");
        Object obj = Objects.requireNonNull(storage.get(uuid), "Could not find " + clazz + " by uuid " + uuid);
        return cast(obj, clazz);
    }

    private <T> T remove(String uuid, Class<T> clazz) {
        Objects.requireNonNull(uuid, "Uuid can't be null");
        Object obj = Objects.requireNonNull(storage.remove(uuid), "Could not find " + clazz + " by uuid " + uuid);
        return cast(obj, clazz);
    }

    private <T> T cast(Object obj, Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        throw new IllegalStateException("Can not cast " + obj + " to " + clazz);
    }

    private boolean isEmpty(String s) {
        return Objects.isNull(s) || s.isEmpty();
    }
}
