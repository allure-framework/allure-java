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

import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;

import java.util.List;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * Child Allure step for one meaningful AssertJ fluent operation.
 *
 * <p>An operation is the report entry for one fluent method call inside an {@link AssertJChain}. The recorder creates
 * it before proceeding with the intercepted AssertJ call, marks it passed or failed after the call returns, and keeps
 * it attached to the chain that owns the assertion object. Earlier operations remain passed when a later operation
 * fails, so the report shows the exact point where the assertion chain stopped matching the expectation.</p>
 *
 * <p>For a simple assertion, each checked method becomes one operation:</p>
 * <pre>{@code
 * assertThat("Data").startsWith("Da").endsWith("ta")
 *
 * assert "Data"
 *   starts with "Da"
 *   ends with "ta"
 * }</pre>
 *
 * <p>For navigation methods, the operation name is enriched with the returned subject. The returned AssertJ object
 * still belongs to the same chain, so the report stays readable as one story:</p>
 * <pre>{@code
 * assertThat(users).first(InstanceOfAssertFactories.STRING).startsWith("alice")
 *
 * assert 1 string
 *   first element as InstanceOfAssertFactory -> "alice@example.org"
 *   starts with "alice"
 * }</pre>
 *
 * <p>For failures, this operation receives the failure status and status details, and the parent chain receives the
 * same status. This makes the failed operation visible without losing the successful context before it:</p>
 * <pre>{@code
 * assertThat("Data").startsWith("Da").hasSize(5)
 *
 * assert "Data"                 FAILED
 *   starts with "Da"             PASSED
 *   has size 5                   FAILED
 * }</pre>
 *
 * <p>Some AssertJ methods call other assertion methods internally. Those calls should not become extra child steps
 * because they would duplicate implementation details instead of user intent. The {@code nestedLevel} counter lets the
 * recorder reuse the active operation while those internal calls run, then finish only the user-visible operation.</p>
 */
final class AssertJOperation {

    private final AssertJChain chain;

    private final String methodName;

    private final StepResult step;

    private final boolean navigation;

    private String returnedSubject;

    private int nestedLevel;

    AssertJOperation(final AssertJChain chain,
                     final String methodName,
                     final String name,
                     final List<Parameter> parameters,
                     final boolean navigation) {
        this.chain = chain;
        this.methodName = methodName;
        this.navigation = navigation;
        this.step = new StepResult()
                .setName(name)
                .setParameters(parameters)
                .setStage(Stage.RUNNING)
                .setStart(System.currentTimeMillis());
    }

    AssertJChain getChain() {
        return chain;
    }

    StepResult getStep() {
        return step;
    }

    boolean isNavigation() {
        return navigation;
    }

    boolean isDescription() {
        return AssertJMethodSupport.isDescription(methodName);
    }

    boolean isNested() {
        return nestedLevel > 0;
    }

    AssertJOperation nested() {
        nestedLevel++;
        return this;
    }

    void leaveNested() {
        nestedLevel--;
    }

    void setReturnedSubject(final String subject) {
        if (returnedSubject != null) {
            return;
        }

        returnedSubject = subject;
        step.setName(AssertJValueRenderer.truncateStepName(step.getName() + " -> " + subject));
    }

    void passed() {
        if (step.getStatus() == null) {
            step.setStatus(Status.PASSED);
        }
        finish();
    }

    void failed(final Throwable throwable) {
        final Status status = getStatus(throwable).orElse(Status.BROKEN);
        final StatusDetails details = getStatusDetails(throwable).orElse(null);
        step
                .setStatus(status)
                .setStatusDetails(details);
        chain.updateStatus(status, details);
        finish();
    }

    private void finish() {
        step
                .setStage(Stage.FINISHED)
                .setStop(System.currentTimeMillis());
        chain.finish();
    }
}
