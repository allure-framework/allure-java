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
import io.qameta.allure.model.TestResult
import org.junit.jupiter.api.Test
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.select

internal class AllureKtormLoggerTest : BaseTest() {

    @Test
    fun selectShouldCreateAttachments() {
        val results = execute(database.from(Departments).select()::rowSet)

        assertThat(results.testResults.flatMap(TestResult::getAttachments))
            .extracting(Attachment::getName)
            .containsExactly("SQL", "PARAMETERS", "RESULTS")
    }

    @Test
    fun insertShouldCreateAttachments() {
        val results = execute {
            database.insert(Departments) {
                set(it.name, "John")
                set(it.location, "Moscow")
            }
        }

        assertThat(results.testResults.flatMap(TestResult::getAttachments))
            .extracting(Attachment::getName)
            .containsExactly("SQL", "PARAMETERS")
    }
}
