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
package io.qameta.allure.test;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author charlie (Dmitry Baev).
 */
public final class AllurePredicates {

    private AllurePredicates() {
        throw new IllegalStateException("Do not instance");
    }

    public static Predicate<TestResult> hasStatus(final Status status) {
        return testResult -> status.equals(testResult.getStatus());
    }

    public static Predicate<TestResult> hasLabel(final String name, final String value) {
        final Predicate<Label> labelPredicate = label -> Objects.equals(label.getName(), name)
                && Objects.equals(label.getValue(), value);

        return testResult -> testResult.getLabels().stream().anyMatch(labelPredicate);
    }
}
