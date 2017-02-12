package io.qameta.allure;

import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestAfterResult;
import io.qameta.allure.model.TestBeforeResult;
import io.qameta.allure.model.TestCaseResult;
import io.qameta.allure.model.TestGroupResult;
import io.qameta.allure.model.TestStepResult;
import io.qameta.allure.model.WithAfter;
import io.qameta.allure.model.WithAttachments;
import io.qameta.allure.model.WithBefore;
import io.qameta.allure.model.WithSteps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static io.qameta.allure.AllureUtils.write;
import static io.qameta.allure.AllureUtils.writeAttachment;

/**
 * @author charlie (Dmitry Baev).
 */
public class Allure {

    private static final Logger LOGGER = LoggerFactory.getLogger(Allure.class);

    private Path resultsDirectory;

    private final Map<String, Object> storage = new HashMap<>();

    private final ThreadLocal<LinkedList<String>> currentStepContext =
            InheritableThreadLocal.withInitial(LinkedList::new);

    public static final Allure LIFECYCLE = new Allure();

    public Allure() {
        resultsDirectory = Paths.get(System.getProperty("allure.results.directory", "allure-results"));
    }

    public void startTestGroup(String uuid, TestGroupResult result) {
        LOGGER.info("Start test group {}", uuid);
        put(uuid, result).withStart(System.currentTimeMillis());
    }

    public void updateTestGroup(String uuid, Consumer<TestGroupResult> update) {
        LOGGER.info("Update test group {}", uuid);
        update.accept(get(uuid, TestGroupResult.class));
    }

    public void stopTestGroup(String uuid) {
        LOGGER.info("Stop test group {}", uuid);
        TestGroupResult result = remove(uuid, TestGroupResult.class)
                .withStop(System.currentTimeMillis());
        write(result, resultsDirectory);
    }

    public void startTestBefore(String parentUuid, String uuid, TestBeforeResult result) {
        LOGGER.info("Start test before {} with parent {}", uuid, parentUuid);
        put(uuid, result).withStart(System.currentTimeMillis());
        get(parentUuid, WithBefore.class).getBefores().add(result);
        currentStepContext.remove();
        currentStepContext.get().push(uuid);
    }

    public void stopTestBefore() {
        stopTestBefore(currentStepContext.get().getLast());
    }

    public void stopTestBefore(String uuid) {
        LOGGER.info("Stop test before {}", uuid);
        currentStepContext.remove();
        remove(uuid, TestBeforeResult.class).withStop(System.currentTimeMillis());
    }

    public void startTestAfter(String parentUuid, String uuid, TestAfterResult result) {
        LOGGER.info("Start test after {} with parent {}", uuid, parentUuid);
        put(uuid, result).withStart(System.currentTimeMillis());
        get(parentUuid, WithAfter.class).getAfters().add(result);
        currentStepContext.remove();
        currentStepContext.get().push(uuid);
    }

    public void stopTestAfter(String uuid) {
        LOGGER.info("Stop test after {}", uuid);
        currentStepContext.remove();
        remove(uuid, TestAfterResult.class).withStop(System.currentTimeMillis());
    }

    public void scheduleTestCase(String uuid, TestCaseResult result) {
        LOGGER.info("Schedule test case {}", uuid);
        put(uuid, result);
    }

    public void cancelTestCase(String uuid, StatusDetails details) {
        LOGGER.info("Cancel test case {}", uuid);
        long currentTimeMillis = System.currentTimeMillis();
        get(uuid, TestCaseResult.class)
                .withStatus(Status.CANCELED)
                .withStatusDetails(details)
                .withStart(currentTimeMillis)
                .withStop(currentTimeMillis);
    }

    public void startTestCase(String uuid) {
        LOGGER.info("Start test case {}", uuid);
        get(uuid, TestCaseResult.class).setStart(System.currentTimeMillis());
        currentStepContext.remove();
        currentStepContext.get().push(uuid);
    }

    public void updateTestCase(Consumer<TestCaseResult> update) {
        updateTestCase(currentStepContext.get().getLast(), update);
    }

    public void updateTestCase(String uuid, Consumer<TestCaseResult> update) {
        LOGGER.info("Update test case {}", uuid);
        update.accept(get(uuid, TestCaseResult.class));
    }

    public void stopTestCase() {
        stopTestCase(currentStepContext.get().getLast());
    }

    public void stopTestCase(String uuid) {
        LOGGER.info("Stop test case {}", uuid);
        currentStepContext.remove();
        get(uuid, TestCaseResult.class).setStop(System.currentTimeMillis());
    }

    public void closeTestCase(String uuid) {
        LOGGER.info("Close test case {}", uuid);
        write(remove(uuid, TestCaseResult.class), resultsDirectory);
    }

    public void addAttachment(byte[] content, String name, String type) {
        String uuid = currentStepContext.get().getLast();
        LOGGER.info("Adding attachment for {}", uuid);
        Attachment attachment = writeAttachment(content, name, type, resultsDirectory);
        get(uuid, WithAttachments.class).getAttachments().add(attachment);
    }

    public void startStep(String uuid, TestStepResult result) {
        LinkedList<String> uuids = currentStepContext.get();
        startStep(uuids.isEmpty() ? null : uuids.getFirst(), uuid, result);
    }

    public void startStep(String parentUuid, String uuid, TestStepResult result) {
        LOGGER.info("Start step {} with parent {}", uuid, parentUuid);
        put(uuid, result).withStart(System.currentTimeMillis());
        currentStepContext.get().push(uuid);

        if (Objects.nonNull(parentUuid)) {
            get(parentUuid, WithSteps.class).getSteps().add(result);
        }
    }

    public void updateStep(Consumer<TestStepResult> update) {
        updateStep(currentStepContext.get().getFirst(), update);
    }

    public void updateStep(String uuid, Consumer<TestStepResult> update) {
        LOGGER.info("Update step {}", uuid);
        update.accept(get(uuid, TestStepResult.class));
    }

    public void stopStep() {
        stopStep(currentStepContext.get().getFirst());
    }

    public void stopStep(String uuid) {
        LOGGER.info("Stop step {}", uuid);
        remove(uuid, TestStepResult.class).withStop(System.currentTimeMillis());
        currentStepContext.get().pop();
    }

    public Path getResultsDirectory() {
        return resultsDirectory;
    }

    public void setResultsDirectory(Path resultsDirectory) {
        this.resultsDirectory = resultsDirectory;
    }

    private <T> T put(String uuid, T item) {
        Objects.requireNonNull(uuid, "Uuid can't be null");
        storage.put(uuid, item);
        return item;
    }

    private <T> T get(String uuid, Class<T> clazz) {
        Objects.requireNonNull(uuid, "Uuid can't be null");
        T result = cast(storage.computeIfAbsent(uuid, u -> new TestCaseResult().withName("Unknown")), clazz);
        return Objects.requireNonNull(result, "Could not find " + clazz + " by uuid " + uuid);
    }

    private <T> T remove(String uuid, Class<T> clazz) {
        Objects.requireNonNull(uuid, "Uuid can't be null");
        T result = cast(storage.remove(uuid), clazz);
        return Objects.requireNonNull(result, "Could not find " + clazz + " by uuid " + uuid);
    }

    private <T> T cast(Object obj, Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        throw new IllegalStateException("Can not cast " + obj + " to " + clazz);
    }
}
