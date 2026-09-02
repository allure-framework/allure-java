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
package io.qameta.allure.kotlin.extensions

import io.qameta.allure.Allure
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.Description
import io.qameta.allure.http.HttpExchange
import io.qameta.allure.http.HttpExchangeRequest
import io.qameta.allure.model.Parameter
import io.qameta.allure.model.Stage
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
import io.qameta.allure.test.AllureResults
import io.qameta.allure.test.IsolatedLifecycle
import io.qameta.allure.test.RunUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.util.concurrent.CompletableFuture

@IsolatedLifecycle
internal class AllureExtensionsTest {
    @Test
    @Description("The Kotlin facade exposes an operation name for every public static method on Allure.")
    fun shouldCoverEveryPublicAllureOperation() {
        val allureOperations =
            Allure::class.java.declaredMethods
                .filter { method -> Modifier.isPublic(method.modifiers) && Modifier.isStatic(method.modifiers) }
                .map { method -> method.name }
                .toSortedSet()
        val facadeOperations =
            Class
                .forName("io.qameta.allure.kotlin.extensions.AllureKotlin")
                .declaredMethods
                .filter { method -> Modifier.isPublic(method.modifiers) && Modifier.isStatic(method.modifiers) }
                .map { method -> method.name }
                .filterNot { name -> name.endsWith("\$default") }
                .toSet()

        assertThat(facadeOperations).containsAll(allureOperations)
    }

    @Test
    @Description("The Kotlin lifecycle property reads and replaces the same process-wide lifecycle as Allure.")
    fun shouldExposeTheProcessWideLifecycleAsAProperty() {
        val originalLifecycle = lifecycle
        val replacementLifecycle = AllureLifecycle()

        try {
            lifecycle = replacementLifecycle

            assertThat(lifecycle).isSameAs(replacementLifecycle)
            assertThat(Allure.getLifecycle()).isSameAs(replacementLifecycle)
        } finally {
            lifecycle = originalLifecycle
        }
    }

    @Test
    @Description("A Unit-returning Kotlin lambda uses the default step name and records exactly one Allure step.")
    fun shouldRunUnitReturningStepWithoutOverloadAmbiguity() {
        var executions = 0

        val results =
            runWithinTestContext {
                step {
                    executions += 1
                }
            }

        val recordedSteps = results.testResults.single().steps

        assertThat(executions).isEqualTo(1)
        assertThat(recordedSteps).hasSize(1)
        assertThat(recordedSteps.single().name).isEqualTo("step")
        assertThat(recordedSteps.single().status).isEqualTo(Status.PASSED)
    }

    @Test
    @Description("A value-returning Kotlin step exposes StepScope with named parameter options.")
    fun shouldReturnValueAndExposeKotlinStepScopeAsReceiver() {
        var returnedValue: String? = null
        val body: StepScope.() -> String = {
            name("Read order 42")
            parameter(
                name = "orderId",
                value = 42,
                excluded = true,
                mode = Parameter.Mode.MASKED,
            )
            "ready"
        }

        val results =
            runWithinTestContext {
                returnedValue = step(name = "Read order", body = body)
            }

        val recordedStep =
            results.testResults
                .single()
                .steps
                .single()
        val recordedParameter = recordedStep.parameters.single()

        assertThat(returnedValue).isEqualTo("ready")
        assertThat(recordedStep.name).isEqualTo("Read order 42")
        assertThat(recordedStep.status).isEqualTo(Status.PASSED)
        assertThat(recordedParameter.name).isEqualTo("orderId")
        assertThat(recordedParameter.value).isEqualTo("42")
        assertThat(recordedParameter.excluded).isTrue()
        assertThat(recordedParameter.mode).isEqualTo(Parameter.Mode.MASKED)
    }

    @Test
    @Description("Top-level stage and completed-step functions preserve stage nesting and explicit step status.")
    fun shouldRecordStagesAndCompletedSteps() {
        val results =
            runWithinTestContext {
                stage("prepare")
                step("customer created")
                stage("verify")
                step("optional check", Status.SKIPPED)
            }

        val recordedStages = results.testResults.single().steps
        val prepareStep = recordedStages[0].steps.single()
        val verifyStep = recordedStages[1].steps.single()

        assertThat(recordedStages.map { it.name }).containsExactly("prepare", "verify")
        assertThat(recordedStages.map { it.stage }).containsOnly(Stage.FINISHED)
        assertThat(recordedStages.map { it.status }).containsOnly(Status.PASSED)
        assertThat(prepareStep.name).isEqualTo("customer created")
        assertThat(prepareStep.status).isEqualTo(Status.PASSED)
        assertThat(verifyStep.name).isEqualTo("optional check")
        assertThat(verifyStep.status).isEqualTo(Status.SKIPPED)
    }

    @Test
    @Description("Top-level label and description functions enrich the current test with their Java API values.")
    fun shouldAddLabelsAndDescriptions() {
        val results =
            runWithinTestContext {
                epic("Checkout")
                feature("Payment")
                story("Saved card")
                suite("Web checkout")
                label(name = "component", value = "billing")
                description("**Markdown** description")
                descriptionHtml("<p>HTML description</p>")
            }

        val testResult = results.testResults.single()
        val labels = testResult.labels.map { it.name to it.value }

        assertThat(labels)
            .containsExactly(
                "epic" to "Checkout",
                "feature" to "Payment",
                "story" to "Saved card",
                "suite" to "Web checkout",
                "component" to "billing",
            )
        assertThat(testResult.description).isEqualTo("**Markdown** description")
        assertThat(testResult.descriptionHtml).isEqualTo("<p>HTML description</p>")
    }

    @Test
    @Description("The Kotlin parameter function supports named options and returns the original value.")
    fun shouldAddAParameterWithNamedOptions() {
        var returnedValue: String? = null

        val results =
            runWithinTestContext {
                returnedValue =
                    parameter(
                        name = "access token",
                        value = "secret",
                        excluded = true,
                        mode = Parameter.Mode.MASKED,
                    )
            }

        val recordedParameter =
            results.testResults
                .single()
                .parameters
                .single()

        assertThat(returnedValue).isEqualTo("secret")
        assertThat(recordedParameter.name).isEqualTo("access token")
        assertThat(recordedParameter.value).isEqualTo("secret")
        assertThat(recordedParameter.excluded).isTrue()
        assertThat(recordedParameter.mode).isEqualTo(Parameter.Mode.MASKED)
    }

    @Test
    @Description("Every Kotlin link overload records its name, type, and URL in the current test metadata.")
    fun shouldAddLinksWithNamedArguments() {
        val results =
            runWithinTestContext {
                link(url = "https://example.test")
                link(name = "Documentation", url = "https://example.test/docs")
                link(name = "Dashboard", type = "dashboard", url = "https://example.test/dashboard")
                issue(name = "BUG-42", url = "https://example.test/issues/42")
                tms(name = "CASE-7", url = "https://example.test/cases/7")
            }

        val recordedLinks = results.testResults.single().links

        assertThat(recordedLinks.map { Triple(it.name, it.type, it.url) })
            .containsExactly(
                Triple(null, null, "https://example.test"),
                Triple("Documentation", null, "https://example.test/docs"),
                Triple("Dashboard", "dashboard", "https://example.test/dashboard"),
                Triple("BUG-42", "issue", "https://example.test/issues/42"),
                Triple("CASE-7", "tms", "https://example.test/cases/7"),
            )
    }

    @Test
    @Description("The String overload writes UTF-8 text with text/plain metadata and a detected txt extension.")
    fun shouldAttachStringContentWithTextDefaults() {
        val results =
            runWithinTestContext {
                attachment(name = "diagnostics", content = "service ready")
            }

        val recordedAttachment = results.attachmentsRecursively.single()

        assertThat(recordedAttachment.name).isEqualTo("diagnostics")
        assertThat(recordedAttachment.type).isEqualTo("text/plain")
        assertThat(recordedAttachment.source).endsWith(".txt")
        assertThat(results.getAttachmentContentAsString(recordedAttachment)).isEqualTo("service ready")
    }

    @Test
    @Description("The ByteArray overload preserves arbitrary bytes and supplies binary attachment defaults.")
    fun shouldAttachByteArrayContentWithBinaryDefaults() {
        val payload = byteArrayOf(0, 1, 2, -1)

        val results =
            runWithinTestContext {
                attachment(name = "payload", content = payload)
            }

        val recordedAttachment = results.attachmentsRecursively.single()

        assertThat(recordedAttachment.name).isEqualTo("payload")
        assertThat(recordedAttachment.type).isEqualTo("application/octet-stream")
        assertThat(recordedAttachment.source).endsWith(".bin")
        assertThat(results.getAttachmentContent(recordedAttachment)).containsExactly(*payload)
    }

    @Test
    @Description("The InputStream overload accepts named metadata arguments and can suppress file extensions.")
    fun shouldAttachInputStreamContentWithNamedOptions() {
        val payload = "{\"status\":\"ready\"}".toByteArray()

        val results =
            runWithinTestContext {
                attachment(
                    name = "response",
                    content = payload.inputStream(),
                    type = "application/json",
                    fileExtension = "",
                )
            }

        val recordedAttachment = results.attachmentsRecursively.single()

        assertThat(recordedAttachment.name).isEqualTo("response")
        assertThat(recordedAttachment.type).isEqualTo("application/json")
        assertThat(recordedAttachment.source).endsWith("-attachment")
        assertThat(results.getAttachmentContent(recordedAttachment)).containsExactly(*payload)
    }

    @Test
    @Description("The async and HTTP exchange functions write their completed content as typed attachment steps.")
    fun shouldAttachAsyncContentAndHttpExchanges() {
        val exchange =
            HttpExchange
                .builder(HttpExchangeRequest.builder("GET", "https://example.test/orders/42").build())
                .build()

        val results =
            runWithinTestContext {
                attachmentAsync(
                    name = "worker output",
                    content = CompletableFuture.completedFuture("ready".byteInputStream()),
                    type = "text/plain",
                    fileExtension = "log",
                ).join()
                addHttpExchange(name = "HTTP exchange", exchange = exchange)
            }

        val attachmentSteps = results.testResults.single().steps
        val asyncAttachment = attachmentSteps[0].attachments.single()
        val httpAttachment = attachmentSteps[1].attachments.single()

        assertThat(attachmentSteps.map { it.name }).containsExactly("worker output", "HTTP exchange")
        assertThat(asyncAttachment.type).isEqualTo("text/plain")
        assertThat(asyncAttachment.source).endsWith(".log")
        assertThat(results.getAttachmentContentAsString(asyncAttachment)).isEqualTo("ready")
        assertThat(httpAttachment.type).isEqualTo(HttpExchange.CONTENT_TYPE)
        assertThat(httpAttachment.source).endsWith(HttpExchange.FILE_EXTENSION)
        assertThat(results.getAttachmentContent(httpAttachment)).isNotEmpty()
    }

    @Test
    @Description("Both global-error overloads write run-level errors through the configured Allure lifecycle.")
    fun shouldWriteThrowableAndStatusDetailsAsGlobalErrors() {
        val results =
            RunUtils.runTests(
                Allure.ThrowableContextRunnableVoid<AllureLifecycle> {
                    globalError(IllegalStateException("startup failed"))
                    globalError(StatusDetails().setMessage("environment unavailable").setKnown(true))
                },
            )

        val recordedErrors = results.globals.flatMap { it.errors }

        assertThat(recordedErrors).hasSize(2)
        assertThat(recordedErrors.map { it.message }).containsExactly("startup failed", "environment unavailable")
        assertThat(recordedErrors[0].trace).contains("IllegalStateException: startup failed")
        assertThat(recordedErrors[1].isKnown).isTrue()
        assertThat(recordedErrors.map { it.timestamp }).allSatisfy { timestamp ->
            assertThat(timestamp).isPositive()
        }
    }

    private fun runWithinTestContext(block: () -> Unit): AllureResults = RunUtils.runWithinTestContext(Runnable(block))
}
