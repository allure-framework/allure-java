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
package io.qameta.allure.jooq;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
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

import static java.lang.Boolean.FALSE;

/**
 * Captures jOOQ query execution as Allure steps.
 *
 * <p>Register this execute listener in jOOQ configuration to report SQL rendering, record fetching, result processing, and execution failures. Pass a lifecycle when tests or embedded runtimes need isolation.</p>
 */
public class AllureJooq implements ExecuteListener {

    private static final String STEP_KEY = "io.qameta.allure.jooq.AllureJooq.STEP_KEY";
    private static final String DO_BUFFER = "io.qameta.allure.jooq.AllureJooq.DO_BUFFER";

    private final AllureLifecycle lifecycle;

    /**
     * Creates an Allure jooq with default configuration.
     */
    public AllureJooq() {
        this(Allure.getLifecycle());
    }

    /**
     * Creates an Allure jooq with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public AllureJooq(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderEnd(final ExecuteContext ctx) {
        if (!lifecycle.getCurrentExecutableKey().isPresent()) {
            return;
        }

        final String stepName = stepName(ctx);
        final AllureExternalKey stepKey = AllureExternalKey.random(AllureJooq.class);
        ctx.data(STEP_KEY, stepKey);
        lifecycle.startStep(
                stepKey, new StepResult()
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordEnd(final ExecuteContext ctx) {
        if (ctx.recordLevel() > 0) {
            return;
        }

        if (!lifecycle.getCurrentExecutableKey().isPresent()) {
            return;
        }

        final Record record = ctx.record();
        if (record != null && !FALSE.equals(ctx.data(DO_BUFFER))) {
            attachResultSet(record);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resultStart(final ExecuteContext ctx) {
        ctx.data(DO_BUFFER, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resultEnd(final ExecuteContext ctx) {
        if (!lifecycle.getCurrentExecutableKey().isPresent()) {
            return;
        }

        attachResultSet(ctx.result());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void end(final ExecuteContext ctx) {
        final AllureExternalKey stepKey = (AllureExternalKey) ctx.data(STEP_KEY);
        if (Objects.isNull(stepKey)) {
            return;
        }

        // the step was opened ambiently (renderEnd) so result-set attachments nest under it; finish it the same way
        // (ambient pop) — a manual stopStep(key) would not unbind the thread, leaking the step onto the stack
        lifecycle.updateStep(stepKey, sr -> sr.setStatus(Status.PASSED));
        lifecycle.stopStep();
    }

    private void attachResultSet(final Formattable formattable) {
        if (Objects.nonNull(formattable)) {
            Allure.attachment("ResultSet", "text/csv", formattable.formatCSV());
        }
    }

}
