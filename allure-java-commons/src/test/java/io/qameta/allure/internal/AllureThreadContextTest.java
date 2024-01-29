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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureThreadContextTest {

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
        final String first = UUID.randomUUID().toString();
        final String second = UUID.randomUUID().toString();

        context.start(first);
        context.start(second);

        assertThat(context.getRoot())
                .hasValue(first);

        assertThat(context.getCurrent())
                .hasValue(second);
    }

    @Test
    void shouldClear() {
        final AllureThreadContext context = new AllureThreadContext();
        final String first = UUID.randomUUID().toString();
        final String second = UUID.randomUUID().toString();

        context.start(first);
        context.start(second);

        context.clear();

        assertThat(context.getRoot())
                .isEmpty();
    }

    @Test
    void shouldStop() {
        final AllureThreadContext context = new AllureThreadContext();
        final String first = UUID.randomUUID().toString();
        final String second = UUID.randomUUID().toString();
        final String third = UUID.randomUUID().toString();

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
    void shouldBeThreadSafe() throws ExecutionException, InterruptedException {
        final AllureThreadContext context = new AllureThreadContext();

        final int threads = 1000;
        final int stepsCount = 200;
        final ExecutorService service = Executors.newFixedThreadPool(threads);

        final List<Callable<Optional<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                for (int j = 0; j < stepsCount; j++) {
                    context.start(UUID.randomUUID().toString());
                    context.stop();
                }
                return context.getCurrent();
            });
        }

        final String base = "ROOT";

        context.start(base);
        final List<Future<Optional<String>>> futures = service.invokeAll(tasks);
        for (Future<Optional<String>> future : futures) {
            final Optional<String> value = future.get();

            assertThat(value)
                    .hasValue(base);

        }
    }
}
