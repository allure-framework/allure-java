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

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
class AllureThreadContextTest {

    private static AllureExternalKey key() {
        return AllureExternalKey.random(AllureThreadContextTest.class);
    }

    @Test
    void shouldCreateEmptyContext() {
        final AllureThreadContext context = new AllureThreadContext();
        assertThat(context.getRoot())
                .isEmpty();

        assertThat(context.getCurrent())
                .isEmpty();
    }

    @Test
    void shouldStart() {
        final AllureThreadContext context = new AllureThreadContext();
        final AllureExternalKey first = key();
        final AllureExternalKey second = key();

        context.start(first);
        context.start(second);

        assertThat(context.getRoot())
                .hasValue(first);

        assertThat(context.getCurrent())
                .hasValue(second);
    }

    @Test
    void shouldReturnCurrentStepOnlyAboveRoot() {
        final AllureThreadContext context = new AllureThreadContext();
        final AllureExternalKey root = key();
        final AllureExternalKey step = key();

        assertThat(context.getCurrentStep())
                .as("no current step in an empty context")
                .isEmpty();

        context.start(root);
        assertThat(context.getCurrentStep())
                .as("the root test or fixture is not a step")
                .isEmpty();

        context.start(step);
        assertThat(context.getCurrentStep())
                .as("a key started above the root is a step")
                .hasValue(step);
    }

    @Test
    void shouldClear() {
        final AllureThreadContext context = new AllureThreadContext();
        final AllureExternalKey first = key();
        final AllureExternalKey second = key();

        context.start(first);
        context.start(second);

        context.clear();

        assertThat(context.getRoot())
                .isEmpty();
    }

    @Test
    void shouldStop() {
        final AllureThreadContext context = new AllureThreadContext();
        final AllureExternalKey first = key();
        final AllureExternalKey second = key();
        final AllureExternalKey third = key();

        context.start(first);
        context.start(second);
        context.start(third);

        context.stop();

        assertThat(context.getCurrent())
                .hasValue(second);

        context.stop();

        assertThat(context.getCurrent())
                .hasValue(first);

        context.stop();

        assertThat(context.getCurrent())
                .isEmpty();

    }

    @Test
    void shouldIgnoreStopForEmpty() {
        final AllureThreadContext context = new AllureThreadContext();
        context.stop();

        assertThat(context.getRoot())
                .isEmpty();
    }

    @Test
    void shouldRestoreExecutionContextSnapshot() {
        final AllureThreadContext context = new AllureThreadContext();
        final AllureExternalKey first = key();
        final AllureExternalKey second = key();

        context.start(first);
        final AllureExecutionContext snapshot = context.copy();
        context.start(second);

        context.set(snapshot);

        assertThat(context.getRoot())
                .hasValue(first);
        assertThat(context.getCurrent())
                .hasValue(first);
    }

    @Test
    void shouldBindExecutionContextCopy() {
        final AllureThreadContext context = new AllureThreadContext();
        final AllureExecutionContext executionContext = new AllureExecutionContext();
        final AllureExternalKey first = key();
        final AllureExternalKey second = key();
        final AllureExternalKey third = key();

        executionContext.start(first);
        context.set(executionContext);
        executionContext.start(second);
        context.start(third);

        assertThat(context.getRoot())
                .hasValue(first);
        assertThat(context.getCurrent())
                .hasValue(third);
        assertThat(executionContext.getCurrent())
                .hasValue(second);
    }

    @Test
    void shouldPushAndPopExecutionContextBinding() {
        final AllureThreadContext context = new AllureThreadContext();
        final AllureExecutionContext executionContext = new AllureExecutionContext();
        final AllureExternalKey first = key();
        final AllureExternalKey second = key();

        context.start(first);
        executionContext.start(second);

        context.push(executionContext);

        assertThat(context.getRoot())
                .hasValue(second);
        assertThat(context.getCurrent())
                .hasValue(second);

        assertThat(context.pop())
                .hasValueSatisfying(
                        popped -> assertThat(popped)
                                .isSameAs(executionContext)
                );
        assertThat(context.getRoot())
                .hasValue(first);
        assertThat(context.getCurrent())
                .hasValue(first);
        assertThat(context.pop())
                .isEmpty();
        assertThat(context.getRoot())
                .hasValue(first);
    }

    @Test
    void shouldInheritOnlyCurrentExecutionContextBinding() throws ExecutionException, InterruptedException {
        final AllureThreadContext context = new AllureThreadContext();
        final AllureExecutionContext executionContext = new AllureExecutionContext();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        final AllureExternalKey first = key();
        final AllureExternalKey second = key();
        final AllureExternalKey third = key();

        context.start(first);
        executionContext.start(second);
        executionContext.start(third);
        context.push(executionContext);

        final Future<List<Optional<AllureExternalKey>>> future = service.submit(() -> {
            final List<Optional<AllureExternalKey>> values = new ArrayList<>();
            values.add(context.getRoot());
            values.add(context.getCurrent());
            values.add(context.getCurrentExecutable());
            return values;
        });

        try {
            assertThat(future.get())
                    .containsExactly(
                            Optional.of(second),
                            Optional.empty(),
                            Optional.of(third)
                    );
        } finally {
            service.shutdownNow();
        }
    }

    @Test
    void shouldReadUpdatesFromLinkedParentContext() {
        final AllureExecutionContext parent = new AllureExecutionContext();
        final AllureExternalKey testKey = key();
        final AllureExternalKey parentStepKey = key();
        final AllureExternalKey childStepKey = key();

        parent.start(testKey);
        parent.start(parentStepKey);
        final AllureExecutionContext child = parent.child();

        assertThat(child.getRoot())
                .hasValue(testKey);
        assertThat(child.getCurrent())
                .isEmpty();
        assertThat(child.getCurrentExecutable())
                .hasValue(parentStepKey);

        child.start(childStepKey);
        parent.stop();

        assertThat(child.getRoot())
                .hasValue(testKey);
        assertThat(child.getCurrent())
                .hasValue(childStepKey);
        assertThat(child.getCurrentExecutable())
                .hasValue(childStepKey);

        child.stop();

        assertThat(child.getCurrent())
                .isEmpty();
        assertThat(child.getRoot())
                .hasValue(testKey);
        assertThat(child.getCurrentExecutable())
                .hasValue(parentStepKey);
    }

    @Test
    void shouldCreateIndependentChildForEmptyParentContext() {
        final AllureExecutionContext parent = new AllureExecutionContext();
        final AllureExecutionContext child = parent.child();
        final AllureExternalKey childStepKey = key();

        assertThat(child.getCurrentExecutable())
                .isEmpty();

        child.start(childStepKey);

        assertThat(child.getRoot())
                .hasValue(childStepKey);
        assertThat(child.getCurrent())
                .hasValue(childStepKey);
        assertThat(child.getCurrentExecutable())
                .hasValue(childStepKey);
    }

    @Test
    void shouldNotInheritEmptyContextInChildThread() throws ExecutionException, InterruptedException {
        final AllureThreadContext context = new AllureThreadContext();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        final AllureExternalKey testKey = key();

        context.getCurrent();

        final Future<List<Optional<AllureExternalKey>>> future = service.submit(() -> {
            context.start(testKey);
            final List<Optional<AllureExternalKey>> values = new ArrayList<>();
            values.add(context.getRoot());
            values.add(context.getCurrent());
            values.add(context.getCurrentExecutable());
            return values;
        });

        try {
            assertThat(future.get())
                    .containsExactly(
                            Optional.of(testKey),
                            Optional.of(testKey),
                            Optional.of(testKey)
                    );
        } finally {
            service.shutdownNow();
        }
    }

    @Test
    void shouldKeepInheritedContextReadOnlyInChildThread() throws ExecutionException, InterruptedException {
        final AllureThreadContext context = new AllureThreadContext();
        final AllureExternalKey testKey = key();
        final AllureExternalKey parentStepKey = key();
        final AllureExternalKey childStepKey = key();
        final ExecutorService service = Executors.newSingleThreadExecutor();

        context.start(testKey);
        context.start(parentStepKey);

        final Future<List<Optional<AllureExternalKey>>> future = service.submit(() -> {
            final List<Optional<AllureExternalKey>> values = new ArrayList<>();
            values.add(context.getRoot());
            values.add(context.getCurrent());
            values.add(context.getCurrentExecutable());

            context.start(childStepKey);
            values.add(context.getRoot());
            values.add(context.getCurrent());
            values.add(context.getCurrentExecutable());

            values.add(context.stop());
            values.add(context.getCurrent());
            values.add(context.getCurrentExecutable());

            values.add(context.stop());
            values.add(context.getCurrent());
            values.add(context.getCurrentExecutable());
            return values;
        });

        try {
            assertThat(future.get())
                    .containsExactly(
                            Optional.of(testKey),
                            Optional.empty(),
                            Optional.of(parentStepKey),
                            Optional.of(testKey),
                            Optional.of(childStepKey),
                            Optional.of(childStepKey),
                            Optional.of(childStepKey),
                            Optional.empty(),
                            Optional.of(parentStepKey),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of(parentStepKey)
                    );
            assertThat(context.getCurrent())
                    .hasValue(parentStepKey);
        } finally {
            service.shutdownNow();
        }
    }

    @Test
    void shouldBeThreadSafe() throws ExecutionException, InterruptedException {
        final AllureThreadContext context = new AllureThreadContext();

        final int threads = 1000;
        final int stepsCount = 200;
        final ExecutorService service = Executors.newFixedThreadPool(threads);

        final List<Callable<Optional<AllureExternalKey>>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                for (int j = 0; j < stepsCount; j++) {
                    context.start(key());
                    context.stop();
                }
                return context.getRoot();
            });
        }

        final AllureExternalKey base = key();

        context.start(base);
        final List<Future<Optional<AllureExternalKey>>> futures = Allure.step("Run thread context operations concurrently", step -> {
            step.parameter("threads", threads);
            step.parameter("stepsPerThread", stepsCount);
            return service.invokeAll(tasks);
        });
        for (Future<Optional<AllureExternalKey>> future : futures) {
            final Optional<AllureExternalKey> value = future.get();

            assertThat(value)
                    .hasValue(base);

        }
    }
}
