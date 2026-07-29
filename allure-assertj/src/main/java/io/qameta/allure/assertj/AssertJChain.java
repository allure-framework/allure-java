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
package io.qameta.allure.assertj;

import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.assertj.core.api.AbstractAssert;

import java.util.Optional;

/**
 * Parent Allure step for one AssertJ assertion chain.
 *
 * <p>A chain is the stable container for all meaningful fluent operations produced by one AssertJ assertion object.
 * {@link AssertJRecorder} creates it when user code calls an AssertJ factory such as {@code assertThat(actual)},
 * stores it by assertion object identity, and appends one {@link AssertJOperation} child for every reported fluent
 * call. Methods such as {@code extracting}, {@code first}, or {@code asInstanceOf} can return another assertion
 * object, but they should still read as the same assertion story, so the returned assertion is associated with this
 * chain instead of creating an unrelated top-level step.</p>
 *
 * <p>For a scalar assertion:</p>
 * <pre>{@code
 * assertThat("Data").hasSize(4)
 *
 * assert "Data"
 *   has size 4
 * }</pre>
 *
 * <p>For an assertion with a description, the parent step is renamed while the operation history stays visible:</p>
 * <pre>{@code
 * assertThat(user).as("user profile").isNotNull()
 *
 * assert user profile
 *   described as "user profile"
 *   is not null
 * }</pre>
 *
 * <p>For navigation or extraction, later checks remain under the same parent:</p>
 * <pre>{@code
 * assertThat(results).extracting(Result::getName).containsExactly("passed")
 *
 * assert 1 Result item
 *   extracts Result::getName -> 1 string
 *   contains exactly ["passed"]
 * }</pre>
 *
 * <p>This class is intentionally only a small mutable model around the retained {@link StepResult}. It owns the
 * parent step name, status, timing, and child operation list. It does not decide which AssertJ methods are meaningful
 * or how subjects and arguments are rendered; those decisions belong to {@link AssertJRecorder},
 * {@link AssertJMethodSupport}, and {@link AssertJValueRenderer}.</p>
 */
final class AssertJChain {

    private static final String ASSERTJ_STEP_PREFIX = "assert ";

    private final AllureExternalKey key;

    private final AbstractAssert<?, ?> assertion;

    private final StepResult step;

    AssertJChain(final AbstractAssert<?, ?> assertion, final String subject) {
        this.key = AllureExternalKey.random(AllureAspectJ.class);
        this.assertion = assertion;
        this.step = new StepResult()
                .setName(chainName(subject))
                .setStatus(Status.PASSED)
                .setStage(Stage.FINISHED)
                .setStart(System.currentTimeMillis())
                .setStop(System.currentTimeMillis());
    }

    AllureExternalKey getKey() {
        return key;
    }

    AbstractAssert<?, ?> getAssertion() {
        return assertion;
    }

    StepResult getStep() {
        return step;
    }

    void addOperation(final AssertJOperation operation) {
        step.getSteps().add(operation.getStep());
    }

    void rename(final Optional<String> description) {
        description.ifPresent(value -> step.setName(chainName(value)));
    }

    void updateStatus(final Status status, final StatusDetails details) {
        step
                .setStatus(status)
                .setStatusDetails(details);
        finish();
    }

    void finish() {
        step.setStop(System.currentTimeMillis());
    }

    private String chainName(final String subject) {
        return AssertJValueRenderer.truncateStepName(ASSERTJ_STEP_PREFIX + subject);
    }
}
