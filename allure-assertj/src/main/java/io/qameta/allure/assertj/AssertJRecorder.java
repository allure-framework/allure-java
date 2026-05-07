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

import io.qameta.allure.AllureLifecycle;
import org.assertj.core.api.AbstractAssert;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Records AssertJ objects by identity and builds one Allure step tree per assertion chain.
 *
 * <p>The recorder is the stateful part behind {@link AllureAspectJ}. The aspect only detects user-side AssertJ
 * factory calls and fluent operation calls; this class decides which {@link AssertJChain} owns the assertion object,
 * where a new {@link AssertJOperation} should be attached, and how pass/fail state should be reflected in the retained
 * Allure {@code StepResult} tree.</p>
 *
 * <p>Each aspect thread gets its own recorder instance. Assertion objects are tracked in an {@link IdentityHashMap}
 * because AssertJ assertion classes can override {@code equals} and {@code hashCode}; object identity is the only safe
 * way to know that a later fluent call belongs to the same assertion object that was created earlier.</p>
 *
 * <p>The normal hard-assertion flow is:</p>
 * <pre>{@code
 * assertThat("Data").startsWith("Da").endsWith("ta")
 *
 * assertionCreated(assertThat result, "Data")
 * startOperation(startsWith, ["Da"])
 * operationPassed(startsWith)
 * startOperation(endsWith, ["ta"])
 * operationPassed(endsWith)
 *
 * assert "Data"
 *   starts with "Da"
 *   ends with "ta"
 * }</pre>
 *
 * <p>Stored assertion instances keep separate chains because the map key is the assertion instance itself:</p>
 * <pre>{@code
 * final AbstractStringAssert<?> a = assertThat("alpha");
 * final AbstractStringAssert<?> b = assertThat("bravo");
 *
 * a.isEqualTo("alpha");
 * b.isEqualTo("bravo");
 *
 * assert "alpha"
 *   is equal to "alpha"
 * assert "bravo"
 *   is equal to "bravo"
 * }</pre>
 *
 * <p>Navigation operations such as {@code extracting}, {@code first}, and {@code asInstanceOf} may return new AssertJ
 * assertion objects. Those returned objects are registered against the existing chain, so later checks stay under the
 * same parent step:</p>
 * <pre>{@code
 * assertThat(results).extracting(Result::getName).containsExactly("passed")
 *
 * assert 1 Result item
 *   extracts Result::getName -> 1 string
 *   contains exactly ["passed"]
 * }</pre>
 *
 * <p>The {@code operations} stack tracks the currently executing user-visible operation. It has two jobs: assertions
 * created inside callbacks such as {@code satisfies} are attached beneath the active operation, and AssertJ internal
 * calls on the same chain are counted as nested work instead of being reported as extra steps.</p>
 *
 * <pre>{@code
 * assertThat("alpha").satisfies(value -> assertThat(value).startsWith("al"))
 *
 * assert "alpha"
 *   satisfies <lambda>
 *     assert "alpha"
 *       starts with "al"
 * }</pre>
 *
 * <p>Soft assertion failures are reported before {@code assertAll()} throws. The AssertJ error collector callback calls
 * {@link #softAssertionFailed(AssertionError)}, which marks the active operation and its chain as failed while
 * preserving the earlier passed operations.</p>
 */
final class AssertJRecorder {

    private final Map<AbstractAssert<?, ?>, AssertJChain> chains = new IdentityHashMap<>();

    private final Deque<AssertJOperation> operations = new ArrayDeque<>();

    private final AssertJValueRenderer renderer = new AssertJValueRenderer();

    void assertionCreated(final AllureLifecycle lifecycle,
                          final AbstractAssert<?, ?> assertion,
                          final Object actual) {
        if (chains.containsKey(assertion)) {
            return;
        }

        final AssertJOperation activeOperation = activeOperation();
        if (isNavigationResult(activeOperation)) {
            chains.put(assertion, activeOperation.getChain());
            return;
        }

        final AssertJChain chain = new AssertJChain(assertion, renderer.renderSubject(actual));
        chains.put(assertion, chain);
        attachChain(lifecycle, chain, activeOperation);
    }

    AssertJOperation startOperation(final AllureLifecycle lifecycle,
                                    final AbstractAssert<?, ?> assertion,
                                    final String methodName,
                                    final Object... args) {
        final AssertJChain chain = chainFor(lifecycle, assertion);
        final String normalizedName = AssertJMethodSupport.normalize(methodName);

        final AssertJOperation activeOperation = activeOperation();
        if (isInternalCallOnSameChain(activeOperation, chain)) {
            return activeOperation.nested();
        }

        final AssertJOperation operation = new AssertJOperation(
                chain,
                normalizedName,
                renderer.renderOperation(normalizedName, args),
                renderer.renderParameters(normalizedName, args),
                AssertJMethodSupport.isNavigation(normalizedName)
        );
        chain.addOperation(operation);
        operations.push(operation);
        return operation;
    }

    void operationPassed(final AssertJOperation operation, final Object result) {
        if (operation.isNested()) {
            pop(operation);
            return;
        }

        registerReturnedAssertion(operation, result);
        renameChainFromDescription(operation);
        operation.passed();
        pop(operation);
    }

    void operationFailed(final AssertJOperation operation, final Throwable throwable) {
        operation.failed(throwable);
        pop(operation);
    }

    void softAssertionFailed(final AssertionError error) {
        final AssertJOperation current = activeOperation();
        if (current != null) {
            current.failed(error);
        }
    }

    boolean isIgnored(final String methodName) {
        return AssertJMethodSupport.isIgnored(methodName);
    }

    private AssertJChain chainFor(final AllureLifecycle lifecycle, final AbstractAssert<?, ?> assertion) {
        final AssertJChain chain = chains.get(assertion);
        if (chain != null) {
            return chain;
        }

        final AssertJChain created = new AssertJChain(assertion, renderer.renderSubject(actualOf(assertion)));
        chains.put(assertion, created);
        attachChain(lifecycle, created, activeOperation());
        return created;
    }

    private void attachChain(final AllureLifecycle lifecycle,
                             final AssertJChain chain,
                             final AssertJOperation parentOperation) {
        if (parentOperation == null) {
            lifecycle.startStep(chain.getUuid(), chain.getStep());
            lifecycle.stopStep(chain.getUuid());
            return;
        }

        parentOperation.getStep().getSteps().add(chain.getStep());
    }

    private void registerReturnedAssertion(final AssertJOperation operation, final Object result) {
        if (!(result instanceof AbstractAssert)) {
            return;
        }

        final AbstractAssert<?, ?> returned = (AbstractAssert<?, ?>) result;
        chains.put(returned, operation.getChain());
        if (operation.isNavigation()) {
            operation.setReturnedSubject(renderer.renderSubject(actualOf(returned)));
        }
    }

    private void renameChainFromDescription(final AssertJOperation operation) {
        if (operation.isDescription()) {
            operation.getChain().rename(descriptionOf(operation.getChain().getAssertion()));
        }
    }

    private AssertJOperation activeOperation() {
        return operations.peek();
    }

    private boolean isNavigationResult(final AssertJOperation activeOperation) {
        return activeOperation != null && activeOperation.isNavigation();
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private boolean isInternalCallOnSameChain(final AssertJOperation activeOperation, final AssertJChain chain) {
        return activeOperation != null && activeOperation.getChain() == chain;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private void pop(final AssertJOperation operation) {
        if (operation.isNested()) {
            operation.leaveNested();
            return;
        }
        if (!operations.isEmpty() && operations.peek() == operation) {
            operations.pop();
        }
    }

    private Object actualOf(final AbstractAssert<?, ?> assertion) {
        return AllureAspectJ.withoutRecording(() -> {
            try {
                return assertion.actual();
            } catch (RuntimeException e) {
                return null;
            }
        });
    }

    private Optional<String> descriptionOf(final AbstractAssert<?, ?> assertion) {
        return AllureAspectJ.withoutRecording(() -> {
            try {
                return Optional.ofNullable(assertion.descriptionText())
                        .map(String::trim)
                        .filter(value -> !value.isEmpty());
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        });
    }
}
