/*
 *  Copyright 2020 Qameta Software OÜ
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

import io.qameta.allure.AllureId;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanV1_0;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * @author eroshenkoam (Artem Eroshenko).
 */
public class AllureFilter extends Filter {

    private final TestPlan testPlan;

    public AllureFilter(final TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    @Override
    public String describe() {
        if (testPlan instanceof TestPlanV1_0) {
            return "include ids: " + ((TestPlanV1_0) testPlan).getTests().stream()
                    .map(TestPlanV1_0.TestCase::getId).collect(Collectors.joining(" "));
        }
        return "Unknown test plan version";
    }

    @Override
    public boolean shouldRun(final Description description) {
        if (Objects.isNull(description.getMethodName())) {
            return Objects.isNull(description.getAnnotation(Ignore.class));
        }
        if (testPlan instanceof TestPlanV1_0) {
            final String selector = getSelector(description.getClassName(), description.getMethodName());

            final String allureId = Optional
                    .ofNullable(description.getAnnotation(AllureId.class))
                    .map(AllureId::value).orElse(null);

            return ((TestPlanV1_0) testPlan).isSelected(allureId, selector);
        } else {
            return true;
        }
    }

    private String getSelector(final String className, final String methodName) {
        return String.format("%s.%s",
                className,
                methodName);
    }
}
