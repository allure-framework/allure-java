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
package io.qameta.allure.test;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ExceptionUtils;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * @author charlie (Dmitry Baev).
 */
public final class RunUtils {

    private RunUtils() {
        throw new IllegalStateException("do not instance");
    }

    public static AllureResults runTests(
            final Allure.ThrowableContextRunnableVoid<AllureLifecycle> runnable) {
        return runTests(
                runnable,
                Allure::setLifecycle,
                StepsAspects::setLifecycle,
                AttachmentsAspects::setLifecycle
        );
    }

    public static AllureResults runTests(
            final Function<AllureResultsWriter, AllureLifecycle> lifecycleFactory,
            final Allure.ThrowableContextRunnableVoid<AllureLifecycle> runnable) {
        return runTests(
                lifecycleFactory,
                runnable,
                Allure::setLifecycle,
                StepsAspects::setLifecycle,
                AttachmentsAspects::setLifecycle
        );
    }

    @SafeVarargs
    public static AllureResults runTests(
            final Allure.ThrowableContextRunnableVoid<AllureLifecycle> runnable,
            final Consumer<AllureLifecycle>... configurers) {
        return runTests(AllureLifecycle::new, runnable, configurers);
    }

    @SafeVarargs
    public static AllureResults runTests(
            final Function<AllureResultsWriter, AllureLifecycle> lifecycleFactory,
            final Allure.ThrowableContextRunnableVoid<AllureLifecycle> runnable,
            final Consumer<AllureLifecycle>... configurers) {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = lifecycleFactory.apply(writer);

        final AllureLifecycle defaultLifecycle = Allure.getLifecycle();
        try {
            Stream.of(configurers).forEach(configurer -> configurer.accept(lifecycle));

            runnable.run(lifecycle);

            return writer;
        } catch (Throwable e) {
            throw ExceptionUtils.sneakyThrow(e);
        } finally {
            Stream.of(configurers).forEach(configurer -> configurer.accept(defaultLifecycle));

            AllureTestCommonsUtils.attach(writer);
        }
    }

    public static AllureResults runWithinTestContext(
            final Runnable runnable) {
        return runTests(lifecycle -> withTestContext(runnable, lifecycle));
    }

    public static AllureResults runWithinTestContext(
            final Function<AllureResultsWriter, AllureLifecycle> lifecycleFactory,
            final Runnable runnable) {
        return runTests(lifecycleFactory, lifecycle -> withTestContext(runnable, lifecycle));
    }

    @SafeVarargs
    public static AllureResults runWithinTestContext(
            final Runnable runnable,
            final Consumer<AllureLifecycle>... configurers) {
        return runTests(lifecycle -> withTestContext(runnable, lifecycle), configurers);
    }

    @SafeVarargs
    public static AllureResults runWithinTestContext(
            final Function<AllureResultsWriter, AllureLifecycle> lifecycleFactory,
            final Runnable runnable,
            final Consumer<AllureLifecycle>... configurers) {
        return runTests(lifecycleFactory, lifecycle -> withTestContext(runnable, lifecycle), configurers);
    }

    private static void withTestContext(final Runnable runnable, final AllureLifecycle lifecycle) {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        try {
            lifecycle.scheduleTestCase(result);
            lifecycle.startTestCase(uuid);

            runnable.run();
        } catch (Throwable e) {
            lifecycle.updateTestCase(uuid, testResult -> {
                getStatus(e).ifPresent(testResult::setStatus);
                getStatusDetails(e).ifPresent(testResult::setStatusDetails);

            });
        } finally {
            lifecycle.stopTestCase(uuid);
            lifecycle.writeTestCase(uuid);
        }
    }

}
