/*
 *  Copyright 2016-2022 Qameta Software OÜ
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
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.Formattable;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Routine;

import java.util.Objects;
import java.util.UUID;

import static java.lang.Boolean.FALSE;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureJooq implements ExecuteListener {

    private static final String STEP_UUID
            = "io.qameta.allure.jooq.AllureJooq.STEP_UUID";
    private static final String DO_BUFFER
            = "io.qameta.allure.jooq.AllureJooq.DO_BUFFER";

    private final AllureLifecycle lifecycle;

    public AllureJooq() {
        this(Allure.getLifecycle());
    }

    public AllureJooq(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }
    
    @Override
    public void renderEnd(final ExecuteContext ctx) {
        if (!lifecycle.getCurrentTestCaseOrStep().isPresent()) {
            return;
        }

        final String stepName = stepName(ctx);
        final String uuid = UUID.randomUUID().toString();
        ctx.data(STEP_UUID, uuid);
        lifecycle.startStep(uuid, new StepResult()
                .setName(stepName)
        );
    }

    private String stepName(final ExecuteContext ctx) {
        final Query query = ctx.query();
        if (query != null) {
            return ctx.dsl().renderInlined(query);
        }

        final Routine<?> routine = ctx.routine();
        if (ctx.routine() != null) {
            return ctx.dsl().renderInlined(routine);
        }

        final String sql = ctx.sql();
        if (Objects.nonNull(sql) && !sql.isEmpty()) {
            return sql;
        }

        final String[] batchSQL = ctx.batchSQL();
        if (batchSQL.length > 0 && batchSQL[batchSQL.length - 1] != null) {
            return String.join("\n", batchSQL);
        }
        return "UNKNOWN";
    }

    @Override
    public void recordEnd(final ExecuteContext ctx) {
        if (ctx.recordLevel() > 0) {
            return;
        }

        if (!lifecycle.getCurrentTestCaseOrStep().isPresent()) {
            return;
        }

        final Record record = ctx.record();
        if (record != null && !FALSE.equals(ctx.data(DO_BUFFER))) {
            attachResultSet(record);
        }
    }

    @Override
    public void resultStart(final ExecuteContext ctx) {
        ctx.data(DO_BUFFER, false);
    }

    @Override
    public void resultEnd(final ExecuteContext ctx) {
        if (!lifecycle.getCurrentTestCaseOrStep().isPresent()) {
            return;
        }

        attachResultSet(ctx.result());
    }

    @Override
    public void end(final ExecuteContext ctx) {
        if (!lifecycle.getCurrentTestCaseOrStep().isPresent()) {
            return;
        }

        final String stepUuid = (String) ctx.data(STEP_UUID);
        if (Objects.isNull(stepUuid)) {
            return;
        }

        lifecycle.updateStep(stepUuid, sr -> sr.setStatus(Status.PASSED));
        lifecycle.stopStep(stepUuid);
    }

    private void attachResultSet(final Formattable formattable) {
        if (Objects.nonNull(formattable)) {
            Allure.addAttachment("ResultSet", "text/csv", formattable.formatCSV());
        }
    }

}
