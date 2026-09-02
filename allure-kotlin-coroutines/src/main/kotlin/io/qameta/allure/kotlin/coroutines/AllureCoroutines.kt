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
@file:JvmName("AllureCoroutines")

package io.qameta.allure.kotlin.coroutines

import io.qameta.allure.Allure
import io.qameta.allure.AllureExternalKey
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.AllureThreadBinding
import io.qameta.allure.kotlin.extensions.StepScope
import io.qameta.allure.model.Parameter
import io.qameta.allure.model.Status
import io.qameta.allure.model.StepResult
import io.qameta.allure.util.ResultsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Runs [body] as an Allure step named [name] while preserving the step across coroutine suspensions.
 *
 * The receiver targets this step by identity, so it remains safe to rename the step or add parameters after a
 * dispatcher switch. Cancellation and other failures are recorded using Allure's standard status mapping and are
 * always rethrown.
 */
public suspend fun <T> step(
    name: String = "step",
    body: suspend StepScope.() -> T,
): T {
    val lifecycle = Allure.getLifecycle()
    val parentKey = lifecycle.currentExecutableKey.orElse(null) ?: return body(NoopStepScope)
    val stepKey = AllureExternalKey.random(CoroutineStepKey::class.java)
    val entered = AtomicBoolean()

    lifecycle.startStep(parentKey, stepKey, StepResult().setName(name))

    try {
        return withContext(AllureCoroutineContextElement.continuing(lifecycle, stepKey)) {
            entered.set(true)
            try {
                val result = body(CoroutineStepScope(lifecycle, stepKey))
                lifecycle.updateStep(stepKey) { stepResult ->
                    if (stepResult.status == null) {
                        stepResult.status = Status.PASSED
                    }
                }
                result
            } catch (throwable: Throwable) {
                lifecycle.recordFailure(stepKey, throwable)
                throw throwable
            } finally {
                lifecycle.stopStep(stepKey)
            }
        }
    } catch (throwable: Throwable) {
        if (!entered.get()) {
            lifecycle.recordFailure(stepKey, throwable)
        }
        throw throwable
    } finally {
        if (!entered.get()) {
            lifecycle.stopStep(stepKey)
        }
    }
}

/**
 * Captures the current Allure executable as a coroutine context element.
 *
 * Each coroutine resume receives a detached local execution stack anchored to the captured test, fixture, or step.
 * This makes sibling coroutines independent while still attaching their steps and attachments to the same parent.
 * An absent executable is captured explicitly and masks unrelated Allure state on worker threads.
 */
public fun allureContext(): CoroutineContext = Allure.getLifecycle().asCoroutineContext()

/**
 * Captures the current executable owned by this lifecycle as a coroutine context element.
 *
 * An absent executable is captured explicitly, so applying the returned context masks any unrelated Allure context
 * already present on a worker thread. The worker's previous context is restored afterwards.
 */
public fun AllureLifecycle.asCoroutineContext(): CoroutineContext =
    AllureCoroutineContextElement.detached(this, currentExecutableKey.orElse(null))

/**
 * Runs [body] with a detached copy of the current Allure execution context from [lifecycle].
 *
 * The context is captured when this function is called. Child coroutines receive isolated local execution stacks,
 * and every thread's previous Allure context is restored after use.
 */
public suspend fun <T> withAllureContext(
    lifecycle: AllureLifecycle = Allure.getLifecycle(),
    body: suspend CoroutineScope.() -> T,
): T = withContext(lifecycle.asCoroutineContext(), body)

private fun AllureLifecycle.recordFailure(
    key: AllureExternalKey,
    throwable: Throwable,
) {
    updateStep(key) { stepResult ->
        stepResult.status = ResultsUtils.getStatus(throwable).orElse(Status.BROKEN)
        stepResult.statusDetails = ResultsUtils.getStatusDetails(throwable).orElse(null)
    }
}

private class CoroutineStepScope(
    private val lifecycle: AllureLifecycle,
    private val key: AllureExternalKey,
) : StepScope {
    override fun name(name: String) {
        lifecycle.updateStep(key) { stepResult -> stepResult.name = name }
    }

    override fun <T> parameter(
        name: String,
        value: T,
        excluded: Boolean?,
        mode: Parameter.Mode?,
    ): T {
        val parameter = ResultsUtils.createParameter(name, value, excluded, mode)
        lifecycle.updateStep(key) { stepResult -> stepResult.parameters.add(parameter) }
        return value
    }
}

private object NoopStepScope : StepScope {
    override fun name(name: String) = Unit

    override fun <T> parameter(
        name: String,
        value: T,
        excluded: Boolean?,
        mode: Parameter.Mode?,
    ): T = value
}

private class AllureCoroutineContextElement private constructor(
    private val lifecycle: AllureLifecycle,
    private val executableKey: AllureExternalKey?,
    private val bindingMode: BindingMode,
) : ThreadContextElement<AllureThreadBinding> {
    override val key: CoroutineContext.Key<AllureCoroutineContextElement>
        get() = ContextKey

    override fun updateThreadContext(context: CoroutineContext): AllureThreadBinding =
        executableKey?.let { key ->
            when (bindingMode) {
                BindingMode.CONTINUING -> lifecycle.bind(key)
                BindingMode.DETACHED -> lifecycle.bindDetached(key)
            }
        } ?: lifecycle.bindEmpty()

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: AllureThreadBinding,
    ) {
        oldState.close()
    }

    private enum class BindingMode {
        CONTINUING,
        DETACHED,
    }

    companion object ContextKey : CoroutineContext.Key<AllureCoroutineContextElement> {
        fun continuing(
            lifecycle: AllureLifecycle,
            key: AllureExternalKey,
        ): AllureCoroutineContextElement = AllureCoroutineContextElement(lifecycle, key, BindingMode.CONTINUING)

        fun detached(
            lifecycle: AllureLifecycle,
            key: AllureExternalKey?,
        ): AllureCoroutineContextElement = AllureCoroutineContextElement(lifecycle, key, BindingMode.DETACHED)
    }
}

private object CoroutineStepKey
