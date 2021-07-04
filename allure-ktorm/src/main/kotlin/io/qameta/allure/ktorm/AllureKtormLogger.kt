/*
 *  Copyright 2021 Qameta Software OÜ
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
package io.qameta.allure.ktorm

import io.qameta.allure.Allure
import io.qameta.allure.model.Status
import io.qameta.allure.model.StepResult
import org.ktorm.logging.Logger
import java.util.UUID

/**
 * Allure logger for Ktorm.
 *
 * Create attachments with sql, parameters and results.
 * Unfortunately now we can't add parameters and results to created step.
 *
 * @author crazyMoonkin(Sergey Frolov)
 *
 * @property createSqlSteps enable creating steps
 */
open class AllureKtormLogger(
    private val createSqlSteps: Boolean = false,
    private val attachParams: Boolean = true,
    private val attachResults: Boolean = true,
) : Logger {

    override fun isTraceEnabled(): Boolean = false

    override fun trace(msg: String, e: Throwable?) {
        log(msg, e)
    }

    override fun isDebugEnabled(): Boolean = true

    override fun debug(msg: String, e: Throwable?) {
        log(msg, e)
    }

    override fun isInfoEnabled(): Boolean = false

    override fun info(msg: String, e: Throwable?) {
        log(msg, e)
    }

    override fun isWarnEnabled(): Boolean = false

    override fun warn(msg: String, e: Throwable?) {
        log(msg, e)
    }

    override fun isErrorEnabled(): Boolean = true

    override fun error(msg: String, e: Throwable?) {
        log(msg, e)
    }

    protected fun log(msg: String, e: Throwable?) {
        val typedMessage = msg.toTypedMessage()
        lateinit var stepUUID: String

        if (createSqlSteps && typedMessage?.type == MessageType.SQL) {
            stepUUID = UUID.randomUUID().toString()
            createStep(stepUUID = stepUUID)
        }
        try {
            typedMessage?.let {
                when {
                    typedMessage.type == MessageType.SQL
                            || typedMessage.type == MessageType.PARAMETERS && attachParams
                            || typedMessage.type == MessageType.RESULTS && attachResults -> {
                        Allure.addAttachment(typedMessage.type.name, typedMessage.msg)
                    }
                }
            }

        } finally {
            if (createSqlSteps && typedMessage?.type == MessageType.SQL) {
                e?.let { Allure.getLifecycle().updateStep(stepUUID) { it.status = Status.FAILED } }
                Allure.getLifecycle().stopStep(stepUUID)
            }
        }
    }

    private fun createStep(stepUUID: String) = Allure.getLifecycle().startStep(
        stepUUID,
        StepResult().apply {
            name = "Executed SQL query"
            status = Status.PASSED
        }
    )

    /**
     * Split logged messages and convert it to TypedMessage
     * Logged messages is type and message separated by colon
     */
    private fun String.toTypedMessage() = split(": ").takeIf { it.size == 2 }?.let { msgParts ->
        MessageType.values().firstOrNull { it.name == msgParts[0].toUpperCase() }?.let {
            TypedMessage(
                type = it,
                msg = msgParts[1]
            )
        }
    }

    protected enum class MessageType {
        SQL,
        RESULTS,
        PARAMETERS,
    }

    protected data class TypedMessage(val type: MessageType, val msg: String)
}
