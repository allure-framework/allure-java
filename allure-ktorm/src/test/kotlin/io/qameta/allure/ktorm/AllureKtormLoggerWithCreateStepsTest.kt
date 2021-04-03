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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import io.qameta.allure.model.Attachment
import io.qameta.allure.model.StepResult
import io.qameta.allure.model.TestResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.select

internal class AllureKtormLoggerWithCreateStepsTest : BaseTest() {

    @BeforeEach
    override fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            logger = AllureKtormLogger(createSqlSteps = true),
            alwaysQuoteIdentifiers = true
        )

        execSqlScript("init-data.sql")
    }

    @Test
    fun selectShouldCreateStepAndAttachments() {
        val results = execute(database.from(Departments).select()::rowSet)

        assertThat(results.testResults.flatMap(TestResult::getSteps))
            .extracting(StepResult::getName)
            .containsExactly("Executed SQL query")

        assertThat(results.testResults.flatMap(TestResult::getSteps).flatMap(StepResult::getAttachments))
            .extracting(Attachment::getName)
            .containsExactly("SQL")

        assertThat(results.testResults.flatMap(TestResult::getAttachments))
            .extracting(Attachment::getName)
            .containsExactly("PARAMETERS", "RESULTS")
    }

    @Test
    fun insertShouldCreateStepAndAttachments() {
        val results = execute {
            database.insert(Departments) {
                set(it.name, "John")
                set(it.location, "Moscow")
            }
        }

        assertThat(results.testResults.flatMap(TestResult::getSteps))
            .extracting(StepResult::getName)
            .containsExactly("Executed SQL query")

        assertThat(results.testResults.flatMap(TestResult::getSteps).flatMap(StepResult::getAttachments))
            .extracting(Attachment::getName)
            .containsExactly("SQL")

        assertThat(results.testResults.flatMap(TestResult::getAttachments))
            .extracting(Attachment::getName)
            .containsExactly("PARAMETERS")
    }
}
