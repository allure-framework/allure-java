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

import io.qameta.allure.listener.FixtureLifecycleListener;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.TestResult;

/**
 * Clears per-thread AssertJ recorder state after Allure has finished owning the current result.
 *
 * <p>{@link AllureAspectJ} keeps an {@link AssertJRecorder} in a {@link ThreadLocal} so assertion objects can
 * be matched by identity across later fluent calls. Test engines commonly reuse worker threads, so that
 * thread-local map would otherwise keep old assertion objects, rendered steps, and operation stack state after
 * the test or fixture result has already been written. The retained {@code StepResult}s are already attached to
 * the Allure model by reference, so removing the recorder here does not remove any reported steps; it only
 * releases per-thread bookkeeping before the next test or fixture starts on the same thread.</p>
 */
public class AssertJLifecycleListener implements TestLifecycleListener, FixtureLifecycleListener {

    @Override
    public void afterTestWrite(final TestResult result) {
        AllureAspectJ.clearContext();
    }

    @Override
    public void afterFixtureStop(final FixtureResult result) {
        AllureAspectJ.clearContext();
    }
}
