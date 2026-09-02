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
@file:JvmName("AllureKotlin")

package io.qameta.allure.kotlin.extensions

import io.qameta.allure.Allure
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.AttachmentOptions
import io.qameta.allure.http.HttpExchange
import io.qameta.allure.model.Parameter
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

private const val TEXT_CONTENT_TYPE = "text/plain"
private const val BINARY_CONTENT_TYPE = "application/octet-stream"

/**
 * The process-wide Allure lifecycle used by the top-level reporting functions.
 *
 * Replacing it is a low-level integration operation. Applications normally only read this property when they need
 * APIs that are not part of the high-level facade.
 */
public var lifecycle: AllureLifecycle
    get() = Allure.getLifecycle()
    set(value) {
        Allure.setLifecycle(value)
    }

/** Adds an already-completed Allure step named [name] with [status]. */
public fun step(
    name: String,
    status: Status = Status.PASSED,
): Unit = Allure.step(name, status)

/**
 * Runs [body] as an Allure step named [name].
 *
 * The receiver exposes [StepScope], so the body can rename the step or add step parameters with Kotlin named
 * arguments. A single generic overload handles both [Unit]-returning and value-returning bodies without the Java SAM
 * overload ambiguity.
 */
public fun <T> step(
    name: String = "step",
    body: StepScope.() -> T,
): T =
    Allure.step(
        name,
        Allure.ThrowableContextRunnable<T, Allure.StepContext> { context -> body(AllureStepScope(context)) },
    )

/** Starts a semantic test stage named [name]. */
public fun stage(name: String): Unit = Allure.stage(name)

/** Adds an epic label to the current test. */
public fun epic(value: String): Unit = Allure.epic(value)

/** Adds a feature label to the current test. */
public fun feature(value: String): Unit = Allure.feature(value)

/** Adds a story label to the current test. */
public fun story(value: String): Unit = Allure.story(value)

/** Adds a suite label to the current test. */
public fun suite(value: String): Unit = Allure.suite(value)

/** Adds a label with [name] and [value] to the current test. */
public fun label(
    name: String,
    value: String,
): Unit = Allure.label(name, value)

/**
 * Adds a parameter to the current test and returns [value].
 *
 * Named arguments replace the Java overloads: [excluded] controls history-key calculation and [mode] controls how
 * the value is displayed.
 */
public fun <T> parameter(
    name: String,
    value: T,
    excluded: Boolean? = null,
    mode: Parameter.Mode? = null,
): T = Allure.parameter(name, value, excluded, mode)

/** Adds an issue link with [name] and [url] to the current test. */
public fun issue(
    name: String,
    url: String,
): Unit = Allure.issue(name, url)

/** Adds a test-management link with [name] and [url] to the current test. */
public fun tms(
    name: String,
    url: String,
): Unit = Allure.tms(name, url)

/** Adds [url] as a link to the current test. */
public fun link(url: String): Unit = Allure.link(url)

/** Adds a link with [name] and [url] to the current test. */
public fun link(
    name: String,
    url: String,
): Unit = Allure.link(name, url)

/** Adds a link with [name], [type], and [url] to the current test. */
public fun link(
    name: String,
    type: String,
    url: String,
): Unit = Allure.link(name, type, url)

/** Adds [description] in Markdown format as the current test description. */
public fun description(description: String): Unit = Allure.description(description)

/** Adds [descriptionHtml] in HTML format as the current test description. */
public fun descriptionHtml(descriptionHtml: String): Unit = Allure.descriptionHtml(descriptionHtml)

/** Adds a run-level error derived from [throwable]. */
public fun globalError(throwable: Throwable): Unit = Allure.globalError(throwable)

/** Adds a run-level error with [statusDetails]. */
public fun globalError(statusDetails: StatusDetails): Unit = Allure.globalError(statusDetails)

/**
 * Adds UTF-8 [content] as an Allure attachment.
 *
 * When [fileExtension] is `null`, Allure detects the extension from [type]. Pass an empty string to suppress the
 * extension.
 */
public fun attachment(
    name: String,
    content: String,
    type: String = TEXT_CONTENT_TYPE,
    fileExtension: String? = null,
): Unit = Allure.attachment(name, type, content, attachmentOptions(fileExtension))

/**
 * Adds [content] as an Allure attachment.
 *
 * When [fileExtension] is `null`, Allure detects the extension from [type]. Pass an empty string to suppress the
 * extension.
 */
public fun attachment(
    name: String,
    content: ByteArray,
    type: String = BINARY_CONTENT_TYPE,
    fileExtension: String? = null,
): Unit = Allure.attachment(name, type, content.inputStream(), attachmentOptions(fileExtension))

/**
 * Adds the bytes read from [content] as an Allure attachment.
 *
 * When [fileExtension] is `null`, Allure detects the extension from [type]. Pass an empty string to suppress the
 * extension.
 */
public fun attachment(
    name: String,
    content: InputStream,
    type: String = BINARY_CONTENT_TYPE,
    fileExtension: String? = null,
): Unit = Allure.attachment(name, type, content, attachmentOptions(fileExtension))

/**
 * Adds an attachment whose stream is supplied by [content] and returns a future completed after it is written.
 *
 * The owning test, fixture, or step waits for the attachment before it ends. When [fileExtension] is `null`, Allure
 * detects the extension from [type]; pass an empty string to suppress the extension.
 */
public fun attachmentAsync(
    name: String,
    content: CompletionStage<out InputStream>,
    type: String = BINARY_CONTENT_TYPE,
    fileExtension: String? = null,
): CompletableFuture<Void> = Allure.attachmentAsync(name, type, content, attachmentOptions(fileExtension))

/** Adds an already captured [exchange] as an Allure HTTP exchange attachment named [name]. */
public fun addHttpExchange(
    name: String,
    exchange: HttpExchange,
): Unit = Allure.addHttpExchange(name, exchange)

private fun attachmentOptions(fileExtension: String?): AttachmentOptions =
    if (fileExtension == null) {
        AttachmentOptions.empty()
    } else {
        AttachmentOptions.withFileExtension(fileExtension)
    }

private class AllureStepScope(
    private val context: Allure.StepContext,
) : StepScope {
    override fun name(name: String): Unit = context.name(name)

    override fun <T> parameter(
        name: String,
        value: T,
        excluded: Boolean?,
        mode: Parameter.Mode?,
    ): T = context.parameter(name, value, excluded, mode)
}
