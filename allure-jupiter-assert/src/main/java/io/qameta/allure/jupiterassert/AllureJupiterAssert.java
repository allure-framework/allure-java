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
package io.qameta.allure.jupiterassert;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ObjectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.getStatus;

/**
 * Integrates JUnit Jupiter assertion with Allure reporting.
 *
 * <p>Register this type through the standard JUnit Jupiter assertion extension, listener, interceptor, or plugin mechanism so framework execution events are written to Allure results. Use explicit dependencies when embedding the integration in tests or custom runtimes.</p>
 */
@SuppressWarnings("all")
@Aspect
public class AllureJupiterAssert {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureJupiterAssert.class);
    private StepResult stepResult;

    /**
     * Handles the any assert callback.
     */
    @Pointcut("call(void org.junit.jupiter.api.Assertions.*(..)) || throwable()")
    public void anyAssert() {

    }

    /**
     * Handles the throwable callback.
     */
    @Pointcut("call(Throwable org.junit.jupiter.api.Assertions.*(..))")
    public void throwable() {

    }

    /**
     * Handles the step start callback.
     *
     * @param joinPoint the join point
     */
    @Before("anyAssert()")
    public void stepStart(final JoinPoint joinPoint) {
        // enrichment-only integration: silently skip when no executable is running,
        // so a disabled Allure reporter produces no warnings and no wasted work
        if (getLifecycle().getCurrentExecutableKey().isEmpty()) {
            return;
        }
        if (joinPoint.getArgs().length > 1) {
            final String assertName = joinPoint.getSignature().getName();
            final String name;
            if (joinPoint.getSignature().getName().equalsIgnoreCase("assertAll")) {
                name = String.format("assert All in " + " '%s'", joinPoint.getArgs()[0].toString());
            } else {
                final String actual = joinPoint.getArgs().length > 0
                        ? ObjectUtils.toString(joinPoint.getArgs()[1])
                        : "<?>";
                final String expected = joinPoint.getArgs().length > 0
                        ? ObjectUtils.toString(joinPoint.getArgs()[0])
                        : "<?>";

                final List<String> assertArray = Arrays.asList(assertName.split("(?=[A-Z])"));
                if (assertArray.size() >= 3) {
                    name = String.format(assertArray.get(0) + " " + assertArray.get(1) + " '%s'", expected)
                            + " " + String.format(
                                    assertArray.stream()
                                            .skip(2)
                                            .collect(Collectors.joining(" ")) + " '%s'",
                                    actual
                            );
                } else {
                    name = String.format(assertArray.get(0) + " '%s'", expected)
                            + " " + String.format(assertArray.get(1) + " '%s'", actual);
                }
            }
            final StepResult result = new StepResult()
                    .setName(name)
                    .setStatus(Status.PASSED);
            getLifecycle().startStep(result);
        } else if (joinPoint.getArgs().length > 0) {
            final String actual = joinPoint.getArgs().length > 0
                    ? ObjectUtils.toString(joinPoint.getArgs()[0])
                    : "<?>";
            final String assertName = joinPoint.getSignature().getName();
            final String name = String.format(assertName + " '%s'", actual);

            final StepResult result = new StepResult()
                    .setName(name)
                    .setStatus(Status.PASSED);
            getLifecycle().startStep(result);
        }
    }

    @AfterThrowing(
            pointcut = "anyAssert()",
            throwing = "e"
    )

    /**
     * Handles the step failed callback.
     *
     * @param e the e
     */
    public void stepFailed(final Throwable e) {
        if (getLifecycle().getCurrentExecutableKey().isEmpty()) {
            return;
        }
        getLifecycle().updateStep(
                s -> s
                        .setStatus(getStatus(e).orElse(Status.BROKEN))
        );
        getLifecycle().stopStep();
    }

    /**
     * Handles the step stop callback.
     */
    @AfterReturning(pointcut = "anyAssert()")
    public void stepStop() {
        if (getLifecycle().getCurrentExecutableKey().isEmpty()) {
            return;
        }
        getLifecycle().updateStep(s -> s.setStatus(Status.PASSED));
        getLifecycle().stopStep();
    }

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public static AllureLifecycle getLifecycle() {
        return Allure.getLifecycle();
    }
}
