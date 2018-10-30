package io.qameta.allure.internal;

import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Internal Allure data storage.
 *
 * @since 2.0
 */
public class AllureStorage {

    private final Map<String, Object> storage = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Optional<TestResultContainer> getContainer(final String uuid) {
        return get(uuid, TestResultContainer.class);
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

    public Optional<FixtureResult> removeFixture(final String uuid) {
        return remove(uuid, FixtureResult.class);
    }

    public Optional<StepResult> getStep(final String uuid) {
        return get(uuid, StepResult.class);
    }

    public Optional<StepResult> removeStep(final String uuid) {
        return remove(uuid, StepResult.class);
    }

    public <T> Optional<T> get(final String uuid, final Class<T> clazz) {
        lock.readLock().lock();
        try {
            Objects.requireNonNull(uuid, "Can't get item from storage: uuid can't be null");
            return Optional.ofNullable(storage.get(uuid))
                    .filter(clazz::isInstance)
                    .map(clazz::cast);
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T put(final String uuid, final T item) {
        lock.writeLock().lock();
        try {
            Objects.requireNonNull(uuid, "Can't put item to storage: uuid can't be null");
            storage.put(uuid, item);
            return item;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <T> Optional<T> remove(final String uuid, final Class<T> clazz) {
        lock.writeLock().lock();
        try {
            Objects.requireNonNull(uuid, "Can't remove item from storage: uuid can't be null");
            return Optional.ofNullable(storage.remove(uuid))
                    .filter(clazz::isInstance)
                    .map(clazz::cast);
        } finally {
            lock.writeLock().unlock();
        }
    }

}
