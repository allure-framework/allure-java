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
package io.qameta.allure.testng;

import io.qameta.allure.AllureId;
import io.qameta.allure.testfilter.FileTestPlanSupplier;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanUnknown;
import io.qameta.allure.testfilter.TestPlanV1_0;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.internal.ConstructorOrMethod;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Filters TestNG methods using an Allure test plan.
 *
 * <p>TestNG calls this interceptor before execution. It keeps only methods selected by the configured Allure test plan so local and CI runs execute the requested subset.</p>
 */
public class AllureTestNgTestFilter implements IMethodInterceptor {

    private final TestPlan testPlan;

    /**
     * Creates an Allure test ng test filter with default configuration.
     */
    public AllureTestNgTestFilter() {
        this.testPlan = new FileTestPlanSupplier().supply().orElse(new TestPlanUnknown());
    }

    /**
     * Creates an Allure test ng test filter with the supplied values.
     *
     * @param testPlan the Allure test plan to evaluate against
     */
    public AllureTestNgTestFilter(final TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IMethodInstance> intercept(final List<IMethodInstance> methods,
                                           final ITestContext context) {
        return methods.stream()
                .filter(this::isSelected)
                .sorted(
                        Comparator.comparing(
                                IMethodInstance::getMethod,
                                Comparator.nullsFirst(
                                        Comparator.comparingInt(
                                                ITestNGMethod::getPriority
                                        )
                                )
                        )
                )
                .collect(Collectors.toList());
    }

    /**
     * Returns whether selected.
     *
     * @param instance the TestNG method instance to evaluate
     * @return true when selected; false otherwise
     */
    public boolean isSelected(final IMethodInstance instance) {
        return isSelected(instance.getMethod());
    }

    /**
     * Returns whether selected.
     *
     * @param method the framework or Java method to inspect
     * @return true when selected; false otherwise
     */
    public boolean isSelected(final ITestNGMethod method) {
        if (testPlan instanceof TestPlanV1_0) {
            return isSelected(method, (TestPlanV1_0) testPlan);
        } else {
            return true;
        }
    }

    /**
     * Returns whether selected.
     *
     * @param testNGMethod the TestNG method to evaluate
     * @param testPlan the Allure test plan to evaluate against
     * @return true when selected; false otherwise
     */
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
        return String.format(
                "%s.%s",
                method.getDeclaringClass().getCanonicalName(),
                method.getName()
        );
    }
}
