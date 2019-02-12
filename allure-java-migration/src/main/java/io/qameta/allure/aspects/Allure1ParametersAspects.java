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
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.FieldSignature;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Objects;

import static io.qameta.allure.util.ResultsUtils.createParameter;

/**
 * Aspects for Allure1 Parameters.
 */
@SuppressWarnings("all")
@Aspect
public class Allure1ParametersAspects {

    private static AllureLifecycle lifecycle;

    @Pointcut("@annotation(ru.yandex.qatools.allure.annotations.Parameter)")
    public void withParameterAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("set(* *)")
    public void setValueToAnyField() {
        //pointcut body, should be empty
    }

    @After("setValueToAnyField() && withParameterAnnotation()")
    public void parameterValueChanged(JoinPoint joinPoint) {
        try {
            FieldSignature fieldSignature = (FieldSignature) joinPoint.getSignature();
            Parameter parameter = fieldSignature.getField().getAnnotation(Parameter.class);
            String name = parameter.value().isEmpty() ? fieldSignature.getName() : parameter.value();
            String value = Objects.toString(joinPoint.getArgs()[0]);

            getLifecycle().updateTestCase(testResult ->
                    testResult.getParameters().add(createParameter(name, value))
            );
        } catch (Exception ignored) {
        }
    }

    /**
     * For tests only.
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        lifecycle = allure;
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }
}
