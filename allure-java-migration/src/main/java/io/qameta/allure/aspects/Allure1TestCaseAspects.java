/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;

/**
 * Allure junit aspects.
 */
@Aspect
public class Allure1TestCaseAspects {

    private static AllureLifecycle lifecycle;

    @Before("execution(@org.junit.Test * *.*(..))")
    public void junitTestStart(final JoinPoint joinPoint) {
        updateTestCase(joinPoint);
    }

    @Before("execution(@org.testng.annotations.Test * *.*(..))")
    public void testNgTestStart(final JoinPoint joinPoint) {
        updateTestCase(joinPoint);
    }

    private void updateTestCase(final JoinPoint joinPoint) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Object[] args = joinPoint.getArgs();
        final Allure1Annotations annotations = new Allure1Annotations(signature, args);
        getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
            getLifecycle().updateTestCase(uuid, annotations::updateTitle);
            getLifecycle().updateTestCase(uuid, annotations::updateDescription);
            getLifecycle().updateTestCase(uuid, annotations::updateParameters);
            getLifecycle().updateTestCase(uuid, annotations::updateLabels);
            getLifecycle().updateTestCase(uuid, annotations::updateLinks);
        });
    }

    /**
     * For tests only.
     */
    public static void setLifecycle(final AllureLifecycle lifecycle) {
        Allure1TestCaseAspects.lifecycle = lifecycle;
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }

}
