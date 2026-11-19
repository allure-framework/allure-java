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
package io.qameta.allure.cucumber7jvm;

import io.qameta.allure.testfilter.FileTestPlanSupplier;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanV1_0;

import java.net.URI;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test-plan muting decisions using the scenario's reporting identity.
 */
final class AllureTestPlan {

    private static final Pattern ALLURE_ID = Pattern.compile("^@allure\\.id[:=](.+)$");

    private final TestPlan plan = new FileTestPlanSupplier().supply().orElse(null);

    boolean hasExcludedTests() {
        return plan instanceof TestPlanV1_0
                && ((TestPlanV1_0) plan).getExcludedTests() != null
                && !((TestPlanV1_0) plan).getExcludedTests().isEmpty();
    }

    boolean isMuted(final Collection<String> tags, final URI uri, final int line) {
        if (!(plan instanceof TestPlanV1_0)) {
            return false;
        }
        final TestPlanV1_0 knownPlan = (TestPlanV1_0) plan;
        final String selector = ScenarioUtils.scenarioUri(uri) + ":" + line;
        return knownPlan.isMuted(null, selector) || tags.stream()
                .map(ALLURE_ID::matcher)
                .filter(Matcher::matches)
                .anyMatch(matcher -> knownPlan.isMuted(matcher.group(1), selector));
    }
}
