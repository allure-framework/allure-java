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
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ExceptionUtils;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * Provides utility methods for Allure Java test support support.
 *
 * <p>The methods are stateless helpers intended for integrations, tests, and extension code that need the same conventions as the built-in Allure adapters.</p>
 */
public final class RunUtils {

    private RunUtils() {
        throw new IllegalStateException("do not instance");
    }

    /**
     * Runs the supplied tests and returns collected Allure results.
     *
     * @param runnable    the runnable
     * @param configurers the configurers exposing the stub lifecycle to the integration under test — for
     *                    integrations that do not resolve {@code Allure.getLifecycle()} at call time; each is
     *                    called with the stub before the run and with the previous lifecycle after it
     * @return the collected Allure results
     */
    @SafeVarargs
    public static AllureResults runTests(
                                         final Allure.ThrowableContextRunnableVoid<AllureLifecycle> runnable,
                                         final Consumer<AllureLifecycle>... configurers) {
        return runTests(AllureLifecycle::new, runnable, configurers);
    }

    /**
     * Runs the supplied tests and returns collected Allure results.
     *
     * @param lifecycleFactory the lifecycle factory
     * @param runnable         the runnable
     * @param configurers      the configurers exposing the stub lifecycle to the integration under test — for
     *                         integrations that do not resolve {@code Allure.getLifecycle()} at call time; each is
     *                         called with the stub before the run and with the previous lifecycle after it
     * @return the collected Allure results
     */
    @SafeVarargs
    public static AllureResults runTests(
                                         final Function<AllureResultsWriter, AllureLifecycle> lifecycleFactory,
                                         final Allure.ThrowableContextRunnableVoid<AllureLifecycle> runnable,
                                         final Consumer<AllureLifecycle>... configurers) {
        return Allure.step("Run isolated Allure lifecycle", () -> {
            final AllureResultsWriterStub writer = new AllureResultsWriterStub();
            final AllureLifecycle lifecycle = lifecycleFactory.apply(writer);

            // swaps the process-wide lifecycle so the facade runs exactly as in production; callers must
            // carry @IsolatedLifecycle so the platform never schedules two such runs concurrently
            final AllureLifecycle previous = Allure.getLifecycle();
            Allure.setLifecycle(lifecycle);
            Stream.of(configurers).forEach(configurer -> configurer.accept(lifecycle));
            try {
                runnable.run(lifecycle);
                return writer;
            } catch (Throwable e) {
                throw ExceptionUtils.sneakyThrow(e);
            } finally {
                // restore in reverse: integration wiring first, then the process-wide lifecycle, so no
                // stub reference survives the run
                Stream.of(configurers).forEach(configurer -> configurer.accept(previous));
                Allure.setLifecycle(previous);
                AllureTestCommonsUtils.attach(writer);
            }
        });
    }

    /**
     * Runs the callback inside an Allure test context and returns collected results.
     *
     * @param runnable    the runnable
     * @param configurers the configurers exposing the stub lifecycle to the integration under test
     * @return the collected Allure results
     */
    @SafeVarargs
    public static AllureResults runWithinTestContext(
                                                     final Runnable runnable,
                                                     final Consumer<AllureLifecycle>... configurers) {
        return runTests(lifecycle -> withTestContext(runnable, lifecycle), configurers);
    }

    /**
     * Runs the callback inside an Allure test context and returns collected results.
     *
     * @param lifecycleFactory the lifecycle factory
     * @param runnable         the runnable
     * @param configurers      the configurers exposing the stub lifecycle to the integration under test
     * @return the collected Allure results
     */
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
        final AllureExternalKey testKey = AllureExternalKey.random(RunUtils.class);

        try {
            lifecycle.scheduleTest(testKey, result);
            lifecycle.startTest(testKey);

            runnable.run();
        } catch (Throwable e) {
            lifecycle.updateTest(testKey, testResult -> {
                getStatus(e).ifPresent(testResult::setStatus);
                getStatusDetails(e).ifPresent(testResult::setStatusDetails);

            });
        } finally {
            lifecycle.stopTest(testKey);
            lifecycle.writeTest(testKey);
        }
    }

}
