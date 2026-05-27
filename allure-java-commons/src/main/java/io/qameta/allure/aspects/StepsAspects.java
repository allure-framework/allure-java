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
package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Step;
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

import java.util.List;
import java.util.UUID;

import static io.qameta.allure.util.AspectUtils.getName;
import static io.qameta.allure.util.AspectUtils.getParameters;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * AspectJ advice that turns {@link io.qameta.allure.Step} methods into Allure steps.
 *
 * <p>The aspect is woven around annotated methods, starts a step before invocation, updates its name and parameters, and records the final status after the method returns or throws.</p>
 */
@Aspect
public class StepsAspects {

    private static final InheritableThreadLocal<AllureLifecycle> LIFECYCLE = new InheritableThreadLocal<AllureLifecycle>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    /**
     * Configures the step annotation.
     */
    @Pointcut("@annotation(io.qameta.allure.Step)")
    public void withStepAnnotation() {
        //pointcut body, should be empty
    }

    /**
     * Handles the any method callback.
     */
    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    /**
     * Handles the step start callback.
     *
     * @param joinPoint the join point
     */
    @Before("anyMethod() && withStepAnnotation()")
    public void stepStart(final JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Step step = methodSignature.getMethod().getAnnotation(Step.class);

        final String uuid = UUID.randomUUID().toString();
        final String name = getName(step.value(), joinPoint);
        final List<Parameter> parameters = getParameters(methodSignature, joinPoint.getArgs());

        final StepResult result = new StepResult()
                .setName(name)
                .setParameters(parameters);

        getLifecycle().startStep(uuid, result);
    }

    @AfterThrowing(
            pointcut = "anyMethod() && withStepAnnotation()",
            throwing = "e"
    )

    /**
     * Handles the step failed callback.
     *
     * @param e the e
     */
    public void stepFailed(final Throwable e) {
        getLifecycle().updateStep(
                s -> s
                        .setStatus(getStatus(e).orElse(Status.BROKEN))
                        .setStatusDetails(getStatusDetails(e).orElse(null))
        );
        getLifecycle().stopStep();
    }

    /**
     * Handles the step stop callback.
     */
    @AfterReturning(pointcut = "anyMethod() && withStepAnnotation()")
    public void stepStop() {
        getLifecycle().updateStep(s -> s.setStatus(Status.PASSED));
        getLifecycle().stopStep();
    }

    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set.
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        LIFECYCLE.set(allure);
    }

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public static AllureLifecycle getLifecycle() {
        return LIFECYCLE.get();
    }
}
