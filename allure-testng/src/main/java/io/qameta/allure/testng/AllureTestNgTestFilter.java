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
package io.qameta.allure.testng;

import io.qameta.allure.AllureId;
import io.qameta.allure.testfilter.FileTestPlanSupplier;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanUnknown;
import io.qameta.allure.testfilter.TestPlanV1_0;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.internal.ConstructorOrMethod;

public class AllureTestNgTestFilter implements IMethodInterceptor {

    private final TestPlan testPlan;

    public AllureTestNgTestFilter() {
        this.testPlan = new FileTestPlanSupplier().supply().orElse(new TestPlanUnknown());
    }

    public AllureTestNgTestFilter(final TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    @Override
    public List<IMethodInstance> intercept(final List<IMethodInstance> methods, final ITestContext context) {
        if (testPlan instanceof TestPlanV1_0) {
            return methods.stream()
                    .filter(instance -> isSelected(instance.getMethod()))
                    .collect(Collectors.toList());
        } else {
            return methods;
        }
    }

    public boolean isSelected(final ITestNGMethod instance) {
        if (testPlan instanceof TestPlanV1_0) {
            return isSelected(instance, (TestPlanV1_0) testPlan);
        } else {
            return true;
        }
    }

    public boolean isSelected(final ITestNGMethod testNGMethod, final TestPlanV1_0 testPlan) {
        final Optional<Method> method = Optional.ofNullable(testNGMethod)
                .map(ITestNGMethod::getConstructorOrMethod)
                .map(ConstructorOrMethod::getMethod);

        if (method.isPresent()) {
            final String selector = getSelector(method.get());
            final String allureId = method
                    .filter(m -> m.isAnnotationPresent(AllureId.class))
                    .map(m -> m.getAnnotation(AllureId.class))
                    .map(AllureId::value).orElse(null);
            return testPlan.isSelected(allureId, selector);
        }
        return false;
    }

    private String getSelector(final Method method) {
        return String.format("%s.%s",
                method.getDeclaringClass().getCanonicalName(),
                method.getName());
    }
}
