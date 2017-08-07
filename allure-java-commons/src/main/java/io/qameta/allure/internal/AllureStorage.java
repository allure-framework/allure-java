package io.qameta.allure.internal;

import io.qameta.allure.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal Allure data storage.
 *
 * @since 2.0
 */
public class AllureStorage {

    private final Map<String, Object> storage = new ConcurrentHashMap<>();

    private final Map<Thread, Deque<String>> currentStepContext = new ConcurrentHashMap<>();

    @SuppressWarnings("PMD.NullAssignment")
    public Optional<String> getCurrentStep() {
        final Deque<String> uids = getStepContext();
        return uids.isEmpty()
                ? Optional.empty()
                : Optional.of(uids.getFirst());
    }

    public void linkContextWith(final Thread thread) {
        if (thread != null && !getCurrentStep().isPresent()) {
            currentStepContext.put(Thread.currentThread(), currentStepContext.get(thread));
        }
    }

    public Optional<String> getStepForThread(final Thread thread) {
        final Deque<String> uids = getStepContext(thread);
        return uids.isEmpty()
                ? Optional.empty()
                : Optional.of(uids.getFirst());
    }

    @SuppressWarnings("PMD.NullAssignment")
    public String getRootStep() {
        final Deque<String> uids = getStepContext();
        return uids.isEmpty()
                ? null
                : uids.getLast();
    }

    private Deque<String> getStepContext() {
        final Thread currentThread = Thread.currentThread();
        Deque<String> list = currentStepContext.get(currentThread);
        if (list == null) {
            list = new LinkedList<>();
            currentStepContext.put(currentThread, list);
        }

        return list;
    }

    private Deque<String> getStepContext(final Thread thread) {
        Deque<String> list = currentStepContext.get(thread);
        if (list == null) {
            list = new LinkedList<>();
            currentStepContext.put(thread, list);
        }

        return list;
    }

    private void removeStepContext() {
        final Deque<String> list = getStepContext();
        if (!list.isEmpty()) {
            list.remove();
        }
    }

    public void startStep(final String uuid) {
        getStepContext().push(uuid);
    }

    public void stopStep() {
        final Deque<String> list = getStepContext();
        if (!list.isEmpty()) {
            list.pop();
        }
    }

    public void clearStepContext() {
        removeStepContext();
    }

    public Optional<TestResultContainer> getContainer(final String uuid) {
        return get(uuid, TestResultContainer.class);
    }

    public void addContainer(final TestResultContainer container) {
        put(container.getUuid(), container);
    }

    public Optional<TestResultContainer> removeContainer(final String uuid) {
        return remove(uuid, TestResultContainer.class);
    }

    public void addTestResult(final TestResult testResult) {
        put(testResult.getUuid(), testResult);
    }

    public Optional<TestResult> getTestResult(final String uuid) {
        return get(uuid, TestResult.class);
    }

    public Optional<TestResult> removeTestResult(final String uuid) {
        return remove(uuid, TestResult.class);
    }

    public Optional<FixtureResult> getFixture(final String uuid) {
        return get(uuid, FixtureResult.class);
    }

    public void addFixture(final String uuid, final FixtureResult fixtureResult) {
        put(uuid, fixtureResult);
    }

    public Optional<FixtureResult> removeFixture(final String uuid) {
        return remove(uuid, FixtureResult.class);
    }

    public Optional<StepResult> getStep(final String uuid) {
        return get(uuid, StepResult.class);
    }

    public void addStep(final String parentUuid, final String uuid, final StepResult step) {
        put(uuid, step);
        get(parentUuid, WithSteps.class).ifPresent(parentStep -> parentStep.getSteps().add(step));
    }

    public Optional<StepResult> removeStep(final String uuid) {
        return remove(uuid, StepResult.class);
    }

    public <T> T put(final String uuid, final T item) {
        Objects.requireNonNull(uuid, "Can't put item to storage: uuid can't be null");
        storage.put(uuid, item);
        return item;
    }

    public <T> Optional<T> get(final String uuid, final Class<T> clazz) {
        Objects.requireNonNull(uuid, "Can't get item from storage: uuid can't be null");
        return Optional.ofNullable(storage.get(uuid))
                .map(item -> cast(item, clazz));
    }

    public <T> Optional<T> remove(final String uuid, final Class<T> clazz) {
        Objects.requireNonNull(uuid, "Can't remove item from storage: uuid can't be null");
        return Optional.ofNullable(storage.remove(uuid))
                .map(item -> cast(item, clazz));
    }

    public <T> T cast(final Object obj, final Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        throw new IllegalStateException("Can not cast " + obj + " to " + clazz);
    }
}
