package io.qameta.allure;

import io.qameta.allure.internal.AllureStorage;
import io.qameta.allure.internal.AllureThreadContext;
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
import io.qameta.allure.model.WithSteps;
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
@SuppressWarnings({"PMD.TooManyMethods", "unused"})
public class AllureLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureLifecycle.class);

    private final AllureResultsWriter writer;

    private final AllureStorage storage;

    private final AllureThreadContext threadContext;

    private final LifecycleNotifier notifier;

    /**
     * Creates a new lifecycle with default results writer. Shortcut
     * for {@link #AllureLifecycle(AllureResultsWriter)}
     */
    public AllureLifecycle() {
        this(getDefaultWriter());
    }

    /**
     * Creates a new lifecycle instance with specified {@link AllureResultsWriter}.
     *
     * @param writer the results writer.
     */
    public AllureLifecycle(final AllureResultsWriter writer) {
        this(writer, getDefaultNotifier());
    }

    /**
     * Creates a new lifecycle instance with specified {@link AllureResultsWriter}
     * and {@link LifecycleNotifier}.
     *
     * @param writer the results writer.
     */
    AllureLifecycle(final AllureResultsWriter writer, final LifecycleNotifier lifecycleNotifier) {
        this.notifier = lifecycleNotifier;
        this.writer = writer;
        this.storage = new AllureStorage();
        this.threadContext = new AllureThreadContext();
    }

    /**
     * Starts test container with specified parent container.
     *
     * @param containerUuid the uuid of parent container.
     * @param container     the container.
     */
    public void startTestContainer(final String containerUuid, final TestResultContainer container) {
        storage.getContainer(containerUuid).ifPresent(parent -> {
            synchronized (storage) {
                parent.getChildren().add(container.getUuid());
            }
        });
        startTestContainer(container);
    }

    /**
     * Starts test container.
     *
     * @param container the container.
     */
    public void startTestContainer(final TestResultContainer container) {
        notifier.beforeContainerStart(container);
        container.setStart(System.currentTimeMillis());
        storage.put(container.getUuid(), container);
        notifier.afterContainerStart(container);
    }

    /**
     * Updates test container.
     *
     * @param uuid   the uuid of container.
     * @param update the update function.
     */
    public void updateTestContainer(final String uuid, final Consumer<TestResultContainer> update) {
        final Optional<TestResultContainer> found = storage.getContainer(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not update test container: container with uuid {} not found", uuid);
            return;
        }
        final TestResultContainer container = found.get();
        notifier.beforeContainerUpdate(container);
        update.accept(container);
        notifier.afterContainerUpdate(container);
    }

    /**
     * Stops test container by given uuid.
     *
     * @param uuid the uuid of container.
     */
    public void stopTestContainer(final String uuid) {
        final Optional<TestResultContainer> found = storage.getContainer(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not stop test container: container with uuid {} not found", uuid);
            return;
        }
        final TestResultContainer container = found.get();
        notifier.beforeContainerStop(container);
        container.setStop(System.currentTimeMillis());
        notifier.afterContainerUpdate(container);
    }

    /**
     * Writes test container with given uuid.
     *
     * @param uuid the uuid of container.
     */
    public void writeTestContainer(final String uuid) {
        final Optional<TestResultContainer> found = storage.getContainer(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not write test container: container with uuid {} not found", uuid);
            return;
        }
        final TestResultContainer container = found.get();
        notifier.beforeContainerWrite(container);
        writer.write(container);

        storage.remove(uuid);
        notifier.afterContainerWrite(container);
    }

    /**
     * Start a new prepare fixture with given parent.
     *
     * @param containerUuid the uuid of parent container.
     * @param uuid          the fixture uuid.
     * @param result        the fixture.
     */
    public void startPrepareFixture(final String containerUuid, final String uuid, final FixtureResult result) {
        storage.getContainer(containerUuid).ifPresent(container -> {
            synchronized (storage) {
                container.getBefores().add(result);
            }
        });
        notifier.beforeFixtureStart(result);
        startFixture(uuid, result);
        notifier.afterFixtureStart(result);
    }

    /**
     * Start a new tear down fixture with given parent.
     *
     * @param containerUuid the uuid of parent container.
     * @param uuid          the fixture uuid.
     * @param result        the fixture.
     */
    public void startTearDownFixture(final String containerUuid, final String uuid, final FixtureResult result) {
        storage.getContainer(containerUuid).ifPresent(container -> {
            synchronized (storage) {
                container.getAfters().add(result);
            }
        });

        notifier.beforeFixtureStart(result);
        startFixture(uuid, result);
        notifier.afterFixtureStart(result);
    }

    /**
     * Start a new fixture with given uuid.
     *
     * @param uuid   the uuid of fixture.
     * @param result the test fixture.
     */
    private void startFixture(final String uuid, final FixtureResult result) {
        storage.put(uuid, result);
        result.setStage(Stage.RUNNING);
        result.setStart(System.currentTimeMillis());
        threadContext.clear();
        threadContext.start(uuid);
    }

    /**
     * Updates current running fixture. Shortcut for {@link #updateFixture(String, Consumer)}.
     *
     * @param update the update function.
     */
    public void updateFixture(final Consumer<FixtureResult> update) {
        final Optional<String> root = threadContext.getRoot();
        if (!root.isPresent()) {
            LOGGER.error("Could not update test fixture: no test fixture running");
            return;
        }
        final String uuid = root.get();
        updateFixture(uuid, update);
    }

    /**
     * Updates fixture by given uuid.
     *
     * @param uuid   the uuid of fixture.
     * @param update the update function.
     */
    public void updateFixture(final String uuid, final Consumer<FixtureResult> update) {
        final Optional<FixtureResult> found = storage.getFixture(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not update test fixture: test fixture with uuid {} not found", uuid);
            return;
        }
        final FixtureResult fixture = found.get();

        notifier.beforeFixtureUpdate(fixture);
        update.accept(fixture);
        notifier.afterFixtureUpdate(fixture);
    }

    /**
     * Stops fixture by given uuid.
     *
     * @param uuid the uuid of fixture.
     */
    public void stopFixture(final String uuid) {
        final Optional<FixtureResult> found = storage.getFixture(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not stop test fixture: test fixture with uuid {} not found", uuid);
            return;
        }
        final FixtureResult fixture = found.get();

        notifier.beforeFixtureStop(fixture);
        fixture.setStage(Stage.FINISHED);
        fixture.setStop(System.currentTimeMillis());

        storage.remove(uuid);
        threadContext.clear();

        notifier.afterFixtureStop(fixture);
    }

    /**
     * Returns uuid of current running test case if any.
     *
     * @return the uuid of current running test case.
     */
    public Optional<String> getCurrentTestCase() {
        return threadContext.getRoot();
    }

    /**
     * Returns uuid of current running test case or step if any.
     *
     * @return the uuid of current running test case or step.
     */
    public Optional<String> getCurrentTestCaseOrStep() {
        return threadContext.getCurrent();
    }

    /**
     * Schedules test case with given parent.
     *
     * @param containerUuid the uuid of container.
     * @param result        the test case to schedule.
     */
    public void scheduleTestCase(final String containerUuid, final TestResult result) {
        storage.getContainer(containerUuid).ifPresent(container -> {
            synchronized (storage) {
                container.getChildren().add(result.getUuid());
            }
        });
        scheduleTestCase(result);
    }

    /**
     * Schedule given test case.
     *
     * @param result the test case to schedule.
     */
    public void scheduleTestCase(final TestResult result) {
        notifier.beforeTestSchedule(result);
        result.setStage(Stage.SCHEDULED);
        storage.put(result.getUuid(), result);
        notifier.afterTestSchedule(result);
    }

    /**
     * Starts test case with given uuid. In order to start test case it should be scheduled at first.
     *
     * @param uuid the uuid of test case to start.
     */
    public void startTestCase(final String uuid) {
        threadContext.clear();
        final Optional<TestResult> found = storage.getTestResult(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not start test case: test case with uuid {} is not scheduled", uuid);
            return;
        }
        final TestResult testResult = found.get();

        notifier.beforeTestStart(testResult);
        testResult
                .setStage(Stage.RUNNING)
                .setStart(System.currentTimeMillis());
        threadContext.start(uuid);
        notifier.afterTestStart(testResult);
    }

    /**
     * Shortcut for {@link #updateTestCase(String, Consumer)} for current running test case uuid.
     *
     * @param update the update function.
     */
    public void updateTestCase(final Consumer<TestResult> update) {
        final Optional<String> root = threadContext.getRoot();
        if (!root.isPresent()) {
            LOGGER.error("Could not update test case: no test case running");
            return;
        }

        final String uuid = root.get();
        updateTestCase(uuid, update);
    }

    /**
     * Updates test case by given uuid.
     *
     * @param uuid   the uuid of test case to update.
     * @param update the update function.
     */
    public void updateTestCase(final String uuid, final Consumer<TestResult> update) {
        final Optional<TestResult> found = storage.getTestResult(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not update test case: test case with uuid {} not found", uuid);
            return;
        }
        final TestResult testResult = found.get();

        notifier.beforeTestUpdate(testResult);
        update.accept(testResult);
        notifier.afterTestUpdate(testResult);
    }

    /**
     * Stops test case by given uuid. Test case marked as {@link Stage#FINISHED} and also
     * stop timestamp is calculated. Result would be stored in memory until
     * {@link #writeTestCase(String)} method is called. Also stopped test case could be
     * updated by {@link #updateTestCase(String, Consumer)} method.
     *
     * @param uuid the uuid of test case to stop.
     */
    public void stopTestCase(final String uuid) {
        final Optional<TestResult> found = storage.getTestResult(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not stop test case: test case with uuid {} not found", uuid);
            return;
        }
        final TestResult testResult = found.get();

        notifier.beforeTestStop(testResult);
        testResult
                .setStage(Stage.FINISHED)
                .setStop(System.currentTimeMillis());
        threadContext.clear();
        notifier.afterTestStop(testResult);
    }

    /**
     * Writes test case with given uuid using configured {@link AllureResultsWriter}.
     *
     * @param uuid the uuid of test case to write.
     */
    public void writeTestCase(final String uuid) {
        final Optional<TestResult> found = storage.getTestResult(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not write test case: test case with uuid {} not found", uuid);
            return;
        }

        final TestResult testResult = found.get();
        notifier.beforeTestWrite(testResult);
        writer.write(testResult);
        storage.remove(uuid);
        notifier.afterTestWrite(testResult);
    }

    /**
     * Start a new step as child step of current running test case or step. Shortcut
     * for {@link #startStep(String, String, StepResult)}.
     *
     * @param uuid   the uuid of step.
     * @param result the step.
     */
    public void startStep(final String uuid, final StepResult result) {
        final Optional<String> current = threadContext.getCurrent();
        if (!current.isPresent()) {
            LOGGER.error("Could not start step: no test case running");
            return;
        }
        final String parentUuid = current.get();
        startStep(parentUuid, uuid, result);
    }

    /**
     * Start a new step as child of specified parent.
     *
     * @param parentUuid the uuid of parent test case or step.
     * @param uuid       the uuid of step.
     * @param result     the step.
     */
    public void startStep(final String parentUuid, final String uuid, final StepResult result) {
        notifier.beforeStepStart(result);

        result.setStage(Stage.RUNNING);
        result.setStart(System.currentTimeMillis());

        threadContext.start(uuid);

        storage.put(uuid, result);
        storage.get(parentUuid, WithSteps.class).ifPresent(parentStep -> {
            synchronized (storage) {
                parentStep.getSteps().add(result);
            }
        });

        notifier.afterStepStart(result);
    }

    /**
     * Updates current step. Shortcut for {@link #updateStep(String, Consumer)}.
     *
     * @param update the update function.
     */
    public void updateStep(final Consumer<StepResult> update) {
        final Optional<String> current = threadContext.getCurrent();
        if (!current.isPresent()) {
            LOGGER.error("Could not update step: no step running");
            return;
        }
        final String uuid = current.get();
        updateStep(uuid, update);
    }

    /**
     * Updates step by specified uuid.
     *
     * @param uuid   the uuid of step.
     * @param update the update function.
     */
    public void updateStep(final String uuid, final Consumer<StepResult> update) {
        final Optional<StepResult> found = storage.getStep(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not update step: step with uuid {} not found", uuid);
            return;
        }

        final StepResult step = found.get();

        notifier.beforeStepUpdate(step);
        update.accept(step);
        notifier.afterStepUpdate(step);
    }

    /**
     * Stops current running step. Shortcut for {@link #stopStep(String)}.
     */
    public void stopStep() {
        final String root = threadContext.getRoot().orElse(null);
        final Optional<String> current = threadContext.getCurrent()
                .filter(uuid -> !Objects.equals(uuid, root));
        if (!current.isPresent()) {
            LOGGER.error("Could not stop step: no step running");
            return;
        }
        final String uuid = current.get();
        stopStep(uuid);
    }

    /**
     * Stops step by given uuid.
     *
     * @param uuid the uuid of step to stop.
     */
    public void stopStep(final String uuid) {
        final Optional<StepResult> found = storage.getStep(uuid);
        if (!found.isPresent()) {
            LOGGER.error("Could not stop step: step with uuid {} not found", uuid);
            return;
        }

        final StepResult step = found.get();
        notifier.beforeStepStop(step);

        step.setStage(Stage.FINISHED);
        step.setStop(System.currentTimeMillis());

        storage.remove(uuid);
        threadContext.stop();

        notifier.afterStepStop(step);
    }

    /**
     * Adds attachment into current test or step if any exists. Shortcut
     * for {@link #addAttachment(String, String, String, InputStream)}
     *
     * @param name          the name of attachment
     * @param type          the content type of attachment
     * @param fileExtension the attachment file extension
     * @param body          attachment content
     */
    public void addAttachment(final String name, final String type,
                              final String fileExtension, final byte[] body) {
        addAttachment(name, type, fileExtension, new ByteArrayInputStream(body));
    }

    /**
     * Adds attachment to current running test or step.
     *
     * @param name          the name of attachment
     * @param type          the content type of attachment
     * @param fileExtension the attachment file extension
     * @param stream        attachment content
     */
    public void addAttachment(final String name, final String type,
                              final String fileExtension, final InputStream stream) {
        writeAttachment(prepareAttachment(name, type, fileExtension), stream);
    }

    /**
     * Adds attachment to current running test or step, and returns source. In order
     * to store attachment content use {@link #writeAttachment(String, InputStream)} method.
     *
     * @param name          the name of attachment
     * @param type          the content type of attachment
     * @param fileExtension the attachment file extension
     * @return the source of added attachment
     */
    @SuppressWarnings({"PMD.NullAssignment", "PMD.UseObjectForClearerAPI"})
    public String prepareAttachment(final String name, final String type, final String fileExtension) {
        final String extension = Optional.ofNullable(fileExtension)
                .filter(ext -> !ext.isEmpty())
                .map(ext -> ext.charAt(0) == '.' ? ext : "." + ext)
                .orElse("");
        final String source = UUID.randomUUID().toString() + ATTACHMENT_FILE_SUFFIX + extension;

        final Optional<String> current = threadContext.getCurrent();
        if (!current.isPresent()) {
            LOGGER.error("Could not add attachment: no test is running");
            //backward compatibility: return source even if no attachment is going to be written.
            return source;
        }
        final Attachment attachment = new Attachment()
                .setName(isEmpty(name) ? null : name)
                .setType(isEmpty(type) ? null : type)
                .setSource(source);

        final String uuid = current.get();
        storage.get(uuid, WithAttachments.class).ifPresent(withAttachments -> {
            synchronized (storage) {
                withAttachments.getAttachments().add(attachment);
            }
        });
        return attachment.getSource();
    }

    /**
     * Writes attachment with specified source.
     *
     * @param attachmentSource the source of attachment.
     * @param stream           the attachment content.
     */
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

    private static LifecycleNotifier getDefaultNotifier() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new LifecycleNotifier(
                load(ContainerLifecycleListener.class, classLoader),
                load(TestLifecycleListener.class, classLoader),
                load(FixtureLifecycleListener.class, classLoader),
                load(StepLifecycleListener.class, classLoader)
        );
    }
}
