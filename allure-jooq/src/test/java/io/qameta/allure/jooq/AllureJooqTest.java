/*
 *  Copyright 2023 Qameta Software OÜ
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
package io.qameta.allure.jooq;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureJooqTest {

    @Test
    void shouldSupportFetchSqlStatements() {
        final AllureResults results = execute(dsl -> dsl.fetchSingle("select 1"));

        final TestResult result = results.getTestResults().get(0);
        assertThat(result.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("select 1", Status.PASSED)
                );
    }

    @Test
    void shouldAddResultSetsAsAttachments() {
        final AllureResults results = execute(dsl -> dsl.fetchSingle("select 1 as one, 2 as two"));
        final TestResult result = results.getTestResults().get(0);
        final StepResult step = result.getSteps().get(0);
        assertThat(step.getAttachments())
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactly(
                        tuple("ResultSet", "text/csv")
                );

        final Attachment attachment = step.getAttachments().get(0);

        final byte[] content = results.getAttachments().get(attachment.getSource());

        assertThat(new String(content, StandardCharsets.UTF_8))
                .contains("one,two\n1,2\n");

    }

    @Test
    void shouldSupportCreateTableStatements() {
        final AllureResults results = execute(dsl -> {
            final Name tableName = DSL.name("first_table");
            final Field<Long> id = DSL.field("id", SQLDataType.BIGINT);
            final Field<String> name = DSL.field("name", SQLDataType.VARCHAR);
            dsl.createTable(tableName)
                    .column(id)
                    .column(name)
                    .primaryKey(id)
                    .execute();

            final Table<Record> table = DSL.table(tableName);

            dsl.insertInto(table, id, name)
                    .values(1L, "first")
                    .values(2L, "second")
                    .execute();
        });

        final TestResult result = results.getTestResults().get(0);
        assertThat(result.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("create table \"first_table\" (\"id\" bigint, \"name\" varchar, primary key (\"id\"))", Status.PASSED),
                        tuple("insert into \"first_table\" (id, name) values (1, 'first'), (2, 'second')", Status.PASSED)
                );
    }

    private static AllureResults execute(final Consumer<DSLContext> dslContextConsumer) {
        final EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
        try (EmbeddedPostgres postgres = builder.start()) {
            final DataSource dataSource = postgres.getPostgresDatabase();

            final DataSourceConnectionProvider connectionProvider = new DataSourceConnectionProvider(dataSource);
            final DefaultConfiguration configuration = new DefaultConfiguration();
            configuration.set(SQLDialect.POSTGRES);
            configuration.set(connectionProvider);

            return runWithinTestContext(
                    () -> {
                        final DefaultDSLContext dsl = new DefaultDSLContext(configuration);
                        dslContextConsumer.accept(dsl);
                    },
                    Allure::setLifecycle,
                    allureLifecycle -> configuration.setExecuteListener(new AllureJooq(allureLifecycle))
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
