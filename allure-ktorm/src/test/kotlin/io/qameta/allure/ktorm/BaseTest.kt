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

import io.qameta.allure.test.AllureResults
import io.qameta.allure.test.RunUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.ktorm.database.Database
import org.ktorm.database.use
import org.ktorm.dsl.Query
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.date
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import java.io.IOException
import java.io.Serializable
import java.time.LocalDate

open class BaseTest {
    lateinit var database: Database

    @BeforeEach
    open fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            logger = AllureKtormLogger(),
            alwaysQuoteIdentifiers = true
        )

        execSqlScript("init-data.sql")
    }

    @AfterEach
    open fun destroy() {
        execSqlScript("drop-data.sql")
    }

    protected fun execSqlScript(filename: String) {
        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                javaClass.classLoader
                    ?.getResourceAsStream(filename)
                    ?.bufferedReader()
                    ?.use { reader ->
                        for (sql in reader.readText().split(';')) {
                            if (sql.any { it.isLetterOrDigit() }) {
                                statement.executeUpdate(sql)
                            }
                        }
                    }
            }
        }
    }

    protected fun execute(query:  () -> Any): AllureResults {
        return RunUtils.runWithinTestContext {
            try {
                query()
            } catch (e: IOException) {
                throw RuntimeException("Could not execute query", e)
            }
        }
    }

    data class LocationWrapper(val underlying: String = "") : Serializable

    interface Department : Entity<Department> {
        companion object : Entity.Factory<Department>()

        val id: Int
        var name: String
        var location: String
        var mixedCase: String?
    }

    open class Departments(alias: String?) : Table<Department>("t_department", alias) {
        companion object : Departments(null)

        override fun aliased(alias: String) = Departments(alias)

        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").bindTo { it.name }
        val location = varchar("location").bindTo { it.location }
        val mixedCase = varchar("mixedCase").bindTo { it.mixedCase }
    }

    val Database.departments get() = this.sequenceOf(Departments)
}
