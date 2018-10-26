package io.qameta.allure;

import io.qameta.allure.internal.AllureStorage;
import io.qameta.allure.listener.ContainerLifecycleListener;
import io.qameta.allure.listener.FixtureLifecycleListener;
import io.qameta.allure.listener.LifecycleNotifier;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.model.WithAttachments;
import io.qameta.allure.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;

import static io.qameta.allure.AllureConstants.ATTACHMENT_FILE_SUFFIX;
import static io.qameta.allure.util.ServiceLoaderUtils.load;

/**
 * The class contains Allure context and methods to change it.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class AllureLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureLifecycle.class);

    private final AllureResultsWriter writer;

    private final AllureStorage storage;

    private final LifecycleNotifier notifier;

    public AllureLifecycle() {
        this(getDefaultWriter());
    }

    public AllureLifecycle(final AllureResultsWriter writer) {
        final ClassLoader classLoader = getClass().getClassLoader();
        this.notifier = new LifecycleNotifier(
                load(ContainerLifecycleListener.class, classLoader),
                load(TestLifecycleListener.class, classLoader),
                load(FixtureLifecycleListener.class, classLoader),
                load(StepLifecycleListener.class, classLoader)
        );
        this.writer = writer;
        this.storage = new AllureStorage();
    }

    public void startTestContainer(final String parentUuid, final TestResultContainer container) {
        updateTestContainer(parentUuid, found -> found.getChildren().add(container.getUuid()));
        startTestContainer(container);
    }

    public void startTestContainer(final TestResultContainer container) {
        notifier.beforeContainerStart(container);
        container.setStart(System.currentTimeMillis());
        storage.addContainer(container);
        notifier.afterContainerStart(container);
    }

    public void updateTestContainer(final String uuid, final Consumer<TestResultContainer> update) {
        storage.getContainer(uuid).ifPresent(container -> {
            notifier.beforeContainerUpdate(container);
            update.accept(container);
            notifier.afterContainerUpdate(container);
        });
    }

    public void stopTestContainer(final String uuid) {
        storage.getContainer(uuid).ifPresent(container -> {
            notifier.beforeContainerStop(container);
            container.setStop(System.currentTimeMillis());
            notifier.afterContainerUpdate(container);
        });
    }

    public void writeTestContainer(final String uuid) {
        storage.removeContainer(uuid).ifPresent(container -> {
            notifier.beforeContainerWrite(container);
            writer.write(container);
            notifier.afterContainerWrite(container);
        });
    }

    public void startPrepareFixture(final String parentUuid, final String uuid, final FixtureResult result) {
        notifier.beforeFixtureStart(result);
        updateTestContainer(parentUuid, container -> container.getBefores().add(result));
        startFixture(uuid, result);
        notifier.afterFixtureStart(result);
    }

    public void startTearDownFixture(final String parentUuid, final String uuid, final FixtureResult result) {
        notifier.beforeFixtureStart(result);
        updateTestContainer(parentUuid, container -> container.getAfters().add(result));
        startFixture(uuid, result);
        notifier.afterFixtureStart(result);
    }

    private void startFixture(final String uuid, final FixtureResult result) {
        storage.addFixture(uuid, result);
        result.setStage(Stage.RUNNING);
        result.setStart(System.currentTimeMillis());
        storage.clearStepContext();
        storage.startStep(uuid);
    }

    public void updateFixture(final Consumer<FixtureResult> update) {
        updateFixture(storage.getRootStep(), update);
    }

    public void updateFixture(final String uuid, final Consumer<FixtureResult> update) {
        storage.getFixture(uuid).ifPresent(fixture -> {
            notifier.beforeFixtureUpdate(fixture);
            update.accept(fixture);
            notifier.afterFixtureUpdate(fixture);
        });
    }

    public void stopFixture(final String uuid) {
        storage.removeFixture(uuid).ifPresent(fixture -> {
            notifier.beforeFixtureStop(fixture);
            storage.clearStepContext();
            fixture.setStage(Stage.FINISHED);
            fixture.setStop(System.currentTimeMillis());
            notifier.afterFixtureStop(fixture);
        });
    }

    public Optional<String> getCurrentTestCase() {
        return Optional.ofNullable(storage.getRootStep());
    }

    public void scheduleTestCase(final String parentUuid, final TestResult result) {
        updateTestContainer(parentUuid, container -> container.getChildren().add(result.getUuid()));
        scheduleTestCase(result);
    }

    public void scheduleTestCase(final TestResult result) {
        notifier.beforeTestSchedule(result);
        result.setStage(Stage.SCHEDULED);
        storage.addTestResult(result);
        notifier.afterTestSchedule(result);
    }

    public void startTestCase(final String uuid) {
        storage.getTestResult(uuid).ifPresent(testResult -> {
            notifier.beforeTestStart(testResult);
            testResult
                    .setStage(Stage.RUNNING)
                    .setStart(System.currentTimeMillis());
            storage.clearStepContext();
            storage.startStep(uuid);
            notifier.afterTestStart(testResult);
        });
    }

    public void updateTestCase(final Consumer<TestResult> update) {
        final String uuid = storage.getRootStep();
        updateTestCase(uuid, update);
    }

    public void updateTestCase(final String uuid, final Consumer<TestResult> update) {
        storage.getTestResult(uuid).ifPresent(testResult -> {
            notifier.beforeTestUpdate(testResult);
            update.accept(testResult);
            notifier.afterTestUpdate(testResult);
        });
    }

    public void stopTestCase(final String uuid) {
        storage.getTestResult(uuid).ifPresent(testResult -> {
            notifier.beforeTestStop(testResult);
            testResult
                    .setStage(Stage.FINISHED)
                    .setStop(System.currentTimeMillis());
            storage.clearStepContext();
            notifier.afterTestStop(testResult);
        });
    }

    public void writeTestCase(final String uuid) {
        storage.removeTestResult(uuid).ifPresent(testResult -> {
            notifier.beforeTestWrite(testResult);
            writer.write(testResult);
            notifier.afterTestWrite(testResult);
        });
    }

    public void startStep(final String uuid, final StepResult result) {
        storage.getCurrentStep().ifPresent(parentUuid -> startStep(parentUuid, uuid, result));
    }

    public void startStep(final String parentUuid, final String uuid, final StepResult result) {
        notifier.beforeStepStart(result);
        result.setStage(Stage.RUNNING);
        result.setStart(System.currentTimeMillis());
        storage.startStep(uuid);
        storage.addStep(parentUuid, uuid, result);
        notifier.afterStepStart(result);
    }

    public void updateStep(final Consumer<StepResult> update) {
        storage.getCurrentStep().ifPresent(uuid -> updateStep(uuid, update));
    }

    public void updateStep(final String uuid, final Consumer<StepResult> update) {
        storage.getStep(uuid).ifPresent(step -> {
            notifier.beforeStepUpdate(step);
            update.accept(step);
            notifier.afterStepUpdate(step);
        });
    }

    public void stopStep() {
        storage.getCurrentStep().ifPresent(this::stopStep);
    }

    public void stopStep(final String uuid) {
        storage.removeStep(uuid).ifPresent(step -> {
            notifier.beforeStepStop(step);
            step.setStage(Stage.FINISHED);
            step.setStop(System.currentTimeMillis());
            storage.stopStep();
            notifier.afterStepStop(step);
        });
    }

    public void addAttachment(final String name, final String type,
                              final String fileExtension, final byte[] body) {
        addAttachment(name, type, fileExtension, new ByteArrayInputStream(body));
    }

    public void addAttachment(final String name, final String type,
                              final String fileExtension, final InputStream stream) {
        writeAttachment(prepareAttachment(name, type, fileExtension), stream);
    }

    @SuppressWarnings({"PMD.NullAssignment", "PMD.UseObjectForClearerAPI"})
    public String prepareAttachment(final String name, final String type, final String fileExtension) {
        final Optional<String> currentStep = storage.getCurrentStep();
        currentStep.ifPresent(uuid -> LOGGER.debug("Adding attachment to item with uuid {}", uuid));
        final String extension = Optional.ofNullable(fileExtension)
                .filter(ext -> !ext.isEmpty())
                .map(ext -> ext.charAt(0) == '.' ? ext : "." + ext)
                .orElse("");
        final String source = UUID.randomUUID().toString() + ATTACHMENT_FILE_SUFFIX + extension;
        final Attachment attachment = new Attachment()
                .setName(isEmpty(name) ? null : name)
                .setType(isEmpty(type) ? null : type)
                .setSource(source);

        currentStep.flatMap(uuid -> storage.get(uuid, WithAttachments.class))
                .ifPresent(withAttachments -> withAttachments.getAttachments().add(attachment));
        return attachment.getSource();
    }

    public void writeAttachment(final String attachmentSource, final InputStream stream) {
        writer.write(attachmentSource, stream);
    }

    private boolean isEmpty(final String s) {
        return Objects.isNull(s) || s.isEmpty();
    }

    private static FileSystemResultsWriter getDefaultWriter() {
        final Properties properties = PropertiesUtils.loadAllureProperties();
        final String path = properties.getProperty("allure.results.directory", "allure-results");
        return new FileSystemResultsWriter(Paths.get(path));
    }
}
