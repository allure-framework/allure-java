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
package io.qameta.allure.playwright;

import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;

/**
 * Allure lifecycle listener that attaches Playwright diagnostics for any supported test framework.
 */
public class AllurePlaywrightLifecycle implements TestLifecycleListener {

    @Override
    public void afterTestStart(final TestResult result) {
        AllurePlaywright.beforeTest();
    }

    @Override
    public void beforeTestStop(final TestResult result) {
        if (isFailed(result)) {
            AllurePlaywright.afterTestFailure();
        }
        AllurePlaywright.afterTest();
    }

    private static boolean isFailed(final TestResult result) {
        return result != null && (Status.FAILED == result.getStatus() || Status.BROKEN == result.getStatus());
    }
}
