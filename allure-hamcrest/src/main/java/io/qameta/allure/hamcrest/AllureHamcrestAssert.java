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
package io.qameta.allure.hamcrest;

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
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import static io.qameta.allure.util.ResultsUtils.getStatus;

/**
 * <p>
 * Aspect "interceptor" for automatic logging to the Allure report.
 * </p>
 * <p>
 * This aspect should work for all asserts that are in the Hamcrest library, since they all go through a single method
 * to start the comparison.
 * </p>
 * <p>
 * In addition to the standard comparisons that are already in the Hamcrest library, this aspect should work correctly
 * with custom matchers if developers correctly implemented the describeMismatch and / or describeMismatchSafely
 * methods in the TypeSafeMatcher class.
 * </p>
 *
 * @see org.hamcrest.TypeSafeMatcher
 */
@Aspect
@SuppressWarnings("all")
public class AllureHamcrestAssert {

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public static AllureLifecycle getLifecycle() {
        return Allure.getLifecycle();
    }

    /**
     * Handles the init assert that callback.
     */
    @Pointcut("execution(void org.hamcrest.MatcherAssert.**(..))")
    public void initAssertThat() {
    }

    /**
     * <p>
     * assertThat(String comment, Object actual, Matcher expected) - one and only one central entry point for all
     * asserts. Based on this rule, you only need to log a method with three arguments.
     * </p>
     * <p>
     * Even if there is no comment as the first argument, an empty string will be passed as the first argument.
     * </p>
     * <p>
     * For example assertThat(value123.get(), is(equalTo("value123"))) will be proxied to the metod
     * assertThat("", value123.get(), is(equalTo("value123")))
     * </p>
     *
     * @param joinPoint - entry point with args and method name
     */
    @Before("initAssertThat()")
    public void catchAndStartStep(final JoinPoint joinPoint) {
        // enrichment-only integration: silently skip when no executable is running,
        // so a disabled Allure reporter produces no warnings and no wasted work
        if (getLifecycle().getCurrentExecutableKey().isEmpty()) {
            return;
        }
        if (joinPoint.getArgs().length == 3) {
            final String reason = (String) joinPoint.getArgs()[0];
            final String actual = ObjectUtils.toString(joinPoint.getArgs()[1]);
            final StringDescription description = new StringDescription();
            final String expecting = description.appendText("assert \"")
                    .appendText(actual)
                    .appendText("\" ")
                    .appendDescriptionOf((Matcher) joinPoint.getArgs()[2])
                    .toString();

            getLifecycle().startStep(
                    new StepResult()
                            .setName(reason.isEmpty() ? expecting : expecting + " | " + reason)
                            .setDescription("Hamcrest assert")
                            .setStatus(Status.PASSED)
            );
        }
    }

    @AfterThrowing(
            pointcut = "initAssertThat()",
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
        getLifecycle().updateStep(s -> s.setStatus(getStatus(e).orElse(Status.BROKEN)));
        getLifecycle().stopStep();
    }

    /**
     * Handles the step stop callback.
     */
    @AfterReturning(pointcut = "initAssertThat()")
    public void stepStop() {
        if (getLifecycle().getCurrentExecutableKey().isEmpty()) {
            return;
        }
        getLifecycle().updateStep(s -> s.setStatus(Status.PASSED));
        getLifecycle().stopStep();
    }

}
