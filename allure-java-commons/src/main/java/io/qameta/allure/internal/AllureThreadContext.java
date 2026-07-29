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
import io.qameta.allure.AllureThreadBinding;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-local binding for the current Allure execution context.
 */
public class AllureThreadContext {

    private static final String EXECUTION_CONTEXT = "execution context";

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
        context.get().push(Objects.requireNonNull(executionContext, EXECUTION_CONTEXT));
    }

    /**
     * Adds execution context binding to the current thread and returns a handle for removing that exact binding.
     *
     * @param executionContext the context to bind
     * @return a binding that removes this exact context when closed, including when another thread closes it
     */
    public AllureThreadBinding pushBinding(final AllureExecutionContext executionContext) {
        final AllureExecutionContext binding = Objects.requireNonNull(executionContext, EXECUTION_CONTEXT);
        final ContextStack stack = context.get();
        stack.push(binding);
        return new ThreadBinding(stack, binding);
    }

    /**
     * Removes latest execution context binding. Ignores base context.
     *
     * @return removed execution context.
     */
    public Optional<AllureExecutionContext> pop() {
        return context.get().pop();
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
    private static final class Context extends InheritableThreadLocal<ContextStack> {

        @Override
        public ContextStack initialValue() {
            return singletonStack(new AllureExecutionContext());
        }

        @Override
        protected ContextStack childValue(final ContextStack parentStack) {
            return singletonStack(parentStack.getFirst().child());
        }

    }

    /**
     * Removes the exact pushed context from its origin thread's stack once.
     */
    private static final class ThreadBinding implements AllureThreadBinding {

        private final ContextStack stack;
        private final AllureExecutionContext binding;
        private final AtomicBoolean closed = new AtomicBoolean();

        private ThreadBinding(final ContextStack stack, final AllureExecutionContext binding) {
            this.stack = stack;
            this.binding = binding;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                stack.remove(binding);
            }
        }

    }

    /**
     * A lock-protected context stack that can be restored by a worker while its owner thread is waiting.
     */
    private static final class ContextStack {

        private final Deque<AllureExecutionContext> stack = new LinkedList<>();
        private final ReentrantLock lock = new ReentrantLock();

        private ContextStack(final AllureExecutionContext executionContext) {
            stack.push(executionContext);
        }

        private AllureExecutionContext getFirst() {
            lock.lock();
            try {
                return stack.getFirst();
            } finally {
                lock.unlock();
            }
        }

        private void push(final AllureExecutionContext executionContext) {
            lock.lock();
            try {
                stack.push(executionContext);
            } finally {
                lock.unlock();
            }
        }

        private Optional<AllureExecutionContext> pop() {
            lock.lock();
            try {
                return stack.size() <= 1
                        ? Optional.empty()
                        : Optional.of(stack.pop());
            } finally {
                lock.unlock();
            }
        }

        private void remove(final AllureExecutionContext executionContext) {
            lock.lock();
            try {
                stack.removeFirstOccurrence(executionContext);
            } finally {
                lock.unlock();
            }
        }

    }

    private static ContextStack singletonStack(final AllureExecutionContext executionContext) {
        return new ContextStack(executionContext);
    }
}
