/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.junit4;

import io.qameta.allure.model.Label;
import io.qameta.allure.testfilter.FileTestPlanSupplier;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanV1_0;
import io.qameta.allure.util.AnnotationUtils;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.Objects;
import java.util.Optional;

import static io.qameta.allure.util.ResultsUtils.ALLURE_ID_LABEL_NAME;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureJunit4Filter extends Filter {

    private final TestPlan testPlan;

    public AllureJunit4Filter() {
        this(new FileTestPlanSupplier().supply().orElse(null));
    }

    public AllureJunit4Filter(final TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    @Override
    public boolean shouldRun(final Description description) {
        if (Objects.isNull(testPlan)) {
            return true;
        }

        if (description.isSuite()) {
            return true;
        }

        final String fullName = AllureJunit4Utils.getFullName(description);
        final String allureId = findAllureId(description).orElse(null);

        return isIncluded(testPlan, allureId, fullName);
    }

    @Override
    public String describe() {
        return "allure testplan filter";
    }

    private boolean isIncluded(final TestPlan testPlan,
                               final String allureId,
                               final String fullName) {
        if (testPlan instanceof TestPlanV1_0) {
            final TestPlanV1_0 tp = (TestPlanV1_0) testPlan;
            return Objects.isNull(tp.getTests()) || tp.getTests()
                    .stream()
                    .filter(Objects::nonNull)
                    .anyMatch(tc -> match(tc, allureId, fullName));
        }
        return true;
    }

    @SuppressWarnings("BooleanExpressionComplexity")
    private boolean match(final TestPlanV1_0.TestCase tc,
                          final String allureId,
                          final String fullName) {
        return Objects.nonNull(tc.getId()) && tc.getId().equals(allureId)
               || Objects.nonNull(tc.getSelector()) && tc.getSelector().equals(fullName);
    }

    private static Optional<String> findAllureId(final Description description) {
        return AnnotationUtils.getLabels(description.getAnnotations())
                .stream()
                .filter(l -> ALLURE_ID_LABEL_NAME.equals(l.getName()))
                .map(Label::getValue)
                .findAny();
    }
}
