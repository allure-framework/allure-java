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
package io.qameta.allure.hamcrest;

import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.test.RunUtils.runTests;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hamcrest asserts are enrichment-only: with no Allure executable running (for example when the reporter is
 * disabled), the aspect must skip silently — no step results, no warnings, and the assert itself still executes.
 */
@IsolatedLifecycle
class AllureHamcrestNoContextTest {

    @Test
    void shouldSkipSilentlyWithoutTestContext() {
        final AllureResults results = runTests(lifecycle -> MatcherAssert.assertThat("the assert still runs", Matchers.notNullValue()));

        assertThat(results.getTestResults()).isEmpty();
        assertThat(results.getTestResultContainers()).isEmpty();
    }
}
