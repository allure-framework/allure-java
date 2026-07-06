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
package io.qameta.allure.internal;

import io.qameta.allure.AllureExternalKey;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable execution context that stores a stack of running Allure executable keys.
 */
public final class AllureExecutionContext {

    private final AllureExecutionContext parent;

    private final AllureExternalKey currentOnFork;

    private final Deque<AllureExternalKey> localKeys;

    /**
     * Creates an empty execution context.
     */
    public AllureExecutionContext() {
        this(null, null, new LinkedList<>());
    }

    private AllureExecutionContext(final AllureExecutionContext parent, final AllureExternalKey currentOnFork,
                                   final Deque<AllureExternalKey> localKeys) {
        this.parent = parent;
        this.currentOnFork = currentOnFork;
        this.localKeys = localKeys;
    }

    /**
     * Returns last (most recent) key.
     */
    public Optional<AllureExternalKey> getCurrent() {
        return localKeys.isEmpty()
                ? Optional.empty()
                : Optional.of(localKeys.getFirst());
    }

    /**
     * Returns first (oldest) key.
     */
    public Optional<AllureExternalKey> getRoot() {
        if (Objects.nonNull(parent)) {
            return parent.getRoot();
        }
        return localKeys.isEmpty()
                ? Optional.empty()
                : Optional.of(localKeys.getLast());
    }

    /**
     * Returns the executable key new executable items should be attached to.
     */
    public Optional<AllureExternalKey> getCurrentExecutable() {
        final Optional<AllureExternalKey> current = getCurrent();
        if (current.isPresent()) {
            return current;
        }
        if (Objects.nonNull(currentOnFork)) {
            return Optional.of(currentOnFork);
        }
        return getRoot();
    }

    /**
     * Adds new key.
     */
    public void start(final AllureExternalKey key) {
        Objects.requireNonNull(key, "executable key");
        localKeys.push(key);
    }

    /**
     * Returns a snapshot of the locally bound keys, most recent first.
     *
     * @return local keys, top of the stack first
     */
    public List<AllureExternalKey> getLocalKeys() {
        return new ArrayList<>(localKeys);
    }

    /**
     * Returns a copy of this execution context.
     *
     * @return context snapshot
     */
    public AllureExecutionContext copy() {
        return new AllureExecutionContext(
                Objects.isNull(parent) ? null : parent.copy(),
                currentOnFork,
                new LinkedList<>(localKeys)
        );
    }

    /**
     * Returns a child execution context linked to this context.
     *
     * @return child context
     */
    public AllureExecutionContext child() {
        if (getRoot().isEmpty()) {
            return new AllureExecutionContext();
        }
        return new AllureExecutionContext(this, getCurrentExecutable().orElse(null), new LinkedList<>());
    }

    /**
     * Removes latest locally added key. Ignores empty local context.
     *
     * @return removed key.
     */
    public Optional<AllureExternalKey> stop() {
        if (!localKeys.isEmpty()) {
            return Optional.of(localKeys.pop());
        }
        return Optional.empty();
    }
}
