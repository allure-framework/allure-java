/*
 *  Copyright 2016-2024 Qameta Software Inc
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

    public Optional<TestResult> getTestResult(final String uuid) {
        return get(uuid, TestResult.class);
    }

    public Optional<FixtureResult> getFixture(final String uuid) {
        return get(uuid, FixtureResult.class);
    }

    public Optional<StepResult> getStep(final String uuid) {
        return get(uuid, StepResult.class);
    }

    public <T> Optional<T> get(final String uuid, final Class<T> clazz) {
        Objects.requireNonNull(uuid, "Can't get item from storage: uuid can't be null");
        lock.readLock().lock();
        try {
            return Optional.ofNullable(storage.get(uuid))
                    .filter(clazz::isInstance)
                    .map(clazz::cast);
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T put(final String uuid, final T item) {
        Objects.requireNonNull(uuid, "Can't put item to storage: uuid can't be null");
        lock.writeLock().lock();
        try {
            storage.put(uuid, item);
            return item;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(final String uuid) {
        Objects.requireNonNull(uuid, "Can't remove item from storage: uuid can't be null");
        lock.writeLock().lock();
        try {
            storage.remove(uuid);
        } finally {
            lock.writeLock().unlock();
        }
    }

}
