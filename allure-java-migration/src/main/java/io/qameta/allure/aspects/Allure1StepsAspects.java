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
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.qameta.allure.aspects.Allure1Utils.getName;
import static io.qameta.allure.aspects.Allure1Utils.getTitle;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * Aspects (AspectJ) for handling {@link Step}.
 */
@Aspect
@SuppressWarnings("unused")
public class Allure1StepsAspects {

    private static AllureLifecycle lifecycle;

    @Pointcut("@annotation(ru.yandex.qatools.allure.annotations.Step)")
    public void withStepAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    @Before("anyMethod() && withStepAnnotation()")
    public void stepStart(final JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final String uuid = UUID.randomUUID().toString();
        final StepResult result = new StepResult()
                .setName(createTitle(joinPoint))
                .setParameters(getParameters(methodSignature, joinPoint.getArgs()));

        getLifecycle().startStep(uuid, result);
    }

    @AfterThrowing(pointcut = "anyMethod() && withStepAnnotation()", throwing = "e")
    public void stepFailed(final JoinPoint joinPoint, final Throwable e) {
        getLifecycle().updateStep(result -> result
                .setStatus(getStatus(e).orElse(Status.BROKEN))
                .setStatusDetails(getStatusDetails(e).orElse(null)));
        getLifecycle().stopStep();
    }

    @AfterReturning(pointcut = "anyMethod() && withStepAnnotation()", returning = "result")
    public void stepStop(final JoinPoint joinPoint, final Object result) {
        getLifecycle().updateStep(step -> step.setStatus(Status.PASSED));
        getLifecycle().stopStep();
    }

    public String createTitle(final JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Step step = methodSignature.getMethod().getAnnotation(Step.class);
        return step.value().isEmpty()
                ? getName(methodSignature.getName(), joinPoint.getArgs())
                : getTitle(step.value(), methodSignature.getName(), joinPoint.getThis(), joinPoint.getArgs());
    }

    private static List<Parameter> getParameters(final MethodSignature signature, final Object... args) {
        return IntStream.range(0, args.length).mapToObj(index -> {
            final String name = signature.getParameterNames()[index];
            final String value = Objects.toString(args[index]);
            return new Parameter().setName(name).setValue(value);
        }).collect(Collectors.toList());
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
