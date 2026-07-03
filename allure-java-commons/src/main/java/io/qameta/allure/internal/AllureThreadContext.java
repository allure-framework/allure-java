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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-local binding for the current Allure execution context.
 */
public class AllureThreadContext {

    private final Context context = new Context();

    /**
     * Returns last (most recent) key.
     */
    public Optional<AllureExternalKey> getCurrent() {
        return get().getCurrent();
    }

    /**
     * Returns first (oldest) key.
     */
    public Optional<AllureExternalKey> getRoot() {
        return get().getRoot();
    }

    /**
     * Returns the current step key, that is the current key unless it is the root test or fixture. Empty when no
     * step is running above the root.
     */
    public Optional<AllureExternalKey> getCurrentStep() {
        final AllureExternalKey root = getRoot().orElse(null);
        return getCurrent().filter(key -> !Objects.equals(key, root));
    }

    /**
     * Returns the executable key new executable items should be attached to.
     */
    public Optional<AllureExternalKey> getCurrentExecutable() {
        return get().getCurrentExecutable();
    }

    /**
     * Returns a snapshot of the current binding's locally bound keys, most recent first.
     *
     * @return local keys, top of the stack first
     */
    public List<AllureExternalKey> getLocalKeys() {
        return get().getLocalKeys();
    }

    /**
     * Adds new key.
     */
    public void start(final AllureExternalKey key) {
        get().start(key);
    }

    /**
     * Returns a copy of the current thread context.
     *
     * @return context snapshot
     */
    public AllureExecutionContext copy() {
        return get().copy();
    }

    /**
     * Returns the current execution context.
     *
     * @return current execution context
     */
    public AllureExecutionContext get() {
        return context.get().getFirst();
    }

    /**
     * Restores the current thread context binding from a snapshot.
     *
     * @param executionContext the context snapshot
     */
    public void set(final AllureExecutionContext executionContext) {
        context.set(singletonStack(executionContext.copy()));
    }

    /**
     * Adds execution context binding to the current thread.
     *
     * @param executionContext the context to bind
     */
    public void push(final AllureExecutionContext executionContext) {
        context.get().push(Objects.requireNonNull(executionContext, "execution context"));
    }

    /**
     * Removes latest execution context binding. Ignores base context.
     *
     * @return removed execution context.
     */
    public Optional<AllureExecutionContext> pop() {
        final Deque<AllureExecutionContext> stack = context.get();
        if (stack.size() <= 1) {
            return Optional.empty();
        }
        return Optional.of(stack.pop());
    }

    /**
     * Removes latest added key. Ignores empty context.
     *
     * @return removed key.
     */
    public Optional<AllureExternalKey> stop() {
        return get().stop();
    }

    /**
     * Removes all the data stored for current thread.
     */
    public void clear() {
        context.remove();
    }

    /**
     * Thread local binding for the current execution context.
     */
    private static final class Context extends InheritableThreadLocal<Deque<AllureExecutionContext>> {

        @Override
        public Deque<AllureExecutionContext> initialValue() {
            return singletonStack(new AllureExecutionContext());
        }

        @Override
        protected Deque<AllureExecutionContext> childValue(final Deque<AllureExecutionContext> parentStack) {
            return singletonStack(parentStack.getFirst().child());
        }

    }

    private static Deque<AllureExecutionContext> singletonStack(final AllureExecutionContext executionContext) {
        final Deque<AllureExecutionContext> stack = new LinkedList<>();
        stack.push(executionContext);
        return stack;
    }
}
