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
package io.qameta.allure.kotlin.coroutines

import io.qameta.allure.Allure
import io.qameta.allure.Description
import io.qameta.allure.kotlin.extensions.StepScope
import io.qameta.allure.model.Parameter
import io.qameta.allure.model.Stage
import io.qameta.allure.model.Status
import io.qameta.allure.test.AllureResults
import io.qameta.allure.test.IsolatedLifecycle
import io.qameta.allure.test.RunUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors

@IsolatedLifecycle
@Timeout(15)
internal class AllureCoroutinesTest {
    @Test
    @Description(
        "A suspend step stays current after a dispatcher switch, supports receiver updates and attachments, " +
            "and restores the worker thread afterwards.",
    )
    fun shouldKeepStepCurrentAcrossDispatcherSwitch() {
        prewarmedDispatcher("allure-coroutine-worker").use { worker ->
            var returnedValue: String? = null
            var workerThread: String? = null
            var workerContextWasRestored = false
            val body: suspend StepScope.() -> String = {
                withContext(worker) {
                    workerThread = Thread.currentThread().name
                    name("Load order 42")
                    parameter(
                        name = "orderId",
                        value = 42,
                        excluded = true,
                        mode = Parameter.Mode.MASKED,
                    )
                    Allure.attachment("worker evidence", "service ready")
                    "ready"
                }
            }

            val results =
                runWithinTestContext {
                    val lifecycle = Allure.getLifecycle()
                    returnedValue = step(name = "Load order", body = body)
                    workerContextWasRestored = withContext(worker) { lifecycle.currentExecutableKey.isEmpty }
                }

            val recordedStep =
                results.testResults
                    .single()
                    .steps
                    .single()
            val attachmentStep = recordedStep.steps.single()
            val recordedAttachment = attachmentStep.attachments.single()

            assertThat(returnedValue).isEqualTo("ready")
            assertThat(workerThread).startsWith("allure-coroutine-worker")
            assertThat(workerContextWasRestored).isTrue()
            assertThat(recordedStep.name).isEqualTo("Load order 42")
            assertThat(recordedStep.status).isEqualTo(Status.PASSED)
            assertThat(recordedStep.stage).isEqualTo(Stage.FINISHED)
            assertThat(recordedStep.parameters.single().name).isEqualTo("orderId")
            assertThat(recordedStep.parameters.single().value).isEqualTo("42")
            assertThat(recordedStep.parameters.single().excluded).isTrue()
            assertThat(recordedStep.parameters.single().mode).isEqualTo(Parameter.Mode.MASKED)
            assertThat(recordedAttachment.name).isEqualTo("worker evidence")
            assertThat(results.getAttachmentContentAsString(recordedAttachment)).isEqualTo("service ready")
        }
    }

    @Test
    @Description(
        "A captured Allure context gives concurrent child coroutines isolated local stacks, so their suspend steps " +
            "are recorded as siblings and every worker is restored.",
    )
    fun shouldIsolateParallelSiblingCoroutines() {
        prewarmedDispatcher("allure-coroutine-first").use { firstWorker ->
            prewarmedDispatcher("allure-coroutine-second").use { secondWorker ->
                var returnedValues: List<String> = emptyList()
                var firstContextWasRestored = false
                var secondContextWasRestored = false

                val results =
                    runWithinTestContext {
                        val lifecycle = Allure.getLifecycle()
                        val firstEntered = CompletableDeferred<Unit>()
                        val secondEntered = CompletableDeferred<Unit>()
                        val release = CompletableDeferred<Unit>()

                        returnedValues =
                            withAllureContext {
                                coroutineScope {
                                    val first =
                                        async(firstWorker) {
                                            step("first sibling") {
                                                firstEntered.complete(Unit)
                                                release.await()
                                                "first"
                                            }
                                        }
                                    val second =
                                        async(secondWorker) {
                                            step("second sibling") {
                                                secondEntered.complete(Unit)
                                                release.await()
                                                "second"
                                            }
                                        }

                                    firstEntered.await()
                                    secondEntered.await()
                                    release.complete(Unit)
                                    awaitAll(first, second)
                                }
                            }

                        firstContextWasRestored =
                            withContext(firstWorker) { lifecycle.currentExecutableKey.isEmpty }
                        secondContextWasRestored =
                            withContext(secondWorker) { lifecycle.currentExecutableKey.isEmpty }
                    }

                val recordedSteps = results.testResults.single().steps

                assertThat(returnedValues).containsExactly("first", "second")
                assertThat(firstContextWasRestored).isTrue()
                assertThat(secondContextWasRestored).isTrue()
                assertThat(recordedSteps)
                    .extracting<String> { it.name }
                    .containsExactlyInAnyOrder("first sibling", "second sibling")
                assertThat(recordedSteps).allSatisfy { recordedStep ->
                    assertThat(recordedStep.steps).isEmpty()
                    assertThat(recordedStep.status).isEqualTo(Status.PASSED)
                    assertThat(recordedStep.stage).isEqualTo(Stage.FINISHED)
                }
            }
        }
    }

    @Test
    @Description(
        "A captured empty Allure context masks an existing worker binding, then restores that binding and can be " +
            "closed without leaking thread state.",
    )
    fun shouldPropagateMissingContextAndRestoreWorker() {
        prewarmedDispatcher("allure-coroutine-masked-worker").use { worker ->
            var contextWasEmpty = false
            var workerContextWasRestored = false
            var workerContextWasCleared = false

            runWithinTestContext {
                val lifecycle = Allure.getLifecycle()
                val testKey = lifecycle.currentExecutableKey.orElseThrow()
                val emptyContext =
                    lifecycle.bindEmpty().use {
                        lifecycle.asCoroutineContext()
                    }
                val workerBinding = withContext(worker) { lifecycle.bind(testKey) }

                try {
                    withContext(worker + emptyContext) {
                        contextWasEmpty = lifecycle.currentExecutableKey.isEmpty
                    }
                    workerContextWasRestored =
                        withContext(worker) {
                            lifecycle.currentExecutableKey.orElse(null) == testKey
                        }
                } finally {
                    workerBinding.close()
                }

                workerContextWasCleared =
                    withContext(worker) {
                        lifecycle.currentExecutableKey.isEmpty
                    }
            }

            assertThat(contextWasEmpty).isTrue()
            assertThat(workerContextWasRestored).isTrue()
            assertThat(workerContextWasCleared).isTrue()
        }
    }

    @Test
    @Description("Cancelling a running suspend step records it as broken, stops it, and rethrows cancellation.")
    fun shouldRecordAndRethrowCancellation() {
        var observedCancellation: CancellationException? = null

        val results =
            runWithinTestContext {
                supervisorScope {
                    val entered = CompletableDeferred<Unit>()
                    val deferred =
                        async {
                            step("Wait for event") {
                                entered.complete(Unit)
                                awaitCancellation()
                            }
                        }

                    entered.await()
                    deferred.cancel(CancellationException("event cancelled"))
                    try {
                        deferred.await()
                    } catch (exception: CancellationException) {
                        observedCancellation = exception
                    }
                }
            }

        val recordedStep =
            results.testResults
                .single()
                .steps
                .single()

        assertThat(observedCancellation).isNotNull()
        assertThat(observedCancellation).hasMessage("event cancelled")
        assertThat(recordedStep.status).isEqualTo(Status.BROKEN)
        assertThat(recordedStep.stage).isEqualTo(Stage.FINISHED)
        assertThat(recordedStep.statusDetails.message).isEqualTo("event cancelled")
    }

    @Test
    @Description("An assertion error from a suspend step is recorded as failed and rethrown unchanged.")
    fun shouldRecordAndRethrowAssertionFailure() {
        prewarmedDispatcher("allure-coroutine-failure").use { worker ->
            var observedFailure: Throwable? = null

            val results =
                runWithinTestContext {
                    try {
                        step("Verify response") {
                            withContext(worker) {
                                throw AssertionError("response differed")
                            }
                        }
                    } catch (throwable: Throwable) {
                        observedFailure = throwable
                    }
                }

            val recordedStep =
                results.testResults
                    .single()
                    .steps
                    .single()

            assertThat(observedFailure).isInstanceOf(AssertionError::class.java)
            assertThat(observedFailure).hasMessage("response differed")
            assertThat(recordedStep.status).isEqualTo(Status.FAILED)
            assertThat(recordedStep.stage).isEqualTo(Stage.FINISHED)
            assertThat(recordedStep.statusDetails.message).isEqualTo("response differed")
        }
    }

    private fun prewarmedDispatcher(name: String): ExecutorCoroutineDispatcher {
        val dispatcher =
            Executors
                .newSingleThreadExecutor { runnable ->
                    Thread(runnable, name).apply { isDaemon = true }
                }.asCoroutineDispatcher()
        runBlocking {
            withContext(dispatcher) {
                Unit
            }
        }
        return dispatcher
    }

    private fun runWithinTestContext(block: suspend () -> Unit): AllureResults =
        RunUtils.runWithinTestContext(
            Runnable {
                runBlocking {
                    block()
                }
            },
        )
}
